/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.index.Language;
import de.citec.sc.sampling.BaselineExplorer;
import de.citec.sc.sampling.StateInitializer;
import de.citec.sc.scoring.SimpleQuestionsObjectiveFunction;
import de.citec.sc.semantics.QueryConstructor;
import de.citec.sc.syntax.UDPipe;
import de.citec.sc.utils.ProjectConfiguration;
import de.citec.sc.utils.TrainingLexiconUtil;
import de.citec.sc.variable.State;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class GenerateTrainingDataset {

    public static void main(String[] args) {

        args = new String[12];
        args[0] = "-trainDataset";//query dataset
        args[1] = "simpleQuestionsTrain";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSubset
        args[2] = "-testDataset";  //test dataset
        args[3] = "simpleQuestionsSubset";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSubset
        args[4] = "-language";//manual lexicon
        args[5] = "EN";//EN
        args[6] = "-process";//manual lexicon
        args[7] = "train";//train, test
        args[8] = "-minObjectiveScore";//minimum objective score to select k states
        args[9] = "0.7";// 0.5, 0.8, 1.0
        args[10] = "-stackSize";//minimum objective score to select k states
        args[11] = "100";//train, test

        Map<Integer, String> dudesMap = new HashMap<>();
        dudesMap.put(1, "Property");
        dudesMap.put(2, "Individual");
        dudesMap.put(3, "What");

        ProjectConfiguration.loadConfigurations(args);

        BaselineExplorer explorer = new BaselineExplorer(dudesMap);
        QueryConstructor.initialize(dudesMap);

        Set<AnnotatedDocument> documents = CorpusLoader.load(ProjectConfiguration.getTrainingDatasetName(), ProjectConfiguration.getLanguage(), true);

        System.out.println("Document# " + documents.size());

        System.out.println("==========================================================");

        int counter = 0;
        int correctStates = 0;

        String nullDocs = "";
        for (AnnotatedDocument doc : documents) {
            if(doc.getDependencyParse() == null){
                System.out.println(doc.getDataPoint().toString());
                counter++;
            }
        }
        
        System.out.println(counter+"/"+documents.size());
        System.exit(0);
        
        for (AnnotatedDocument doc : documents) {
            counter++;

            StateInitializer initializer = new StateInitializer();

            if (doc.getDependencyParse() == null) {
                nullDocs += doc.getDataPoint().getQuestion() + "\n";
                continue;
            }

            State state = initializer.getInitialState(doc);

            System.out.print(counter +" "+state.toString());

            try {
                List<State> states = explorer.getNextStates(state);

                boolean hasCorrectState = false;
                for (State sampledState : states) {
                    String query = QueryConstructor.getSPARQLQuery(sampledState);

                    double score = SimpleQuestionsObjectiveFunction.computeValue(query, doc.getDataPoint().getQuery());
                    sampledState.setObjectiveScore(score);
                    if (score == 1.0) {
                        
                        TrainingLexiconUtil.indexState(sampledState);
                        
                        hasCorrectState = true;
                        break;
//                    System.out.println("===========================================================================\n" + s1.toString() + "\n\nQuery:" + query + "\n\n");
                    }
                }

                if (hasCorrectState) {
                    correctStates++;
                }

                System.out.println("  States: " + states.size());
            } catch (Exception e) {
//                System.out.println("Problem with : \n"+doc.toString()+"\n");
            }
        }

        System.out.println(nullDocs);

        System.out.println("Recall: " + (correctStates / (double) counter));
    }
}
