/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.baseline;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.corpus.SampledMultipleInstance;
import de.citec.sc.query.SPARQLParser;
import de.citec.sc.sampling.BaselineExplorer;
import de.citec.sc.sampling.MyBeamSearchSampler;
import de.citec.sc.sampling.SamplingStrategies;
import de.citec.sc.sampling.StateInitializer;
import de.citec.sc.scoring.FeatureMapData;
import de.citec.sc.scoring.LibSVMRegressionScorer;
import de.citec.sc.scoring.NELHybridSamplingStrategyCallback;
import de.citec.sc.scoring.NELTrainer;
import de.citec.sc.scoring.NELTrainer.InstanceCallback;
import de.citec.sc.scoring.SimpleQuestionsObjectiveFunction;
import de.citec.sc.semantics.QueryConstructor;
import de.citec.sc.template.BaselineTemplate;
import de.citec.sc.utils.ProjectConfiguration;
import de.citec.sc.variable.State;
import de.citec.sc.variable.URIVariable;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import learning.AdvancedLearner;
import learning.Learner;
import learning.Model;
import learning.ObjectiveFunction;
import learning.optimizer.SGD;
import learning.scorer.DefaultScorer;
import learning.scorer.Scorer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sampling.Explorer;
import sampling.samplingstrategies.AcceptStrategies;
import sampling.stoppingcriterion.BeamSearchStoppingCriterion;
import templates.AbstractTemplate;
import variables.AbstractState;

/**
 *
 * @author sherzod
 */
public class BaselinePipeline {

    private static final Map<Integer, String> dudesMap = new LinkedHashMap<>();
    private static final int NUMBER_OF_SAMPLING_STEPS = 1;
    private static int BEAM_SIZE_TRAINING = 10;
    private static final int NUMBER_OF_EPOCHS = 10;
    private static final FeatureMapData featureMapData = new FeatureMapData();

    private static Logger log = LogManager.getFormatterLogger();

    public static void main(String[] args) {

        System.out.println("running the pipeline");

        dudesMap.put(1, "Property");
        dudesMap.put(2, "Individual");

        args = new String[12];
        args[0] = "-trainDataset";//query dataset
        args[1] = "simpleQuestionsTrain";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSubset  simpleQuestionsSmall
        args[2] = "-testDataset";  //test dataset
        args[3] = "simpleQuestionsTest";//simpleQuestionsTrain  simpleQuestionsTest simpleQuestionsSubset  simpleQuestionsSmall
        args[4] = "-language";//manual lexicon
        args[5] = "EN";//EN
        args[6] = "-process";//manual lexicon
        args[7] = "train";//train, test
        args[8] = "-minObjectiveScore";//minimum objective score to select k states
        args[9] = "0.7";// 0.5, 0.8, 1.0
        args[10] = "-stackSize";//minimum objective score to select k states
        args[11] = "100";//train, test

        //initialize
        ProjectConfiguration.loadConfigurations(args);
        QueryConstructor.initialize(dudesMap);

        BEAM_SIZE_TRAINING = ProjectConfiguration.getStackSize();

        Set<AnnotatedDocument> documents1 = CorpusLoader.load(ProjectConfiguration.getTrainingDatasetName(), ProjectConfiguration.getLanguage(), false);
        Set<AnnotatedDocument> documents2 = CorpusLoader.load(ProjectConfiguration.getTestDatasetName(), ProjectConfiguration.getLanguage(), false);

        List<AnnotatedDocument> trainingDocs = new ArrayList<>(documents1);
        List<AnnotatedDocument> testDocs = new ArrayList<>(documents2);

        //train
        Model trainedModel = null;

        try {

            //train the model and save
            trainedModel = train(trainingDocs);
            trainedModel.saveModelToFile(new File("models"));

//            //load model
//            List<AbstractTemplate<AnnotatedDocument, State, ?>> templates = new ArrayList<>();
//            templates.add(new LexicalFeatureTemplate());
//
//            Scorer scorer = new LibSVMRegressionScorer();
//
//            trainedModel = new Model<>(scorer, templates);
//            trainedModel.setMultiThreaded(false);
//
//            QATemplateFactory templateFactory = new QATemplateFactory();
//            trainedModel.loadModelFromDir(new File("models"), templateFactory);
//            
//            System.out.println(trainedModel.toDetailedString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //test
        args[7] = "test";
        ProjectConfiguration.loadConfigurations(args);
        List<SampledMultipleInstance<AnnotatedDocument, String, State>> testResults = test(trainedModel, testDocs);

        System.out.println("\n\nEvaluating test results ... \n\n");

        double macroF1 = 0;

        for (SampledMultipleInstance<AnnotatedDocument, String, State> sampledInstance : testResults) {
            List<State> states = sampledInstance.getStates();

            for (State state : states) {
                
                String subject = "";
                String predicate = "";

                for (Integer tokenID : state.getUriVariables().keySet()) {
                    URIVariable var = state.getUriVariables().get(tokenID);

                    if (var.getUri().startsWith("http://m.")) {
                        subject = var.getUri();
                    } else {
                        predicate = var.getUri();
                    }
                }

                String constructedQuery = "SELECT ?x WHERE {<" + subject + "> <" + predicate + "> ?x .}";

                String goldQuery = sampledInstance.getGoldResult();

                //if it's valid query
                if (!constructedQuery.isEmpty() && !SPARQLParser.extractURIsFromQuery(constructedQuery).isEmpty()) {

                    double score = SimpleQuestionsObjectiveFunction.computeValue(constructedQuery, goldQuery);
                    macroF1 += score;
                    System.out.println(state);
                    System.out.println("Query: " + constructedQuery + "\n\nScore:" + score);

                    break;
                }

            }
        }

        System.out.println("\nOverall: " + (macroF1 / testResults.size()));
    }

    private static Model<AnnotatedDocument, State> train(List<AnnotatedDocument> trainingDocuments) {
        /*
         * Setup all necessary components for training and testing.
         *
         *
         * Define an objective function that guides the training procedure.
         */
        ObjectiveFunction<State, String> objective = new SimpleQuestionsObjectiveFunction();

        /*
         * Define templates that are responsible to generate factors/features to
         * score generated states.
         */
        List<AbstractTemplate<AnnotatedDocument, State, ?>> templates = new ArrayList<>();
        templates.add(new BaselineTemplate());

        /*
         * Create the scorer object that computes a score from the factors'
         * features and the templates' weight vectors.
         */
        Scorer scorer = new DefaultScorer();
//        Scorer scorer = new LibSVMRegressionScorer();


        /*
         * Define a model and provide it with the necessary templates.
         */
        Model<AnnotatedDocument, State> model = new Model<>(scorer, templates);
        model.setMultiThreaded(false);

        /*
         * Create an Initializer that is responsible for providing an initial
         * state for the sampling chain given a document.
         */
        StateInitializer initializer = new StateInitializer();

        /*
         * Define the explorers that will provide "neighboring" states given a
         * starting state. The sampler will select one of these states as a
         * successor state and, thus, perform the sampling procedure.
         */
        List<Explorer<State>> explorers = new ArrayList<>();
        explorers.add(new BaselineExplorer(dudesMap));
        /*
         * Create a sampler that generates sampling chains with which it will
         * trigger weight updates during training.
         */

 /*
         * Stopping criterion for the sampling process. If you set this value
         * too small, the sampler can not reach the optimal solution. Large
         * values, however, increase computation time.
         */
        BeamSearchStoppingCriterion<State> scoreStoppingCriterion = new BeamSearchStoppingCriterion<State>() {

            @Override
            public boolean checkCondition(List<List<State>> chain, int step) {

                List<State> lastStates = chain.get(chain.size() - 1);
                //sort by objective
                lastStates = lastStates.stream().sorted((s1, s2) -> Double.compare(s1.getObjectiveScore(), s2.getObjectiveScore())).collect(Collectors.toList());

                State s = (State) lastStates.get(lastStates.size() - 1);

                double maxScore = s.getObjectiveScore();

                if (maxScore == 1.0) {
                    return true;
                }

                int count = 0;
                final int maxCount = 4;

                for (int i = chain.size() - 1; i >= 0; i--) {
                    List<State> chainStates = chain.get(i);
                    State maxState = (State) chainStates.get(chainStates.size() - 1);

                    if (maxState.getObjectiveScore() >= maxScore) {
                        count++;
                    }
                }

                return count >= maxCount || chain.size() >= NUMBER_OF_SAMPLING_STEPS;
            }
        };

        /*
         * 
         */
        MyBeamSearchSampler<AnnotatedDocument, State, String> nelSampler = new MyBeamSearchSampler<>(model, objective, explorers,
                scoreStoppingCriterion);

        Double minObjectiveScore = 1.0;

        nelSampler.setTrainSamplingStrategy(SamplingStrategies.greedyBeamSearchSamplingStrategyByObjective(BEAM_SIZE_TRAINING, s -> s.getObjectiveScore(), minObjectiveScore));
        nelSampler.setTrainAcceptStrategy(AcceptStrategies.strictObjectiveAccept());

        nelSampler.addStepCallback(new MyBeamSearchSampler.StepCallback() {
            @Override
            public <InstanceT, StateT extends AbstractState<InstanceT>> void onEndStep(MyBeamSearchSampler<InstanceT, StateT, ?> sampler, int step, int e, int numberOfExplorers, List<StateT> initialStates, List<StateT> currentStates) {

                for (final StateT stateT : currentStates) {

                    featureMapData
                            .addFeatureDataPoint(((State) stateT).toTrainingPoint(featureMapData, true));
                }
            }

        });

        /*
         * Define a learning strategy. The learner will receive state pairs
         * which can be used to update the models parameters.
         */
        Learner<State> learner = new AdvancedLearner<>(model, new SGD());

        log.info("####################");
        log.info("Start training");

        /*
         * The trainer will loop over the data and invoke sampling and learning.
         * Additionally, it can invoke predictions on new data.
         */
        NELTrainer neltrainer = new NELTrainer();
        neltrainer.addInstanceCallback(new InstanceCallback() {

            @Override
            public <InstanceT, StateT extends AbstractState<InstanceT>> void onEndInstance(NELTrainer caller,
                    InstanceT instance, int indexOfInstance, StateT finalState, int numberOfInstances, int epoch,
                    int numberOfEpochs) {

                if (scorer instanceof LibSVMRegressionScorer) {
                    ((LibSVMRegressionScorer) scorer).svmTrain(featureMapData);
                }
            }
        });
        //hybrid training procedure, switches every epoch to another scoring method {objective or model}
        neltrainer.addEpochCallback(new NELHybridSamplingStrategyCallback(nelSampler, BEAM_SIZE_TRAINING));

        //train the model
        List<SampledMultipleInstance<AnnotatedDocument, String, State>> trainResults = neltrainer.train(nelSampler, initializer, learner, trainingDocuments, i -> i.getResult(), NUMBER_OF_EPOCHS);

        log.info("\nNEL Model :\n" + model.toDetailedString());

//        Map<Model<AnnotatedDocument, State>, List<SampledMultipleInstance<AnnotatedDocument, String, State>>> pair = new HashMap<>();
//        pair.put(model, trainResults);
        return model;
    }

    private static List<SampledMultipleInstance<AnnotatedDocument, String, State>> test(Model<AnnotatedDocument, State> model, List<AnnotatedDocument> testDocuments) {
        /*
         * Setup all necessary components for training and testing.
         */
 /*
         * Define an objective function that guides the training procedure.
         */
        ObjectiveFunction<State, String> objective = new SimpleQuestionsObjectiveFunction();
        /*
         * Create an Initializer that is responsible for providing an initial
         * state for the sampling chain given a document.
         */
        StateInitializer initializer = new StateInitializer();


        /*
         * Define the explorers that will provide "neighboring" states given a
         * starting state. The sampler will select one of these states as a
         * successor state and, thus, perform the sampling procedure.
         */
        List<Explorer<State>> explorers = new ArrayList<>();
        explorers.add(new BaselineExplorer(dudesMap));
        /*
         * Create a sampler that generates sampling chains with which it will
         * trigger weight updates during training.
         */

 /*
         * Stopping criterion for the sampling process. If you set this value
         * too small, the sampler can not reach the optimal solution. Large
         * values, however, increase computation time.
         */
        BeamSearchStoppingCriterion<State> scoreStoppingCriterion = new BeamSearchStoppingCriterion<State>() {

            @Override
            public boolean checkCondition(List<List<State>> chain, int step) {

                List<State> lastStates = chain.get(chain.size() - 1);
                State s = (State) lastStates.get(lastStates.size() - 1);

                double maxScore = s.getModelScore();

                int count = 0;
                final int maxCount = 4;

                for (int i = chain.size() - 1; i >= 0; i--) {
                    List<State> chainStates = chain.get(i);
                    State maxState = (State) chainStates.get(chainStates.size() - 1);

                    if (maxState.getModelScore() >= maxScore) {
                        count++;
                    }
                }
                return count >= maxCount || chain.size() >= NUMBER_OF_SAMPLING_STEPS;
            }
        };

        /*
         * 
         */
        MyBeamSearchSampler<AnnotatedDocument, State, String> sampler = new MyBeamSearchSampler<>(model, objective, explorers,
                scoreStoppingCriterion);
        sampler.setTestSamplingStrategy(SamplingStrategies.greedyBeamSearchSamplingStrategyByModel(BEAM_SIZE_TRAINING, s -> s.getModelScore()));
        sampler.setTestAcceptStrategy(AcceptStrategies.strictModelAccept());

        log.info("####################");
        log.info("Start testing");

        /*
         * The trainer will loop over the data and invoke sampling.
         */
        NELTrainer trainer = new NELTrainer();

        List<SampledMultipleInstance<AnnotatedDocument, String, State>> testResults = trainer.test(sampler, initializer, testDocuments, i -> i.getResult());
        /*
         * Since the test function does not compute the objective score of its
         * predictions, we do that here, manually, before we print the results.
         */
        return testResults;
    }
}
