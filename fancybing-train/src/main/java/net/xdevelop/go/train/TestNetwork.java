package net.xdevelop.go.train;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import net.xdevelop.go.train.util.TrainUtil;

public class TestNetwork {
	   
	public static void main(String[] args) throws Exception {
		Nd4j.getMemoryManager().setAutoGcWindow(2000);
		
		int labelIndex = 361 * 10;
        int batchSize = 64;
        int outputNum = 361;
        
		String testPath = "E:/features/data/test/kgs_test.txt";
		ComputationGraph model = TrainUtil.loadComputationGraph("ResNetwork_idx_999_0.zip", 0.0001);
        MultiDataSetIterator testData = TrainUtil.loadDataSetIter(testPath, batchSize, labelIndex, outputNum);
        TrainUtil.evaluate(model, outputNum, testData, 20, batchSize);
    }

}
