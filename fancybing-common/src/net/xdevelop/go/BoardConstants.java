package net.xdevelop.go;

import static net.xdevelop.go.Global.BOARD_INDEX_NUM;

import net.xdevelop.go.util.LightList;

public final class BoardConstants {
	public final static int[] NAKADE_CENTER = new int[] {0, 0, 0, 2, 3, 3, 4}; // 点眼点的气数
	public static int[][] ADJCENT; // 邻居点
	public static int[][] ADJCENT_D; // 斜线上的邻居点
	
	public static void init() {
		initAdjacent();
	}
	
	private final static void initAdjacent() {
		// 初始化每个点的邻居点数组及气数
		ADJCENT = new int[BOARD_INDEX_NUM][4];
		LightList pointList = new LightList(4);
		for (int y = 0; y < 19; y++) {
			for (int x = 0; x < 19; x++) {
				if (x > 0) {
					pointList.add(y * 19 + x - 1);
				}
				if (x < Global.BOARD_SIZE - 1) {
					pointList.add(y * 19 + x + 1);
				}
				if (y > 0) {
					pointList.add((y - 1) * 19 + x);
				}
				if (y < Global.BOARD_SIZE - 1) {
					pointList.add((y + 1) * 19 + x);
				}
				
				ADJCENT[y * 19 + x] = new int[pointList.size()];
				for (int i = 0; i < ADJCENT[y * 19 + x] .length; i++) {
					ADJCENT[y * 19 + x][i] = pointList.get(i);
				}
				
				pointList.clear();
			}
		}
		
		// 邻斜角点
		ADJCENT_D = new int[BOARD_INDEX_NUM][4];
		pointList = new LightList(4);
		for (int y = 0; y < 19; y++) {
			for (int x = 0; x < 19; x++) {
				for (int m = x - 1; m <= x + 1; m++) {
					for (int n = y - 1; n <= y + 1; n++) {
						if (m >= 0 && m < Global.BOARD_SIZE && n >= 0 && n < Global.BOARD_SIZE && m != x && n != y) {
							pointList.add(n * 19 + m);
						}
					}
				}
				
				ADJCENT_D[y * 19 + x] = new int[pointList.size()];
				for (int i = 0; i < ADJCENT_D[y * 19 + x].length; i++) {
					ADJCENT_D[y * 19 + x][i] = pointList.get(i);
				}
				
				pointList.clear();
			}
		}
	}
}