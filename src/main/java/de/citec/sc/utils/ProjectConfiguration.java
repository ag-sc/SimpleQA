/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.utils;

import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.index.Language;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author sherzod
 */
public class ProjectConfiguration {

    public static void loadConfigurations(String[] args) {

        //read parameters
        readParamsFromCommandLine(args);
    }

    private static final Map<String, String> PARAMETERS = new HashMap<>();

    private static final String PARAMETER_PREFIX = "-";
    private static final String PARAM_SETTING_TRAIN_DATASET = "-trainDataset";
    private static final String PARAM_SETTING_TEST_DATASET = "-testDataset";
    private static final String PARAM_SETTING_PROCESS = "-process";
    private static final String PARAM_SETTING_LANGUAGE = "-language";
    private static final String PARAM_SETTING_MIN_OBJECTIVE_SCORE = "-minObjectiveScore";
    private static final String PARAM_SETTING_STACK_SIZE = "-stackSize";

    public static Language.lang getLanguage() {
        return Language.lang.valueOf(PARAMETERS.get(PARAM_SETTING_LANGUAGE));
    }

    public static CorpusLoader.Dataset getTrainingDatasetName() {
        return CorpusLoader.Dataset.valueOf(PARAMETERS.get(PARAM_SETTING_TRAIN_DATASET));
    }

    public static CorpusLoader.Dataset getTestDatasetName() {
        return CorpusLoader.Dataset.valueOf(PARAMETERS.get(PARAM_SETTING_TEST_DATASET));
    }

    public static String getProcessName() {
        return PARAMETERS.get(PARAM_SETTING_PROCESS);
    }
    public static Double getMinObjectiveScore() {
        return Double.parseDouble(PARAMETERS.get(PARAM_SETTING_MIN_OBJECTIVE_SCORE));
    }
    public static Integer getStackSize() {
        return Integer.parseInt(PARAMETERS.get(PARAM_SETTING_STACK_SIZE));
    }

    private static void readParamsFromCommandLine(String[] args) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith(PARAMETER_PREFIX)) {
                    PARAMETERS.put(args[i], args[i++ + 1]); // Skip value
                }
            }
        }
    }
}
