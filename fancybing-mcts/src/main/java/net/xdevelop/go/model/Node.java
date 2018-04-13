package net.xdevelop.go.model;


import static net.xdevelop.go.model.Board.BLACK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.nd4j.linalg.api.ndarray.INDArray;

import com.google.common.util.concurrent.AtomicDouble;

import net.xdevelop.go.Config;
import net.xdevelop.go.util.LightList;
import net.xdevelop.go.util.SgfUtil;

public class Node implements Comparable<Node> {
	private final static Node[] NODES = new Node[0]; // for toArray cast
	
	public volatile boolean expanding = false;
	public volatile boolean expanded = false;
	
	private int moveNumber = 0;
	private float probability = 0; // PolicyNet move probability value
	private int nodeColor = 0; 
	private int moveIndex = 0;  // current move 当前着点
	private int rootMoveNumber = 0;
	private int rotation = 0;
	
	private Node parent;
	private float[] features;
	private boolean[] legalPoints;
	private AtomicInteger vLoss = new AtomicInteger(0);
	private AtomicInteger estCount = new AtomicInteger(0);  // estimate count 子节点总共评估次数
	private AtomicDouble boardScore = new AtomicDouble(0);  // board estimate value which base on black view 基于围棋的局面评估分
	private float initScore = 0;
	
	private ArrayList<Node> child;
	private volatile Node[] childNodes;
	private boolean isEndGame;
	
	private int hash = 0;
	
	public Node(Node parent, int nodeColor, int moveIndex, int moveNumber) {
		this.parent = parent;
		this.nodeColor = nodeColor;
		this.moveIndex = moveIndex;
		this.moveNumber = moveNumber;
		this.child = new ArrayList<Node>(Config.POLICY_NETWORK_TOP_N);
		if (parent != null) {
			if (parent.moveIndex == Config.PASS && moveIndex == Config.PASS) {
				isEndGame = true;
			}
		}
	}
	
	/**
	 * Get moves from root node to current node
	 * 获取从根节点到当前节点的所有着点
	 * @return
	 */
	public int[] genMoves() {
		int[] moves;
		Node p = parent;
		if (p == null || moveNumber <= rootMoveNumber) { // root node 根节点
			moves = new int[0];
			return moves;
		}
		
		LightList list = new LightList();
		list.add(moveIndex);
		while (p != null && p.moveNumber > rootMoveNumber) {
			list.add(p.moveIndex);
			p = p.parent;
		}
		moves = list.descValues();
		return moves;
	}
	
	public void setLegalPoints(boolean[] legalPoints) {
		this.legalPoints = legalPoints;
	}
	
	public boolean[] getLegalPoints() {
		return this.legalPoints;
	}
	
	public void setFeatures(float[] features) {
		this.features = features;
	}
	
	public void getFeatures(INDArray arr) {
		int len = features.length;
		for (int i = 0; i < len; i++) {
			arr.putScalar(i, features[i]);
		}
	}
	
	public int getHash() {
		return hash;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	public void updateScore(double boardValue) {
		if (this.nodeColor == BLACK) { // to move color is WHITE, evaluate value is base on white, convert it to base black
			boardValue = -boardValue;
		}
		
		estCount.incrementAndGet();
		boardScore.addAndGet(boardValue);
		
		// Update to root node 往根节点方向更新
		Node p = parent;
		while (p != null && p.moveNumber >= rootMoveNumber) {
			p.estCount.incrementAndGet();
			p.boardScore.addAndGet(boardValue);
			p = p.parent;
		}
	}
	
	/**
	 * Update score for pass move, increase the estimate count and value keep same
	 */
	public void updateScoreForPass() {
		double boardValue = boardScore.get();
		if (estCount.get() > 0) {
			boardValue = boardScore.get() / estCount.get();
		}
		
		estCount.incrementAndGet();
		boardScore.addAndGet(boardValue);
		
		// Update to root node 往根节点方向更新
		Node p = parent;
		while (p != null && p.moveNumber >= rootMoveNumber) {
			p.estCount.incrementAndGet();
			p.boardScore.addAndGet(boardValue);
			p = p.parent;
		}
	}
	
	public float getScore(int virLoss) {
		int estcount = estCount.intValue();
		if (estcount == 0) {
			if (nodeColor == BLACK) {
				return initScore;
			}
			else {
				return 1- initScore;
			}
		}
		
		estcount += virLoss;
		float bvScore = boardScore.floatValue() / estcount;
		bvScore = bvScore / 2.0f + 0.5f;
		
		if (nodeColor == BLACK) {
			return bvScore;
		}
		else {
			return 1 - bvScore;
		}
	}
	
	public float getScore() {
		return getScore(0);
	}
	
	public float getSelScore() {
		return getScore(vLoss.get());
	}
	
	/**
	 * Raw board estimate value which is base on Black view
	 * @return
	 */
	public float getBoardScore() {
		int count = estCount.intValue();
		if (count <= 0) {
			return 0f;
		}
		
		float bvScore = boardScore.floatValue() / count;
		return bvScore / 2.0f + 0.5f;
	}
	
	public Node[] getChildNodes() {
		return childNodes == null ? new Node[0] : childNodes;
	}
	
	public synchronized Node[] getSortedChildNodes() {
		Node[] nodes = getChildNodes();
		Arrays.sort(nodes);
		return nodes;
	}
	
	public Node getMaxNode() {
		int len = childNodes == null ? 0 : childNodes.length;
		if (len == 0) {
			return null;
		}
		int bestIndex = 0;
		int maxCount = childNodes[bestIndex].estCount.get();
		for (int i = 1; i < len; i++) {
			if (childNodes[i].estCount.get() > maxCount) {
				bestIndex = i;
				maxCount = childNodes[i].estCount.get();
			}
		}
		return childNodes[bestIndex];
	}
	
	public void addChild(Node node) {
		child.add(node);
	}
	
	public void active() {
		childNodes = child.toArray(NODES);
		decreaseVLoss();
		this.expanded = true;
		this.expanding = false;
	}

	/**
	 * Get the best route moves
	 * 取该节点最佳变化
	 * @return
	 */
	public String bestRoute() {
		Node node = getMaxNode();
		StringBuilder sb = new StringBuilder();
		sb.append("--").append(nodeColor == Board.BLACK ? "B[" : "W[").append(SgfUtil.index2String(moveIndex)).append("];");
		int depth = 1;
		while(node != null) {
			depth++;
			sb.append(node.nodeColor == Board.BLACK ? "B[" : "W[").append(SgfUtil.index2String(node.moveIndex)).append("];");
			node = node.getMaxNode();
		}
		return sb.append(depth).toString();
	}
	

	public void addVLoss() {
		vLoss.addAndGet(Config.V_LOSS);
	}
	
	public void decreaseVLoss() {
		vLoss.addAndGet(-Config.V_LOSS);
		Node p = parent;
		while (p != null && p.moveNumber >= rootMoveNumber) {
			p.vLoss.addAndGet(-Config.V_LOSS);
			p = p.parent;
		}
	}
	
	public void clearChild() {
		synchronized (this) {
			child.clear();
			if (childNodes != null) {
				for (int i = 0; i < childNodes.length; i++) {
					childNodes[i] = null;
				}
				childNodes = null;
			}
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Node)) {
			return false;
		}
		Node node = (Node) obj;
		return node.moveIndex == moveIndex && node.moveNumber == moveNumber && node.nodeColor == nodeColor;
	}

	@Override
	public int compareTo(Node o) {
		if (o == null) {
			return -1;
		}
		return -Integer.compare(estCount.get(), o.estCount.get()); // descending
	}
	
	public Node getParent() {
		return parent;
	}
	
	public int getEstCount() {
		return estCount.get();
	}
	
	public boolean isEndGame() {
		return isEndGame;
	}
	
	public int getMoveNumber() {
		return moveNumber;
	}

	public void setMoveNumber(int moveNumber) {
		this.moveNumber = moveNumber;
	}

	public float getProbability() {
		return probability;
	}

	public void setProbability(float probability) {
		this.probability = probability;
	}

	public int getNodeColor() {
		return nodeColor;
	}

	public void setNodeColor(int nodeColor) {
		this.nodeColor = nodeColor;
	}

	public int getMoveIndex() {
		return moveIndex;
	}

	public void setMoveIndex(int moveIndex) {
		this.moveIndex = moveIndex;
	}

	public int getRootMoveNumber() {
		return rootMoveNumber;
	}

	public void setRootMoveNumber(int rootMoveNumber) {
		this.rootMoveNumber = rootMoveNumber;
	}

	public int getRotation() {
		return rotation;
	}

	public void setRotation(int rotation) {
		this.rotation = rotation;
	}
	
	public int getDepth(int rootMoveNumber) {
		return moveNumber - rootMoveNumber;
	}
	
	public void setInitScore(float initScore) {
		this.initScore = initScore;
	}
}
