/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.sampling.BaselineExplorer;
import de.citec.sc.semantics.QueryConstructor;
import de.citec.sc.utils.ProjectConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class UDParseDataset {

    public static void main(String[] args) {
        args = new String[12];
        args[0] = "-trainDataset";//query dataset
        args[1] = "simpleQuestionsTrain";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSubset
        args[2] = "-testDataset";  //test dataset
        args[3] = "simpleQuestionsTest";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSubset
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

        System.out.println("Train Dataset");
        Set<AnnotatedDocument> trainDocuments = CorpusLoader.load(ProjectConfiguration.getTrainingDatasetName(), ProjectConfiguration.getLanguage(), true);

        System.out.println("Test Dataset");
        Set<AnnotatedDocument> testDocuments = CorpusLoader.load(ProjectConfiguration.getTestDatasetName(), ProjectConfiguration.getLanguage(), true);

    }
}
