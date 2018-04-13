package net.xdevelop.go.mcts;

import net.xdevelop.go.Config;
import net.xdevelop.go.model.GoColor;
import net.xdevelop.go.model.Node;
import net.xdevelop.go.util.SgfUtil;

public class Monitor extends Thread {
	private MCTS mcts;
	public Monitor(MCTS mcts) {
		this.mcts = mcts;
	}
	
	public static void init(MCTS mcts) {
		Thread t = new Monitor(mcts);
		t.setName("Monitor");
		t.setDaemon(true);
		t.start();
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Node root = mcts.getRoot();
				if (root != null) {
					System.out.println(String.format("%s simCount: %d score: %f  moveNumber: %d expanded: %s", 
							root.getNodeColor() == GoColor.BLACK.value() ? "White" : "Black", root.getEstCount(), 1 - root.getScore(), root.getMoveNumber(), root.expanded));
					
					System.out.println(String.format("simCount: %d  expandCount: %d  maxDepth: %d  KOMI: %f  AvgSpeed: %d  CacheHits: %d ", 
							UCTThread.simCount.get(), UCTThread.expandCount.get(), UCTThread.maxDepth.get(),
							Game.getKomi(), Config.AVG_EVALUATE_SPEED, UCTThread.cacheHits.get()));
				}
				else {
					System.out.println("Empty root");
				}
				System.out.println(String.format("PolicyNetPool: %d avgEva: %f count: %d ",
						Evaluator.size(), Evaluator.getAvgEvaluate(), Evaluator.COUNT.intValue()));
				
				if (root != null) {
					Node[] child = root.getSortedChildNodes();
					int i = 0;
					for (Node node : child) {
						if (node == null) continue;

						System.out.println(String.format("%d move %s  probability: %f  sim: %d  Score: %f", 
								i, SgfUtil.index2String(node.getMoveIndex()), node.getProbability(), node.getEstCount(), node.getScore()));
						if (i < 8) {
							System.out.println(node.bestRoute());
						}
						else if (i > 20) {
							break;
						}
						i++;
					}
				}
				
				System.out.println("====================================");
				try {
					sleep(2000);
				} catch (InterruptedException e) {
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
