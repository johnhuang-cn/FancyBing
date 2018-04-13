package net.xdevelop.go.train;

import java.util.Date;

import org.deeplearning4j.datasets.iterator.AsyncMultiDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.LearningRatePolicy;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xdevelop.go.train.util.TrainUtil;

public class ResNetwork {
    private static final Logger log = LoggerFactory.getLogger(ResNetwork.class);
    
    private static int channels = 10;
    
    private int height = 19;
    private int width = 19;
    private long seed = 123456;
    private int iterations = 1;
    private boolean miniBatch = true;
    private int numClasses = 361;
    
    public ResNetwork() {
    }
    
    public ComputationGraph init() {
    	ComputationGraphConfiguration.GraphBuilder graph = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .activation(Activation.LEAKYRELU)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .lrPolicyDecayRate(0.5)
                .learningRateDecayPolicy(LearningRatePolicy.Score)
                .updater(Adam.builder().build())
                .weightInit(WeightInit.XAVIER)
                .learningRate(0.02)
                .miniBatch(miniBatch)
                .convolutionMode(ConvolutionMode.Truncate)
                .trainingWorkspaceMode(WorkspaceMode.SINGLE)
                .inferenceWorkspaceMode(WorkspaceMode.SINGLE)
                .graphBuilder();
    	
    	// set input & output
		graph
			.addInputs("input").setInputTypes(InputType.convolutionalFlat(height, width, channels))
			.addLayer("policy", new OutputLayer.Builder()
	                .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
	                .activation(Activation.SOFTMAX)
	                .nOut(numClasses).build(), "embeddings_c")
			.addLayer("value1", new OutputLayer.Builder()
	                .lossFunction(LossFunctions.LossFunction.MSE)
	                .activation(Activation.TANH)
	                .nOut(1).build(), "embeddings_r1")
//	        .addLayer("value2", new OutputLayer.Builder()
//	                .lossFunction(LossFunctions.LossFunction.MSE)
//	                .activation(Activation.TANH)
//	                .nOut(1).build(), "embeddings_r2")
	        .setOutputs("policy", "value1", "value2")
			.backprop(true).pretrain(false);
		
		int kernelSize = 128;
		
		graph.addLayer("c-layer0", new ConvolutionLayer.Builder(new int[]{3,3}, new int[]{1,1}, new int[]{1,1}).activation(Activation.LEAKYRELU).nOut(kernelSize).build(), "input");
		
		int blockNum = 8;
		String prevLayer = "c-layer0";
		for (int i = 1; i <= blockNum; i++) {
			String layerName = "c-block" + i + "-";
			graph.addLayer(layerName + "1", new ConvolutionLayer.Builder(new int[]{3,3}, new int[]{1,1}, new int[]{1,1}).activation(Activation.LEAKYRELU).nOut(kernelSize).build(), prevLayer);
			graph.addLayer(layerName + "2", new ConvolutionLayer.Builder(new int[]{3,3}, new int[]{1,1}, new int[]{1,1}).activation(Activation.IDENTITY).nOut(kernelSize).build(), layerName + "1");
			graph.addVertex("shortcut" + i, new ElementWiseVertex(ElementWiseVertex.Op.Add), layerName + "2", prevLayer);
			graph.addLayer(layerName + "3", new ActivationLayer.Builder().activation(Activation.LEAKYRELU).build(), "shortcut" + i);
			prevLayer = layerName + "3";
		}
		
		// for classification
		graph.addLayer("embeddings_c", new ConvolutionLayer.Builder(new int[]{3,3}, new int[]{1,1}, new int[]{1,1}).activation(Activation.IDENTITY).nOut(2).build(), prevLayer);
		
		// for value regression
		graph.addLayer("reg-c-layer1", new ConvolutionLayer.Builder(new int[]{3,3}, new int[]{1,1}, new int[]{1,1}).activation(Activation.IDENTITY).nOut(1).build(), prevLayer);
		graph.addLayer("embeddings_r1", new DenseLayer.Builder().activation(Activation.IDENTITY).nOut(256).build(), "reg-c-layer1");
		
//		graph.addLayer("reg-c-layer2", new ConvolutionLayer.Builder(new int[]{3,3}, new int[]{1,1}, new int[]{1,1}).activation(Activation.IDENTITY).nOut(1).build(), prevLayer);
//		graph.addLayer("embeddings_r2", new DenseLayer.Builder().activation(Activation.IDENTITY).nOut(256).build(), "reg-c-layer2");

		ComputationGraphConfiguration conf = graph.build();
		ComputationGraph model = new ComputationGraph(conf);
		model.init();
		
		log.info("\nNumber of params: " + model.numParams()+"\n");
		return model;
    }
    
	public static void main(String[] args) throws Exception {
    	Nd4j.getMemoryManager().setAutoGcWindow(4000); // adjust this value if the GPU memory is lower than 8M
    	String trainPath = "f:/features/data/train/train-mid/";
		String testPath = "E:/features/data/test/kgs_test.txt"; 
        
        int labelIndex = 361 * channels;
        int batchSize = 128;
        int outputNum = 361;
        
        MultiDataSetIterator trainData = null;
        MultiDataSetIterator testData = null;

//		ComputationGraph model = new ResNetwork().init();   // uncomment this when start a new training
        ComputationGraph model = TrainUtil.loadComputationGraph("ResNetwork_idx_999_0.zip", 0.01);  // comment this line and change the learning rate if want to continue train exist model
		
		log.info("Train model...." + new Date());
//		TrainUtil.attachUI(model);  // uncomment this line if want to view the train UI, it would affect the performance                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         
	    
	    for( int i = 0; i < 1000; i++ ) {
	    	long start = System.currentTimeMillis();
	    	
	    	// load next data package
	    	String trainFile = trainPath + i + ".txt";
	    	trainData = TrainUtil.loadDataSetIter(trainFile, batchSize, labelIndex, outputNum);
	    	
	    	// train
	        model.fit(trainData);
	        log.info("train time: " + (System.currentTimeMillis() - start));
	        log.info("*** Completed index {} ***", i);
	
	        // reload model
	        ((AsyncMultiDataSetIterator) trainData).shutdown(); // performance may decreased after long training without shutdown, this bug seems fixed in DL4J 0.9.1
        	TrainUtil.saveModel("ResNetwork", model, 999, 0);

        	if (i % 10 != 0) {	
	        	continue;
	        }
	        
	        double accuracy = 0.0;
	        try {
	        	if (testData == null) {
	        		 testData = TrainUtil.loadDataSetIter(testPath, 256, labelIndex, outputNum);
	        	}
	        	accuracy = TrainUtil.evaluate(model, outputNum, testData, 10, 256);
	        	((AsyncMultiDataSetIterator) testData).shutdown(); // performance may decreased after long training without shutdown
	        	testData = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
	        finally {
	        	// save model
	        	TrainUtil.saveModel("ResNetwork", model, i, (int) (accuracy * 10000));
	        }
	    }
	    log.info("****************Finished********************");
	}
}
