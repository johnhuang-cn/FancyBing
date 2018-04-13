package net.xdevelop.go.mcts;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import net.xdevelop.go.Config;
import net.xdevelop.go.model.Node;
import net.xdevelop.go.policynet.PolicyNetUtil;
import net.xdevelop.go.policynet.RemoteEvaluator;
import net.xdevelop.go.util.NodeUtil;

/**
 * Policy network evaluate thread pool
 * 策略网络评估池
 */
public class Evaluator extends Thread {
	private static int BATCH_SIZE;
	private static AtomicInteger TOTAL = new AtomicInteger();
	public static AtomicInteger COUNT = new AtomicInteger();
	
	private static BlockingQueue<Node> queue;
	
	private RemoteEvaluator resNet;
	private ArrayList<Node> batchNodes;
	private INDArray[] arrs;
	INDArray features; 
	
	public static void init(int threads_num, int topN, int batchSize) {
		BATCH_SIZE = batchSize;
		queue = new ArrayBlockingQueue<Node>(batchSize * 8 * threads_num);
		for (int i = 0; i < threads_num; i++) {
			Evaluator t = new Evaluator();
			t.batchNodes = new ArrayList<Node>(batchSize);
			t.resNet = RemoteEvaluator.getRemotePolicyNet(i); 
			t.arrs = new INDArray[BATCH_SIZE];
			t.features = Nd4j.create(batchSize, Config.FEATURE_COLUMNS);
			for (int j = 0; j < BATCH_SIZE; j++) {
				t.arrs[j] = Nd4j.create(Config.FEATURE_COLUMNS);
			}
			t.start();
		}
	}
	
	public Evaluator() {
	}
	
	public static boolean addNode(Node node) {
			try {
				queue.put(node);
				return true;
			} catch (Exception e) {
				node.expanding = false;
				return false;
			}
	}
	
	public static float getAvgEvaluate() {
		return TOTAL.floatValue() / COUNT.get();
	}
	
	public static int size() {
		return queue != null ? queue.size() : 0;
	}
	
	public static void clear() {
		if (queue != null) {
			queue.clear();
		}
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				batchNodes.clear();
				queue.drainTo(batchNodes, BATCH_SIZE);
				
				if (batchNodes.size() == 0) continue;
				
				COUNT.incrementAndGet();
				TOTAL.addAndGet(batchNodes.size());
				
				evaluate(batchNodes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void evaluate(ArrayList<Node> nodes) throws RemoteException {
		int rows = nodes.size();
		if (rows == 0) return;
		
		Node node;
		for (int i = 0; i < rows; i++) {
			node = nodes.get(i);
			node.getFeatures(arrs[i]);
			features.putRow(i, arrs[i]);
		}
		
		INDArray[] rs = resNet.evaluate(features);
		INDArray guesses = rs[0];
		INDArray values1 = rs[1];
		
		int[][] moves = null;
		INDArray guess;
		float score;
		for (int i = 0; i < rows; i++) {
			node = nodes.get(i);
			guess = guesses.getRow(i);
			moves = PolicyNetUtil.getMoves(guess, node.getRotation(), node.getLegalPoints());
			score = values1.getRow(i).getFloat(0);
			node.updateScore(score);
			NodeUtil.createChildNodes(node, moves);
			PolicyCache.put(node.getHash(), moves, score, node.getMoveNumber());
			node.active();
		}
	}
}
