package net.xdevelop.go.util;

import net.xdevelop.go.model.Board;
import net.xdevelop.go.model.Node;

public final class NodeUtil {
	
	/**
	 * Create then return the first node
	 * @param node
	 * @param indexes
	 * @return
	 */
	public static void createChildNodes(Node node, int[][] indexes) {
		int moveNumber = node.getMoveNumber() + 1;
		int nextMoveColor = -node.getNodeColor();
		float parentScore = node.getBoardScore(); 
		int count = 0;
		Node firstNode = null;
		for (int[] index : indexes) {
			if (index[0] == Board.PASS) continue;
			count++;
			Node childNode = new Node(node, nextMoveColor, index[0], moveNumber);
			childNode.setProbability(index[1] / 1000f); //index[1] / 1000f; //
//			childNode.setProbability((Math.max(10, index[1])) / 1000f);
			node.addChild(childNode);
			
			if (firstNode == null) {
				childNode.setInitScore(parentScore);
				firstNode = childNode;
				parentScore *= 0.5f;
			}
			else {
				float initScore = parentScore + parentScore * (float) Math.sqrt(1 - childNode.getProbability() / firstNode.getProbability()) ;
				initScore = Math.max(0, initScore);
				childNode.setInitScore(initScore);
			}
		}
		if (count == 0) {
			Node childNode = new Node(node, nextMoveColor, Board.PASS, moveNumber);
			childNode.setProbability(1);
			node.addChild(childNode);
			childNode.setInitScore(parentScore);
		}
	}
}
