/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.d4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration.GraphBuilder;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.graph.MergeVertex;
import org.deeplearning4j.nn.conf.graph.rnn.DuplicateToTimeSeriesVertex;
import org.deeplearning4j.nn.conf.graph.rnn.LastTimeStepVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.EmbeddingLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

/**
 *
 * @author sherzod
 */
public class Test {
    
    private static final double LEARNING_RATE = 0.01;
    private static final double RMS_DECAY = 0.01;
    //back ward length
    private static final int TBPTT_SIZE = 10;
    
    //LSTM in and out layer size
    private static final int WORD_EMBEDDING_WIDTH = 300;
    private static final int LSTM_LAYER_WIDTH = 30;
    
    
    //position embedding size
    private static final int POSITION_EMBEDDING_WIDTH = 30;
    
            
    private static Map<String, String> wordDict;
    private static Map<String, String> positionDict;
            
    
    private void createComputationGraph() {
        NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder();
        builder.iterations(1).learningRate(LEARNING_RATE).rmsDecay(RMS_DECAY)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).miniBatch(true).updater(Updater.RMSPROP)
                .weightInit(WeightInit.XAVIER).gradientNormalization(GradientNormalization.RenormalizeL2PerLayer);

        GraphBuilder graphBuilder = builder.graphBuilder().pretrain(false).backprop(true).backpropType(BackpropType.Standard)
                .tBPTTBackwardLength(TBPTT_SIZE).tBPTTForwardLength(TBPTT_SIZE);
        graphBuilder.addInputs("wordIndices", "relativePositionToEntity")
                .setInputTypes(InputType.recurrent(wordDict.size()), InputType.recurrent(positionDict.size()))
                .addLayer("embeddingEncoder", new EmbeddingLayer.Builder().nIn(wordDict.size()).nOut(WORD_EMBEDDING_WIDTH).build(), "wordIndices")
                .addLayer("lstmEncoder",
                        new GravesLSTM.Builder().nIn(WORD_EMBEDDING_WIDTH).nOut(LSTM_LAYER_WIDTH).activation(Activation.TANH).build(),
                        "embeddingEncoder")
                
                .addLayer("positionEncoder", new EmbeddingLayer.Builder().nIn(positionDict.size()).nOut(POSITION_EMBEDDING_WIDTH).build(), "wordIndices")
                
                
                .addVertex("sentenceSummary", new LastTimeStepVertex("wordIndices"), "lstmEncoder")
                
                .addVertex("repeatedSentenceSummary", new DuplicateToTimeSeriesVertex("wordIndices"), "sentenceSummary")
                
                .addVertex("concatenatedEncodings", new MergeVertex(), "lstmEncoder", "positionEncoder", "repeatedSentenceSummary")
                
                .addLayer("innerAttentionEmbedding",
                        new DenseLayer.Builder().nIn(LSTM_LAYER_WIDTH + POSITION_EMBEDDING_WIDTH + LSTM_LAYER_WIDTH).nOut(INNER_ATTENTION_WIDTH).activation(Activation.TANH)
                                .build(),
                        "concatenatedEncodings")
                .addLayer("attentionScores",
                        new DenseLayer.Builder().nIn(INNER_ATTENTION_WIDTH).nOut(1)
                                .build(),
                        "innerAttentionEmbedding")
                
            
                .addLayer("output", new RnnOutputLayer.Builder().nIn(HIDDEN_LAYER_WIDTH).nOut(dict.size()).activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.MCXENT).build(), "decoder")
                .setOutputs("output");

        net = new ComputationGraph(graphBuilder.build());
        net.init();
    }
    

    public static void test1() {
        
        
        List<String> currencyHiddenLayerNames = new ArrayList<String>();
        GraphBuilder b = new NeuralNetConfiguration.Builder().seed(seed).iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).updater(Updater.ADAM)
                .graphBuilder().addInputs(getInputNames());
        
        for (InputConfig input : inputs) {
            String hiddenlayerName = "H1_" + input.name;
            b.addLayer(hiddenlayerName, new DenseLayer.Builder().nIn(input.nInputUnits).nOut(input.nHiddenUnits)
                    .dropOut(dropout).l2(l2Hidden).activation(Activation.ELU).build(),
                    input.name);
            currencyHiddenLayerNames.add(hiddenlayerName);
        }
        
        
        int jointHiddenInputSize = inputs.stream().mapToInt(c
                -> c.nHiddenUnits).sum();
        b.addLayer("H_all",
                new DenseLayer.Builder().nIn(jointHiddenInputSize).nOut(nJointHiddenUnits).dropOut(dropout).l2(l2Hidden)
                        .activation(Activation.ELU).build(),
                currencyHiddenLayerNames.toArray(new String[currencyHiddenLayerNames.size()]));
        b.addLayer(outputName, new OutputLayer.Builder(LossFunction.MSE).nIn(nJointHiddenUnits).nOut(nOutputUnits)
                .l2(l2Output).activation(Activation.TANH).build(), "H_all");
        b.setOutputs(outputName);
        b.pretrain(false);
        b.backprop(true);
        ComputationGraphConfiguration conf = b.build();

        network = new ComputationGraph(conf);
        network.init();
    }
    

    public void trainOnBatch(DataBatches data, INDArray outputData) {
        checkInputs(data);
        INDArray[] inputDataBatches = data.get(getInputNames());
        INDArray[] outputDataBatches = new INDArray[]{outputData};
        MultiDataSet trainData = new MultiDataSet(inputDataBatches,
                outputDataBatches);
        network.fit(trainData);

    }

}
