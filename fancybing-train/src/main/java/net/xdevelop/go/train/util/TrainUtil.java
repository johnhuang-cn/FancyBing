package net.xdevelop.go.train.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderMultiDataSetIterator;
import org.deeplearning4j.datasets.iterator.AsyncMultiDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xdevelop.go.train.FeatureRecordReader;

public class TrainUtil {
	private static final Logger log = LoggerFactory.getLogger(TrainUtil.class);
	
    public static String saveModel(String name, Model model, int index, int accuracy) throws Exception {
    	System.err.println("Saving model, don't shutdown...");
        try {
        	String fn = name + "_idx_" + index + "_" + accuracy + ".zip";
			File locationToSave = new File(System.getProperty("user.dir") + "/model/" + fn);
			boolean saveUpdater = true;                                             //Updater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this if you want to train your network more in the future
			ModelSerializer.writeModel(model, locationToSave, saveUpdater);
			System.err.println("Model saved");
			return fn;
		} catch (IOException e) {
			System.err.println("Save model failed");
			e.printStackTrace();
			throw e;
		}
    }
    
    public static MultiLayerNetwork loadNetwork(String fn, double learningRate) throws Exception {
    	System.err.println("Loading model...");
    	File locationToSave = new File(System.getProperty("user.dir") + "/model/" + fn);
		MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(locationToSave);
		
		int numLayers = model.getnLayers();
		for (int i = 0; i < numLayers; i++) {
			model.getLayer(i).conf().setLearningRateByParam("W", learningRate);
			model.getLayer(i).conf().setLearningRateByParam("b", learningRate);
		}
		return model;
    }
    
    public static ComputationGraph loadComputationGraph(String fn, double learningRate) throws Exception {
    	System.err.println("Loading model...");
    	File locationToSave = new File(System.getProperty("user.dir") + "/model/" + fn);
    	ComputationGraph model = ModelSerializer.restoreComputationGraph(locationToSave);
		
		int numLayers = model.getNumLayers();
		for (int i = 0; i < numLayers; i++) {
			model.getLayer(i).conf().setLearningRateByParam("W", learningRate);
			model.getLayer(i).conf().setLearningRateByParam("b", learningRate);
		}
    	
		return model;
    }
    
    private static UIServer uiServer;
    private static StatsListener statsListener;
    public static UIServer getUIServer() {
    	if (uiServer == null) {
    		uiServer = UIServer.getInstance();
    	}
    	
    	return uiServer;
    }
    
    
    public static void attachUI(Model model) {
        if (statsListener == null) {
        	//Initialize the user interface backend
            UIServer uiServer = getUIServer();
            //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
        	StatsStorage statsStorage = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later
//        	StatsStorage statsStorage = new FileStatsStorage(new File(System.getProperty("user.dir") + "/model/stats.data"));
	        int listenerFrequency = 100;
	        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
	        uiServer.attach(statsStorage);
	        statsListener = new StatsListener(statsStorage, listenerFrequency);
        }
        
        //Then add the StatsListener to collect this information from the network, as it trains
        model.setListeners(statsListener);
    }
    
    public static void detachUI(ComputationGraph model) {
    	model.getListeners().clear();
    }
    
    public static void detachUI(MultiLayerNetwork model) {
    	model.getListeners().clear();
    }
    
    private static List<String> createLabels(int numClasses) {
        if (numClasses == 1)
            numClasses = 2; //Binary (single output variable) case...
        List<String> list = new ArrayList<>(numClasses);
        for (int i = 0; i < numClasses; i++) {
            list.add(String.valueOf(i));
        }
        return list;
    }
    
    public static MultiDataSetIterator loadDataSetIter(String dataPath, int batchSize, int labelIndex, int outputNum) throws Exception {
    	RecordReader reader = new FeatureRecordReader();
    	reader.initialize(new FileSplit(new File(dataPath)));
    	MultiDataSetIterator iterator = new RecordReaderMultiDataSetIterator.Builder(batchSize)
    	        .addReader("reader", reader)
    	        .addInput("reader", 0, labelIndex - 1) //Input: all columns from input reader
    	        .addOutputOneHot("reader", labelIndex, outputNum)   //Output 1: one-hot for classification
    	        .addOutput("reader", labelIndex + 1, labelIndex + 1) //Output 2: for value regression
//    	        .addOutput("reader", labelIndex + 2, labelIndex + 2) //Output 2: for win/lost regression
    	        .build();
    	
    	MultiDataSetIterator iter = new AsyncMultiDataSetIterator(iterator, 1);
		return iter;
    }
    
    public static double evaluate(Model model, int outputNum, MultiDataSetIterator testData, int topN, int batchSize) {
    	log.info("Evaluate model....");
        Evaluation clsEval = new Evaluation(createLabels(outputNum), topN);
        RegressionEvaluation valueRegEval1 = new RegressionEvaluation(1);
        int count = 0;
        
        long begin = 0;
        long consume = 0;
        while(testData.hasNext()){
        	MultiDataSet ds = testData.next();
        	begin = System.nanoTime();
        	INDArray[] output = ((ComputationGraph) model).output(false, ds.getFeatures());
        	consume += System.nanoTime() - begin;
            clsEval.eval(ds.getLabels(0), output[0]);
            valueRegEval1.eval(ds.getLabels(1), output[1]);
            count++;
        }
        String stats = clsEval.stats();
        int pos = stats.indexOf("===");
        stats = "\n" + stats.substring(pos);
        log.info(stats);
        log.info(valueRegEval1.stats());
        testData.reset();
        log.info("Evaluate time: " + consume + " count: " + (count * batchSize) + " average: " + ((float) consume/(count*batchSize)/1000));
    	return clsEval.accuracy();
    }  
    
//    public static double evaluateII(Model model, int outputNum, MultiDataSetIterator testData, int topN, int batchSize) {
//    	log.info("Evaluate model....");
//        Evaluation clsEval = new Evaluation(createLabels(outputNum), topN);
//        RegressionEvaluation valueRegEval1 = new RegressionEvaluation(1);
//        RegressionEvaluation valueRegEval2 = new RegressionEvaluation(1);
//        int count = 0;
//        
//        long begin = 0;
//        long consume = 0;
//        while(testData.hasNext()){
//        	MultiDataSet ds = testData.next();
//        	begin = System.nanoTime();
//        	INDArray[] output = ((ComputationGraph) model).output(false, ds.getFeatures());
//        	consume += System.nanoTime() - begin;
//            clsEval.eval(ds.getLabels(0), output[0]);
//            valueRegEval1.eval(ds.getLabels(1), output[1]);
//            valueRegEval2.eval(ds.getLabels(2), output[2]);
//            count++;
//        }
//        String stats = clsEval.stats();
//        int pos = stats.indexOf("===");
//        stats = "\n" + stats.substring(pos);
//        log.info(stats);
//        log.info(valueRegEval1.stats());
//        log.info(valueRegEval2.stats());
//        testData.reset();
//        log.info("Evaluate time: " + consume + " count: " + (count * batchSize) + " average: " + ((float) consume/(count*batchSize)/1000));
//    	return clsEval.accuracy();
//    }    
}
