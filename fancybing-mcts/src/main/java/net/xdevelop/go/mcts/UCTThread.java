package net.xdevelop.go.mcts;

import static net.xdevelop.go.Config.PUCT_C;

import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.xdevelop.go.Config;
import net.xdevelop.go.model.Board;
import net.xdevelop.go.model.Node;
import net.xdevelop.go.model.ValueCache;
import net.xdevelop.go.util.NodeUtil;
import net.xdevelop.go.util.ZoristHash;

public class UCTThread extends Thread {
	private final static ReadWriteLock LOCKER = new ReentrantReadWriteLock(false);
	public static long startTime = 0L;
	public static AtomicInteger expandCount = new AtomicInteger(0);
	public static AtomicInteger simCount = new AtomicInteger(0);
	public static AtomicInteger maxDepth = new AtomicInteger(0);
	public static AtomicInteger cacheHits = new AtomicInteger(0);
//	public static AtomicInteger cacheMistakes = new AtomicInteger(0);
	
	private SplittableRandom rotationRan = new SplittableRandom();
	
	private static Node ROOT = null;
	private static Board ROOT_BOARD = null;
	
	public UCTThread() {
	}
	
	public static void init(int threadsNum) {
		for (int i = 0; i < threadsNum; i++) {
			UCTThread t = new UCTThread();
			t.setName("UCT" + i);
			t.start();
		}
	}
	
	public void run() {
		Board board = new Board();
		while (true) {
			LOCKER.readLock().lock();
			Node root = ROOT;
			Board rootBoard = ROOT_BOARD;
			LOCKER.readLock().unlock();
			
			if (root == null) {
				try {
					sleep(10);
				} catch (InterruptedException e) {
				}
				continue;
			}
			
			Node node = select(root);
			synchronized(node) {
				if (node.isEndGame()) {
					node.updateScoreForPass();
					node.decreaseVLoss();
					continue;
				}
				else if (!node.expanding && !node.expanded) {
					node.expanding = true;
				}
				else { // already expanded
					node.decreaseVLoss();
					continue;
				}
			}
			
			board = rootBoard.copyTo(board);
			node.setRootMoveNumber(rootBoard.getMoveNumber());
			int[] moves = node.genMoves();
			board.playMoves(moves);
			
			int rotation = rotationRan.nextInt(8);
			node.setRotation(rotation);
			node.setFeatures(board.getFeatures(rotation));
			node.setLegalPoints(board.getLegalPoints());
//			if (ZoristHashL.hash(board.getPoints()) != board.hash()) {
//				System.err.println("Hash Error");
//			}
			node.setHash(ZoristHash.hash(board.getPoints()));
			expand(node, board.getKoIndex() >= 0);
		}
	}
	
	private Node select(Node root) {
		double maxScore = -99;
		int selIdx = -1;
		double score = 0;
		Node node;
		Node curSelect = root;
		Node[] child;
		int childLen;

		while (curSelect.expanded) {
			maxScore = -99;
			selIdx = -1;
			synchronized (curSelect) {
				child = curSelect.getChildNodes();
				childLen = child.length;
			
	  		 	// progress widening (1.0 / Math.log(1.8)) * Math.log(1.0/40 * curSelect.simCount.get()) + 2
//				if (!root.equals(curSelect) && !root.equals(curSelect.getParent())) {
//					int pwLen = Math.max((int) (/*2.9720134119884616*/ 1.7 * Math.log(0.025 * curSelect.getEstCount())), 3);
//					childLen = Math.min(childLen, pwLen);
//				}
				
				for (int i = 0; i < childLen; i++) {
					node = child[i];

					// refer to alphago PUCT
					score = node.getSelScore() + PUCT_C * node.getProbability() * ( Math.sqrt(curSelect.getEstCount()) / (1 + node.getEstCount()));
					if (score > maxScore && !node.expanding) {
						maxScore = score;
						selIdx = i;
					}
				}
				if (selIdx == -1) {
					break;
				}
				curSelect = child[selIdx];
				curSelect.addVLoss();
			}
		}
		return curSelect;
	}
	
	
	public static void expand(Node node, boolean inKo) {
		boolean expandSuccess = false;
		int hash = node.getHash();
		ValueCache cache = PolicyCache.get(hash);
		
//		if (cache != null && cache.moveNumber != node.getMoveNumber()) {
//			cacheMistakes.incrementAndGet();
//		}
		
		if (cache != null && cache.moveNumber == node.getMoveNumber() && !inKo) {
			node.updateScore(cache.score);
			NodeUtil.createChildNodes(node, cache.moves);
			node.active();
			cacheHits.incrementAndGet();
			
			expandSuccess = true;
		}
		else if (Evaluator.addNode(node)) {
			expandSuccess = true;
		}
		
		if (expandSuccess) {
			expandCount.incrementAndGet();
			if (ROOT_BOARD == null) return;
			int depth = node.getMoveNumber() - ROOT_BOARD.getMoveNumber();
			if (depth > maxDepth.get()) {
				maxDepth.set(depth);
			}
		}
	}
	
	public static void updateRoot(Node root, Board rootBoard) {
		LOCKER.writeLock().lock();
		ROOT = root;
		ROOT_BOARD = rootBoard;
		LOCKER.writeLock().unlock();
		long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
		if (elapsedTime > 0) {
			int newAvg = (int) (expandCount.intValue() / elapsedTime);
			Config.AVG_EVALUATE_SPEED +=  (newAvg - Config.AVG_EVALUATE_SPEED)  * 2 / ( 20 + 1 ) ;
		}
		startTime = System.currentTimeMillis();
		expandCount.set(0);
		simCount.set(0);
		maxDepth.set(0);
		cacheHits.set(0);
//		cacheMistakes.set(0);
	}
}
