package net.xdevelop.go.model;

import static net.xdevelop.go.BoardConstants.ADJCENT;
import static net.xdevelop.go.BoardConstants.ADJCENT_D;
import static net.xdevelop.go.Config.BOARD_INDEX_NUM;

import java.util.Arrays;

import net.xdevelop.go.Config;
import net.xdevelop.go.model.GoColor;
import net.xdevelop.go.util.LightList;
import net.xdevelop.go.util.LightSet;

/**
 * In order to get the similar playout performance just as C/C++, this class was over optimized and hard to read :(
 * Good news is it seems work correctly :)
 */
public final class Board {
	private final static int MAX_MOVE = Config.MAX_MOVE;
	public final static int BLACK = GoColor.BLACK.value();
	public final static int EMPTY = GoColor.EMPTY.value();
	public final static int WHITE = GoColor.WHITE.value();
	public final static int PASS = Config.PASS;
	
	// points status 当前棋盘各点的模式状态
	private int[] points = new int[BOARD_INDEX_NUM]; // -1 0 1 black/empty/white
	private int[] groupOfIndex = new int[BOARD_INDEX_NUM]; //the stone group index of each point 点所属棋块序号
	
	private LightList[] pointsInGroup = new LightList[MAX_MOVE]; // points list of each group 棋块点组合
	private int[] libertyOfPoint = new int[BOARD_INDEX_NUM]; // the liberty of each point 每点外气状态
	private int[] libertyOfGroup = new int[MAX_MOVE]; // the liberty of each stone group 棋块外气状态
	
	private LightSet[] emptyPointsOfGroup = new LightSet[MAX_MOVE]; // the empty points of each stone group 棋块外气点
	private int[] libertyOfGroupBefore = new int[MAX_MOVE]; // the empty points of each stone group of last move 保存前一手外气状态
	
	private int koIndex = 0; // ko point 打劫点
	private int moveNumber = 0;  // move number is begin with 1 第0手不用，从第1手开始，和棋谱手数一致
	private int lastMoveIndex = -1;

	private int curMoveIndex = -1; // 当前着点的棋盘索引位置
	private int toMoveColor = 0;
	private int[] historyMoves = new int[MAX_MOVE];
	private boolean[][] legalMoves = new boolean[2][361];   // array[0] for white, array[1] for black 0 是白   1为黑合法点
	private boolean[][] senseMoves = new boolean[2][361];   // 0 是白   1为黑有意义点
	
	private int continueAtari = 0;
	private int lastAtariColor = 0;
	private boolean suicideMove = false;
	private int killedNumber = 0;
	
	// tmp variables 临时变量
	private LightList affectFrdGroups = new LightList(4);
	private LightList affectOppGroups = new LightList(4);
	private LightList surroundGroups = new LightList(100);
	private LightList killedIndexes = new LightList(200);
	private LightSet affectPoints = new LightSet();
	
	public Board() {
		init();
	}
	
	/**
	 * 初始化
	 */
	public void init() {
		// initital empty board liberty 初始化空点外气
		libertyOfPoint = new int[] {
			2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3,
			2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2
		};
		
		for (int i = 0; i < BOARD_INDEX_NUM; i++) {
			legalMoves[0][i] = true;
			legalMoves[1][i] = true;
			senseMoves[0][i] = true;
			senseMoves[1][i] = true;
			points[i] = 0;
			groupOfIndex[i] = 0;
		}
		
		for (int i = 0; i < pointsInGroup.length; i++) {
			if (pointsInGroup[i] == null) {
				pointsInGroup[i] = new LightList();
				emptyPointsOfGroup[i] = new LightSet();
			}
			else {
				pointsInGroup[i].clear();
				emptyPointsOfGroup[i].clear();;
			}
			
			libertyOfGroup[i] = 0;
			libertyOfGroupBefore[i] = 0;
		}
		
		curMoveIndex = -1;
		lastMoveIndex = -1;
		moveNumber = 0;
		koIndex = -1;
		toMoveColor = 0;
	}
	
	public final boolean play(int color, int moveIndex) {
		moveNumber++;
		historyMoves[moveNumber] = moveIndex;
		int curGroup = moveNumber;
		this.lastMoveIndex = this.curMoveIndex;
		this.curMoveIndex = moveIndex;
		this.toMoveColor = -color;
		killedNumber = 0;
		killedIndexes.clear();
		
		suicideMove = false;
		
		// put ko point back to legal points 将打劫点重新加入合法着点
		if (koIndex >= 0) {
			legalMoves[color == BLACK ? 1 : 0][koIndex] = true;
		}
		koIndex = -1;
		if (moveIndex == PASS) return true;
		
		// make a new stone group for current move 建立棋块号
		points[moveIndex] = color;
		groupOfIndex[moveIndex] = curGroup;
		pointsInGroup[curGroup].add(moveIndex);
		
		// calculate the liberty changing 周围空点的气数变化计算
		affectOppGroups.clear();
		affectFrdGroups.clear();
		for (int adjIndex : ADJCENT[moveIndex]) {
			if (points[adjIndex] == EMPTY) { 
				emptyPointsOfGroup[curGroup].add(adjIndex); // find the empty point around current point 建立该点的外气点
				libertyOfPoint[adjIndex]--; // the adjacent point decrease liberty 邻空点外气减1
			}
			// Check if same color stone around, if true, merge them and re-calculate the liberty 检查相邻点，合并棋块和计算气的变化
			// 棋块融合，若周围有己方棋子，将老棋块与新棋块合并
			else if (points[adjIndex] == color) { // 有同色点
				int adjGroupNum = groupOfIndex[adjIndex]; // 邻点的所属棋块
				if (adjGroupNum != curGroup) { // 避免两次合并
					affectFrdGroups.add(adjGroupNum);
					libertyOfGroupBefore[adjGroupNum] = libertyOfGroup[adjGroupNum];
				
					pointsInGroup[adjGroupNum].addAll(pointsInGroup[curGroup]); // 合并到新棋块
					emptyPointsOfGroup[adjGroupNum].addAll(emptyPointsOfGroup[curGroup]); // 将新块的外气点加进来
					emptyPointsOfGroup[adjGroupNum].remove(moveIndex); // 落子点要从原外气点排除
					// update the merged groups to one group id 将所有组员的块号更新到一个块号
					int[] values = pointsInGroup[curGroup].values();
					for (int i = 0, len = values.length; i < len; i++) {
						int p = values[i]; 
						groupOfIndex[p] = adjGroupNum;
					}
					
					curGroup = adjGroupNum;
				}
			}
			else if (points[adjIndex] == -color) { // check if exists opponent stone group 有对方棋子
				// and calculate the liberty changing of opponent stone group 计算对方棋块的气
				int adjGroupNum = groupOfIndex[adjIndex];
				if (!affectOppGroups.contains(adjGroupNum)) {
					libertyOfGroupBefore[adjGroupNum] = libertyOfGroup[adjGroupNum]; // 保存原棋块的气
					emptyPointsOfGroup[adjGroupNum].remove(moveIndex); // 将当前着点从原棋块外气点中移除
					affectOppGroups.add(adjGroupNum);
				}
			}
		}
		
		// Update the liberty status of current move's stone group更新当前落子棋块的外气值
		emptyPointsOfGroup[curGroup].remove(moveIndex); // 当前着点要从气中扣除
		int liberties = emptyPointsOfGroup[curGroup].size();
		libertyOfGroup[curGroup] = liberties;
		int[] values = pointsInGroup[curGroup].values();
		for (int i = 0, len = values.length; i < len; i++) {
			int pIdx = values[i];
			libertyOfPoint[pIdx] = liberties;
		}
		
		if (liberties == 0) {
			suicideMove = true;
		}
		
		// Update the affected opponent stone group liberty status 更新涉及到的对方棋块成员点的外气值
		values = affectOppGroups.values();
		surroundGroups.clear();
		int[] tmp;
		for (int gId : values) {
			// calc the opponent stone group liberty 计算对方棋块的气
			liberties = emptyPointsOfGroup[gId].size();
			libertyOfGroup[gId] = liberties;
			tmp = pointsInGroup[gId].values();
			if (liberties == 0) {  // dead group 计算提子
				killedIndexes.addAll(tmp);
				killedNumber += tmp.length;
				for (int j = 0, len2 = tmp.length; j < len2; j++) {
					int idx = tmp[j];
					points[idx] = EMPTY;
					groupOfIndex[idx] = 0;
					// calculate surround stone group liberty
					for (int adjIndex : ADJCENT[idx]) {
						int adjGroupNum = groupOfIndex[adjIndex];
						if (adjGroupNum > 0 && adjGroupNum != gId) { // 包围异色棋块
							emptyPointsOfGroup[adjGroupNum].add(idx);  // 增加外气点
							if (!surroundGroups.contains(adjGroupNum)) { // 每个提子只更新一次
								if (!affectFrdGroups.contains(adjGroupNum)) { // 当前合并棋块已经取过before值 
									libertyOfGroupBefore[adjGroupNum] = libertyOfGroup[adjGroupNum];
								}
								surroundGroups.add(adjGroupNum);
							}
						}
					}
				}
				pointsInGroup[gId].clear(); // clear dead group 死子棋块清除
				affectOppGroups.remove(gId);
			}
			else {
				for (int i = 0, len = tmp.length; i < len; i++) {
					libertyOfPoint[tmp[i]] = liberties;
				}
			}
		}
		
		// update surround stone group liberty 更新受提子影响的棋块的成员点外气
		values = surroundGroups.values();
		for (int gId : values) {
			// 计算死子外围棋块的气
			liberties = emptyPointsOfGroup[gId].size();
			libertyOfGroup[gId] = liberties;
			tmp = pointsInGroup[gId].values();
			for (int i = 0, len = tmp.length; i < len; i++) {
				libertyOfPoint[tmp[i]] = liberties;
			}
		}
		
		// check if it is ko 计算打劫，提子数量为1，自身只有一子且只有一口气
		if (killedNumber == 1 && libertyOfGroup[curGroup] == 1 && pointsInGroup[curGroup].size() == 1) {
			// 是打劫
			koIndex = killedIndexes.get(0);
			libertyOfPoint[koIndex] = 0;
		}
		else if (killedNumber >= 1) { // 当提子数>=1时，
			values = killedIndexes.values();
			for (int i = 0, len = values.length; i < len; i++) {
				int index = values[i]; 
				// 将提子点的外气重新计算
				libertyOfPoint[index] = 0;
				for (int adjIndex : ADJCENT[index]) {
					if (points[adjIndex] == EMPTY) {
						libertyOfPoint[index]++;
					}
				}
			}
		}
		
		// update legal moves
		updatePointStatus(curGroup);
		
		return true;
	}
	
	private final void updatePointStatus(int curGroup) {
		affectPoints.clear();
		
		// remove current move from the legal move 将当前着点从合法着点中移除
		legalMoves[0][this.curMoveIndex] = false;
		legalMoves[1][this.curMoveIndex] = false;
		
		// 当前受影响点
		int[] values = affectFrdGroups.values();
		for (int gid : values) {
			if (libertyOfGroupBefore[gid] == 1 || libertyOfGroup[gid] == 1) {
				affectPoints.addAll(emptyPointsOfGroup[gid]);
			}
		}
		affectPoints.addAll(emptyPointsOfGroup[curGroup]);
		
		values = affectOppGroups.values();
		boolean atari = false;
		for (int gid : values) { 
			if (libertyOfGroup[gid] == 1) { // 当前着点周围棋块变1气
				atari = true;
				int p = emptyPointsOfGroup[gid].get(0);
				affectPoints.add(p);
			}
		}
		
		// the flag to indicate if in ladder, this flag is used to extract ladder train data
		if (atari) {
			if (lastAtariColor == toMoveColor) {
				continueAtari++;
			}
			else {
				lastAtariColor = toMoveColor;
				continueAtari = 1;
			}
		}
		else if (lastAtariColor == toMoveColor) {
			lastAtariColor = 0;
			continueAtari = 0;
		}
		
		// 提子影响点
		values = surroundGroups.values();
		for (int gid : values) {
			if (libertyOfGroupBefore[gid] == 1) { // 提子包围棋块从1气变多气
				affectPoints.addAll(emptyPointsOfGroup[gid]);
			}
		}
		
		// add captured points into affected points 提子
		affectPoints.addAll(killedIndexes.values());
		
		int[] ps = affectPoints.values();
		for (int i = 0, l = ps.length; i < l; i++) {
			int index = ps[i];
			if (libertyOfPoint[index] > 0) {
				legalMoves[0][index] = true;
				legalMoves[1][index] = true;
			}
			else { // 检查无气空点
				legalMoves[0][index] = false; // 先设为禁着点
				legalMoves[1][index] = false;
				int sameColor = 0;
				int len = ADJCENT[index].length;
				for (int adjIndex : ADJCENT[index]) {
					sameColor += points[adjIndex];
				}
				
				if (sameColor == len) sameColor = 1;
				else if (sameColor == -len) sameColor = -1;
				else sameColor = -99;
				
				if (sameColor == -99) { // 四周非同色，即非眼
					for (int adjIndex : ADJCENT[index]) {
						if ( (points[adjIndex] == WHITE && libertyOfPoint[adjIndex] > 1) // 有相邻2气以上白棋
						  || (points[adjIndex] == BLACK && libertyOfPoint[adjIndex] == 1)	// 白可以提子
								) { 
							legalMoves[0][index] = true;
							break;
						}
					}
				
					for (int adjIndex : ADJCENT[index]) {
						if ( (points[adjIndex] == BLACK && libertyOfPoint[adjIndex] > 1) // 有相邻2气以上黑棋
						  || (points[adjIndex] == WHITE && libertyOfPoint[adjIndex] == 1) // 黑可以提子
						  ) { 
							legalMoves[1][index] = true;
							break;
						}
					}
				}
				else { // check eye 眼
					for (int adjIndex : ADJCENT[index]) {
						if (libertyOfPoint[adjIndex] == 1) { // can be captured, it is legal point 可以提子
							legalMoves[sameColor == BLACK ? 0 : 1][index] = true;
							break;
						}
					}
					
					boolean border = ADJCENT_D[index].length <= 2;
					int oppCount = 0;
					for (int adjIndex : ADJCENT_D[index]) {
						if (points[adjIndex] == -sameColor) {
							oppCount++;
						}
					}
					
					if (oppCount >= 2 || (border && oppCount >= 1)) { // fake eye 假眼
						for (int adjIndex : ADJCENT[index]) {
							if (libertyOfPoint[adjIndex] > 1) { // not exists suicide, it is legal move 不存在自杀
								legalMoves[sameColor == BLACK ? 1 : 0][index] = true;
								senseMoves[sameColor == BLACK ? 1 : 0][index] = true;
								break;
							}
						}
					}
					else {
						legalMoves[sameColor == BLACK ? 1 : 0][index] = true;
						senseMoves[sameColor == BLACK ? 1 : 0][index] = false;
					}
				}
			}
		}
		
		// ko
		if (koIndex >= 0) {
			legalMoves[toMoveColor == BLACK ? 1 : 0][koIndex] = false;
		}
	}
	
	public final boolean isLegal(int index) {
		return legalMoves[toMoveColor == BLACK ? 1 : 0][index];
	}
	
	public final boolean isEmpty(int index) {
		return points[index] == EMPTY;
	}
	
	public int[] getAffectPoints() {
		return affectPoints.values();
	}
	
	public final boolean[] getLegalPoints() {
		if (toMoveColor == BLACK) {
			return Arrays.copyOf(legalMoves[1], 361);
		}
		else {
			return Arrays.copyOf(legalMoves[0], 361);
		}
	}
	
	public final boolean[] getLegalPoints(int toMoveColor) {
		if (toMoveColor == BLACK) {
			return legalMoves[1];
		}
		else {
			return legalMoves[0];
		}
	}

	public final boolean[] getSensePoints(int toMoveColor) {
		if (toMoveColor == BLACK) {
			return senseMoves[1];
		}
		else {
			return senseMoves[0];
		}
	}
	
	/**
	 * Get features for policy network
	 * 
	 * @param rotationMode
	 * @return
	 */
	public final float[] getFeatures(int rotationMode) {
	 	float[] arr = new float[Config.FEATURE_COLUMNS];
		int bSeq = 0, wSeq = 1;
		int m = 0;
		if (toMoveColor == WHITE) {
			bSeq = 1;
			wSeq = 0;
		}
		
		int index = 0;
		for (int i = 0; i < BOARD_INDEX_NUM; i++) {
			index = rotateIndex(i, rotationMode);
			arr[bSeq * 361 + index] = points[i] == BLACK ? 1 : 0;
			arr[wSeq * 361 + index] = points[i] == WHITE ? 1 : 0;
			if (points[i] == EMPTY) {
				arr[2 * 361 + index] = 1;
				
				arr[3 * 361 + index] = 0;
				arr[4 * 361 + index] = 0;
				arr[5 * 361 + index] = 0;
				arr[6 * 361 + index] = 0;
			}
			else {
				arr[2 * 361 + index] = 0;
				
				arr[3 * 361 + index] = libertyOfPoint[i] == 1 ? 1 : 0;
				arr[4 * 361 + index] = libertyOfPoint[i] == 2 ? 1 : 0;
				arr[5 * 361 + index] = libertyOfPoint[i] == 3 ? 1 : 0;
				arr[6 * 361 + index] = libertyOfPoint[i] >= 4 ? 1 : 0;
			}
			arr[7 * 361 + index] = koIndex >= 0 ? 1 : 0;
			
			// history move feature
			m = -1;
			for (int j = 0; j < 8; j++) {
				if (moveNumber - j >= 0 && i == historyMoves[moveNumber - j]) {
					m = j;
					break;
				}
			}
			if (m >= 0) {
				arr[8 * 361 + index] = (float) Math.exp(-m * 0.1);
			}
			else {
				arr[8 * 361 + index] = 0;
			}
			
			arr[9 * 361 + index] = toMoveColor == BLACK ? 1 : 0;
		}
		return arr;
	}
	
    public static int rotateIndex(int originIndex, int rotationMode) {
    	int index = 0;
		int y = originIndex / 19;
		int x = originIndex - y * 19;

		switch (rotationMode) {
		case 0:
			index = originIndex;
			break;
		case 1:
			index = y * 19 + 19 - x - 1;
			break;
		case 2:
			index = (19 - y - 1) * 19 + x;
			break;
		case 3:
			index = x * 19 + y;
			break;
		case 4:
			index = x * 19 + 19 - y - 1;
			break;
		case 5:
			index = (19 - x - 1) * 19 + y;
			break;
		case 6:
			index = (19 - y - 1) * 19 + 19 - x - 1;
			break;
		case 7:
			index = (19 - x - 1) * 19 + 19 - y - 1;
			break;
		default:
			index = originIndex;
		}
		
		return index;
    }
    
    public static int getOriginIndex(int originIndex, int rotationMode) {
    	int index = 0;
		int y = originIndex / 19;
		int x = originIndex - y * 19;

		switch (rotationMode) {
		case 0:
			index = originIndex;
			break;
		case 1:
			index = y * 19 + 19 - x - 1; //y * 19 + 19 - x - 1;
			break;
		case 2:
			index = (19 - y - 1) * 19 + x;
			break;
		case 3:
			index = x * 19 + y; //x * 19 + y;
			break;
		case 4:
			index = (19 - x - 1) * 19 + y; //x * 19 + 19 - y - 1;
			break;
		case 5:
			index = x * 19 + 19 - y -1; //(19 - x - 1) * 19 + y;
			break;
		case 6:
			index = (19 - y - 1) * 19 + 19 - x - 1;
			break;
		case 7:
			index = (19 - x - 1) * 19 + 19 - y - 1;
			break;
		default:
			index = originIndex;
		}
		
		return index;
    }    
    
	public final int[] getPoints() {
		return points;
	}
	
	public boolean isSuicideMove() {
		return this.suicideMove;
	}
	
	public int getCaptureNum() {
		return this.killedNumber;
	}
	
	/**
	 * Copy board status to another board object 拷贝当前Board状态到另一个Board
	 * 
	 * @param board
	 * @return
	 */
	public final Board copyTo(Board board) {
		for (int i = 0; i < BOARD_INDEX_NUM; i++) {
			board.libertyOfPoint[i] = libertyOfPoint[i];
			board.legalMoves[0][i] = legalMoves[0][i];
			board.legalMoves[1][i] = legalMoves[1][i];
			board.senseMoves[0][i] = senseMoves[0][i];
			board.senseMoves[1][i] = senseMoves[1][i];
			board.points[i] = points[i];
			board.groupOfIndex[i] = groupOfIndex[i];
		}
		
		for (int i = 0; i < MAX_MOVE; i++) {
			board.historyMoves[i] = historyMoves[i];
			board.pointsInGroup[i].clear();
			board.emptyPointsOfGroup[i].clear();
			board.pointsInGroup[i].addAll(pointsInGroup[i]);
			board.emptyPointsOfGroup[i].addAll(emptyPointsOfGroup[i]);
			board.libertyOfGroup[i] = libertyOfGroup[i];
			board.libertyOfGroupBefore[i] = libertyOfGroupBefore[i];
		}
		
		board.curMoveIndex = curMoveIndex;
		board.lastMoveIndex = lastMoveIndex;
		board.moveNumber = moveNumber;
		board.koIndex = koIndex;
		board.toMoveColor = toMoveColor;
		
		board.continueAtari = continueAtari;
		return board;
	}
	
	public final int getMoveNumber() {
		return moveNumber;
	}
	
	/**
	 * Play all moves and return the pattern mask before last move
	 * 
	 * @param moves
	 * @return
	 */
	public final boolean playMoves(int[] moves) {
		for (int mv : moves) {
			play(toMoveColor, mv);
		}
		return true;
	}
	
	public int getContinueAtariNum() {
		return continueAtari;
	}
	
	public int getKoIndex() {
		return this.koIndex;
	}
}