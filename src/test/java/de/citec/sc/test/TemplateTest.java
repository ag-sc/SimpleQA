/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.test;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.scoring.SimpleQuestionsObjectiveFunction;
import de.citec.sc.semantics.QueryConstructor;
import de.citec.sc.template.LexicalFeatureTemplate;
import de.citec.sc.template.StateFactorScope;
import de.citec.sc.utils.ProjectConfiguration;
import de.citec.sc.variable.State;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 *
 * @author sherzod
 */
public class TemplateTest {
    @Test
    public void test() {
        String[] args = new String[8];
        args[0] = "-trainDataset";//query dataset
        args[1] = "simpleQuestionsSubset";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSubset
        args[2] = "-testDataset";  //test dataset
        args[3] = "simpleQuestionsSubset";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSubset
        args[4] = "-language";//manual lexicon
        args[5] = "EN";//EN
        args[6] = "-process";//manual lexicon
        args[7] = "train";//train, test

        ProjectConfiguration.loadConfigurations(args);

        Map<Integer, String> dudesMap = new HashMap<>();
        dudesMap.put(1, "Property");
        dudesMap.put(2, "Individual");
        dudesMap.put(3, "What");

        QueryConstructor.initialize(dudesMap);

        Set<AnnotatedDocument> documents = CorpusLoader.load(ProjectConfiguration.getTrainingDatasetName(), ProjectConfiguration.getLanguage(), true);

        System.out.println("==========================================================");

        for (AnnotatedDocument doc : documents) {

            System.out.println(doc + "\n\n");
            State state1 = new State(doc);

            state1.addURIVariable(5, 1, "http://dbpedia.org/ontology/stateOfOrigin");
            state1.addURIVariable(1, 3, "WHO_URI");
            state1.addURIVariable(3, 2, "http://dbpedia.org/resource/Ellen_Swallow_Richards");

            state1.addSlotVariable(3, 5, 2);
            state1.addSlotVariable(1, 5, 1);

            String query1 = QueryConstructor.getSPARQLQuery(state1);

            double score1 = SimpleQuestionsObjectiveFunction.computeValue(query1, doc.getDataPoint().getQuery());
            state1.setObjectiveScore(score1);
            System.out.println("===========================================================================\n" + state1.toString() + "\n\nQuery:" + query1 + "\n\n");
            
            
            LexicalFeatureTemplate template = new LexicalFeatureTemplate();
            List<StateFactorScope<State>> features = template.generateFactorScopes(state1);
            
            System.out.println(features);
        }
    }
}
