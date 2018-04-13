package net.xdevelop.go.mcts;

import static net.xdevelop.go.Config.EVALUATE_BATCH_SIZE;
import static net.xdevelop.go.Config.NETWORK_THREADS_NUM;
import static net.xdevelop.go.Config.POLICY_NETWORK_TOP_N;

import net.xdevelop.go.Config;
import net.xdevelop.go.model.Board;
import net.xdevelop.go.model.GoColor;
import net.xdevelop.go.model.Node;

public class MCTS {
	private static MCTS instance;
	
	private int maxTime = 20; // 最长思考时间, 上一手预测未命中的情况
	private int timeout = 15;
	private long start;
	
	private Node root;
	private boolean isPondering = true;
	
	private MCTS(boolean pondering) {
		isPondering = pondering;
		root = new Node(null, GoColor.BLACK.value(), -1, 0);
		
		Evaluator.init(NETWORK_THREADS_NUM, POLICY_NETWORK_TOP_N, EVALUATE_BATCH_SIZE);
		UCTThread.init(Config.UCT_THREADS_NUM);
		Monitor.init(this);
		PolicyCache.clearAll();
	}
	
	public void reset() {
		root = new Node(null, GoColor.BLACK.value(), -1, 0);
		PolicyCache.clearAll();
	}
	
	public synchronized static MCTS getInstance(boolean pondering) {
		if (instance == null) {
			instance = new MCTS(pondering);
		}
		return instance;
	}
	
	/**
	 * Select the best move against current board situation
	 * 
	 * @return
	 */
	public int genmove(int toMoveColor, Board inputBoard, int curMoveIndex, int lastMoveIndex, int curMoveNumber) {
		if (curMoveNumber <= 80) {
			Config.IN_OPEN = true;
			Config.IN_MID = false;
		}
		else if (curMoveNumber <= 180) {
			Config.IN_MID = true;
			Config.IN_OPEN = false;
		}
		else {
			Config.IN_MID = false;
			Config.IN_OPEN = false;
		}
		
		PolicyCache.setCurrentMoveNumber(curMoveNumber);
		
		start = System.currentTimeMillis();
		
		// 生成新根节点前必须拷贝Board，否则原Board的变化会影响模拟和评估线程的Board状态
		Board rootBoard = inputBoard.copyTo(new Board());
		
		// 初始化根结点
		int rootNodeColor = -toMoveColor;
		initRootNode(rootNodeColor, curMoveIndex, curMoveNumber, rootBoard);
		
		while (!timeout()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		
		// 取结果
		Node maxNode = root.getMaxNode();
		try {
			if (maxNode == null) {
				return Board.PASS;
			}
			return maxNode.getMoveIndex();
		} finally {
			if (isPondering && maxNode != null) {
				pondering(-maxNode.getNodeColor(), maxNode.getMoveIndex(), curMoveNumber + 1, rootBoard);
			}
			else {
				UCTThread.updateRoot(null, null);
			}
		}
	}
	
	public static void stopPondering() {
		UCTThread.updateRoot(null, null);
	}
	
	public void pondering(int toMoveColor, int curMoveIndex, int curMoveNumber, Board rootBoard) {
		int rootColor = -toMoveColor;
		Board newRootBoard = new Board();
		newRootBoard = rootBoard.copyTo(newRootBoard);
		newRootBoard.play(rootColor, curMoveIndex);
		initRootNode(rootColor, curMoveIndex, curMoveNumber, newRootBoard);
		PolicyCache.clearCache();
	}
	
	private void initRootNode(int rootColor, int curMoveIndex, int moveNumber, Board rootBoard) {
		// 先从当前子节点寻找
		Node[] nodes = root.getChildNodes();
		Node nextRoot = null;
		for (Node node : nodes) {
			if (node.getNodeColor() == rootColor && node.getMoveIndex() == curMoveIndex) {
				nextRoot = node;
				break;
			}
		}
		
		// 之前预测没有命中
		if (nextRoot == null) {
			root = new Node(null, rootColor, curMoveIndex, moveNumber);
		}
		else { // 原树结点命中
			root.clearChild();
			root = nextRoot;
		}
		timeout = maxTime;
		UCTThread.updateRoot(root, rootBoard);
	}
	
	public Node getRoot() {
		return root;
	}
	
	/**
	 * Set thinking time
	 * @param maxTime
	 */
	public void setThinkingTime(int maxTime) {
		this.maxTime = maxTime;
	}
	
	public boolean timeout() {
		int timeElapsed = (int) ((System.currentTimeMillis() - start) / 1000);
		if (timeElapsed > maxTime) {
			return true;
		}
		Node[] child = root.getSortedChildNodes();
		
		// 前10手
		if (root.getMoveNumber() < 4 && timeElapsed > 1.2) {
			return true;
		}
		else if (root.getMoveNumber() < 10 && timeElapsed > 8) {
			return true;
		}

		int scoreDiff = -1;
		if (child.length >= 2) {
			scoreDiff = child[0].getEstCount() - child[1].getEstCount();
		}
		else if (child.length == 1) {
			return true;
		}
		else {
			return false;
		}
 		
		// discard the non-contend nodes
		int remainderTime = maxTime - timeElapsed;
		int threshold = (int) (remainderTime * Config.AVG_EVALUATE_SPEED * 0.7f);
		for (int i = 2, len = child.length; i < len; i++) {
			if (child[i].getProbability() < 0) {
				continue;
			}
			int diff = child[0].getEstCount() - child[i].getEstCount();
			if (diff > threshold) {
				child[i].setProbability(-1f);
				System.err.println(String.format("Node %d discarded", i)); // use err.print because it can print red message in eclipse
			}
		}
		
		// early stop 
		if (scoreDiff > Math.max(1500, (timeout - timeElapsed) * Config.AVG_EVALUATE_SPEED) * 0.8) {
			System.err.println("Early stop thinking");
			return true;
		}
		
		return false;
	}
}
