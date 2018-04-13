package net.xdevelop.go.policynet;

import java.io.File;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import net.xdevelop.go.Config;
import net.xdevelop.go.model.Board;

public final class PolicyNetUtil {

	public static ComputationGraph loadComputationGraph(String fn) throws Exception {
    	System.err.println("Loading model...");
//    	File locationToSave = new File(System.getProperty("user.dir") + "/model/" + fn);
    	File locationToSave = new File("D:\\workspace\\fancybing-train\\model\\" + fn);
    	ComputationGraph model = ModelSerializer.restoreComputationGraph(locationToSave);
	
		return model;
    }
	
	public static MultiLayerNetwork loadNetwork(String fn) throws Exception {
    	System.err.println("Loading model...");
    	File locationToSave = new File(System.getProperty("user.dir") + "/model/" + fn);
    	MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(locationToSave);
	
		return model;
    }
	
	public static int[][] getMoves(INDArray guess, int rotationMode, boolean[] legalPoints) {
		// Limit the top moves < 10 can improve the performance, but also increase the blind point risk
		float accuProbability = Config.POLICY_NETWORK_TOP_PROBABILITY;
		int[][] moves = new int[Config.POLICY_NETWORK_MAX_TOP_N][2];
		INDArray guessIndex;
		float accumProb = 0.0f;
		double prob = 0.0f;
		int count = 0;
		int dataIndex = 0;
		for (int i = 0; i < Config.POLICY_NETWORK_MAX_TOP_N; i++) {
			guessIndex = Nd4j.argMax(guess, 1);
			dataIndex = (int) guessIndex.getFloat(0);
			prob = guess.getDouble(dataIndex);
			if (prob < 0.01 && count > 0) { // ignore the low probability move
				break;
			}
			
			moves[i][0] = Board.getOriginIndex(dataIndex, rotationMode);
			moves[i][1] = (int) (prob * 1000 + 0.5);
			count++;
			if (!legalPoints[moves[i][0]]) {
				moves[i][0] = Board.PASS;
			}
			accumProb += prob;
			if (accumProb >= accuProbability) {
				break;
			}
			guess.putScalar(dataIndex, -1.0f);
		}
		
		int[][] rs = new int[count][2];
		for (int i = 0; i < rs.length; i++) {
			rs[i][0] = moves[i][0];
			rs[i][1] = moves[i][1];
		}
		return rs;
    }
}
