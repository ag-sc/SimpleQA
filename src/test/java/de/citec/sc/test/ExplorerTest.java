/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.test;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.sampling.BaselineExplorer;
import de.citec.sc.sampling.StateInitializer;
import de.citec.sc.scoring.SimpleQuestionsObjectiveFunction;
import de.citec.sc.semantics.QueryConstructor;
import de.citec.sc.utils.ProjectConfiguration;
import de.citec.sc.variable.State;
import de.citec.sc.variable.URIVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 *
 * @author sherzod
 */
public class ExplorerTest {

    @Test
    public void test() {
        String[] args = new String[12];
        args[0] = "-trainDataset";//query dataset
        args[1] = "simpleQuestionsSmall";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSmall
        args[2] = "-testDataset";  //test dataset
        args[3] = "simpleQuestionsSmall";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSmall
        args[4] = "-language";//manual lexicon
        args[5] = "EN";//EN
        args[6] = "-process";//manual lexicon
        args[7] = "train";//train, test
        args[8] = "-minObjectiveScore";//minimum objective score to select k states
        args[9] = "0.7";// 0.5, 0.8, 1.0
        args[10] = "-stackSize";//minimum objective score to select k states
        args[11] = "100";//train, test

        ProjectConfiguration.loadConfigurations(args);

        Map<Integer, String> dudesMap = new HashMap<>();
        dudesMap.put(1, "Property");
        dudesMap.put(2, "Individual");

        BaselineExplorer explorer = new BaselineExplorer(dudesMap);
        QueryConstructor.initialize(dudesMap);

        Set<AnnotatedDocument> documents = CorpusLoader.load(ProjectConfiguration.getTrainingDatasetName(), ProjectConfiguration.getLanguage(), false);

        System.out.println("==========================================================");

        int counter = 0;
        int correctStates = 0;
        for (AnnotatedDocument doc : documents) {
            counter++;

            StateInitializer initializer = new StateInitializer();

            State state = initializer.getInitialState(doc);

            List<State> states = explorer.getNextStates(state);

            boolean hasCorrectState = false;
            for (State s1 : states) {

                String subject = "";
                String predicate = "";

                for (Integer tokenID : s1.getUriVariables().keySet()) {
                    URIVariable var = s1.getUriVariables().get(tokenID);

                    if (var.getUri().startsWith("http://m.")) {
                        subject = var.getUri();
                    } else {
                        predicate = var.getUri();
                    }
                }

                String constructedQuery = "SELECT ?x WHERE {<" + subject + "> <" + predicate + "> ?x .}";

                double score = SimpleQuestionsObjectiveFunction.computeValue(constructedQuery, doc.getDataPoint().getQuery());
                s1.setObjectiveScore(score);
                if (score == 1.0) {
                    hasCorrectState = true;
                    break;
//                    System.out.println("===========================================================================\n" + s1.toString() + "\n\nQuery:" + query + "\n\n");
                }
            }

            if (hasCorrectState) {
                correctStates++;
            }

            System.out.println(counter + "  States: " + states.size());
        }

        System.out.println("Recall: " + (correctStates / (double) counter));

    }
}
