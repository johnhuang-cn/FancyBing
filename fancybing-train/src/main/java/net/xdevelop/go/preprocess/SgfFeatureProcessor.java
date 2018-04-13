package net.xdevelop.go.preprocess;

import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.EMPTY;
import static net.sf.gogui.go.GoColor.WHITE;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.StringInfo;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.sgf.SgfReader;
import net.xdevelop.go.preprocess.util.FeatureUtil;

/**
 * Extract feature from sgf
 */
public class SgfFeatureProcessor {
	private final static String ALLONE = "1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
	private final static String ALLZERO = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	private Game game;
	private double rsValue65;
	private double rsValue75;
	private boolean inKo = false;
	
	public SgfFeatureProcessor(String sgfFile) throws Exception {
		InputStream in = new FileInputStream(sgfFile);
		SgfReader reader = new SgfReader(in, null, null, 0);
		game = new Game(reader.getTree());
		if (game.getBoard().isSetupHandicap()) {
			throw new Exception("handicap game");
		}
		in.close();
	}
	
	private String getStoneFeature(GoColor color) {
		StringBuilder feature = new StringBuilder();
		
		ConstBoard board = game.getBoard();
		int size = board.getSize();
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				GoPoint point = GoPoint.get(x, y);
		        GoColor c = board.getColor(point);
		        if (c == color) {
		        	feature.append("1");
		        }
		        else {
		        	feature.append("0");
		        }
			}
		}
		
		return feature.toString();
	}
	
	private String getBlackFeature() {
		return getStoneFeature(BLACK);
	}
	
	private String getWhiteFeature() {
		return getStoneFeature(WHITE);
	}
	
	private String getEmptyFeature() {
		return getStoneFeature(EMPTY);
	}
	
	private String getNextColorFeature(boolean isBlack) {
		if (isBlack) {
			return ALLONE;
		}
		else {
			return ALLZERO;
		}
	}
	
	private String getKoFeature() {
		ConstBoard board = game.getBoard();
		int size = board.getSize();
		inKo = false;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				GoPoint point = GoPoint.get(x, y);
				if (board.isKo(point)) {
					inKo = true;
					return ALLONE;
				}
			}
		}
		
		return ALLZERO;
	}
	
	private String getHistoryFeature() {
		StringBuilder feature = new StringBuilder();
		ConstBoard board = game.getBoard();
		int size = board.getSize();
		int n = board.getNumberMoves();
		GoPoint prevPoint1 = n - 1 >= 0 ? board.getMove(n - 1).getPoint() : null;
		GoPoint prevPoint2 = n - 2 >= 0 ? board.getMove(n - 2).getPoint() : null;
		GoPoint prevPoint3 = n - 3 >= 0 ? board.getMove(n - 3).getPoint() : null;
		GoPoint prevPoint4 = n - 4 >= 0 ? board.getMove(n - 4).getPoint() : null;
		GoPoint prevPoint5 = n - 5 >= 0 ? board.getMove(n - 5).getPoint() : null;
		GoPoint prevPoint6 = n - 6 >= 0 ? board.getMove(n - 6).getPoint() : null;
		GoPoint prevPoint7 = n - 7 >= 0 ? board.getMove(n - 7).getPoint() : null;
		GoPoint prevPoint8 = n - 8 >= 0 ? board.getMove(n - 8).getPoint() : null;

		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if ( (prevPoint1 != null && prevPoint1.getX() == x && prevPoint1.getY() == y)) {
					feature.append("1");
				}
				else if ( (prevPoint2 != null && prevPoint2.getX() == x && prevPoint2.getY() == y)) {
					feature.append("2"); //Math.exp(-1*0.1) 0.90
				}
				else if ( (prevPoint3 != null && prevPoint3.getX() == x && prevPoint3.getY() == y)) {
					feature.append("3");//0.82
				}
				else if ( (prevPoint4 != null && prevPoint4.getX() == x && prevPoint4.getY() == y)) {
					feature.append("4");//0.74
				}
				else if ( (prevPoint5 != null && prevPoint5.getX() == x && prevPoint5.getY() == y)) {
					feature.append("5"); //0.67
				}
				else if ( (prevPoint6 != null && prevPoint6.getX() == x && prevPoint6.getY() == y)) {
					feature.append("6"); //0.61
				}
				else if ( (prevPoint7 != null && prevPoint7.getX() == x && prevPoint7.getY() == y)) {
					feature.append("7"); // 0.55
				}
				else if ( (prevPoint8 != null && prevPoint8.getX() == x && prevPoint8.getY() == y)) {
					feature.append("8"); // 0.50
				}
				else {
					feature.append("0");
				}
			}
		}
		
		return feature.toString();
	}
	
	private List<String> getGroupLibertiesFeature() {
		StringBuilder feature1 = new StringBuilder();
		StringBuilder feature2 = new StringBuilder();
		StringBuilder feature3 = new StringBuilder();
		StringBuilder feature4 = new StringBuilder();
		
		ConstBoard board = game.getBoard();
		int size = board.getSize();
		int liberties = 0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				GoPoint point = GoPoint.get(x, y);
				if (board.getColor(point) == EMPTY) {
					feature1.append("0");
					feature2.append("0");
					feature3.append("0");
					feature4.append("0");
					continue;
				}
				
				liberties = FeatureUtil.countGroupLiberties(board, point);
				
				feature1.append(liberties == 1 ? "1" : "0");
				feature2.append(liberties == 2 ? "1" : "0");
				feature3.append(liberties == 3 ? "1" : "0");
				feature4.append(liberties >= 4 ? "1" : "0");
			}
		}
		
		List<String> features = new ArrayList<String>();
		features.add(feature1.toString());
		features.add(feature2.toString());
		features.add(feature3.toString());
		features.add(feature4.toString());
		return features;
	}
	
	/**
	 * Get next move label
	 */
	public String getMoveLabel() {
		try {
			return Integer.toString(game.getCurrentNode().getChildConst().getMove().getPoint().getIndex());
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public double getValueLabel65(boolean isBlack) {
		return isBlack ? rsValue65 : -rsValue65;
	}
	
	public double getValueLabel75(boolean isBlack) {
		return isBlack ? rsValue75 : -rsValue75;
	}

	private boolean parseResult(ConstNode node) {
		boolean isValid = true;
		ConstGameInfo gameInfo = game.getGameInfo(node);
		double komi = gameInfo.getKomi().toDouble();
		if (komi != 6.5 && komi != 7.5) return false;
		
		String rs = gameInfo.get(StringInfo.RESULT).toUpperCase();
		if (rs.indexOf("TIME") >= 0 || rs.indexOf("OFFLINE") >= 0) {
			isValid = false;
		}
		String scoreStr = rs.replaceAll("[^\\.\\d]", "");
		double score = 0;
		if ("".equals(scoreStr)) {
			score = 20; // give a big value for resign game
		}
		else {
			score = Double.parseDouble(scoreStr);
		}
		
		double blackScore = 0;
		if (rs.indexOf("B+") >= 0) { // black win
			blackScore = komi + score;
		}
		else {
			blackScore = -(score - komi);
		}
		
		if (blackScore > 6.5f) {
			rsValue65 =  1;
		}
		else {
			rsValue65 =  -1;
		}
		
		if (blackScore > 7.5f) {
			rsValue75 =  1;
		}
		else {
			rsValue75 =  -1;
		}
		
//		System.out.println(String.format("rsValue1: %f  rsValue2: %f  blackArea: %f komi: %f  rs[%s]", rsValue65, rsValue75, blackScore, komi, rs) );
		return isValid;
	}
	
	public List<String> process(Random random, float prob, int startMove, int endMove) {
		prob = 1 - prob;
		List<String> features = new ArrayList<String>();
		ConstNode root = game.getRoot();
		ConstNode node = root.getChildConst();
		game.gotoNode(node);
		
		boolean isValid = parseResult(node);
		while (isValid) {
			if (!node.hasChildren()) break;
			Move move = game.getCurrentNode().getChildConst().getMove();
			
			if (game.getMoveNumber() > endMove) break;
			if (move == null || move.getPoint() == null || game.getMoveNumber() < startMove) {
				node = node.getChildConst();
				game.gotoNode(node);
				continue;
			}
			
			boolean selected = false;
			if (random.nextFloat() >= prob) {
				selected = true;
			}
			
			if (!selected) {
				node = node.getChildConst();
				game.gotoNode(node);
				continue;
			}
			
			StringBuilder feature = new StringBuilder();
			boolean isBlack = node.getToMove() == BLACK;
			feature.append(getMoveLabel()).append(",");  // pos label
			feature.append(getValueLabel65(isBlack)).append(",");  // value label
//			feature.append(getValueLabel75(isBlack)).append(",");  // value label2
			if (isBlack) {
				feature.append(getBlackFeature());  // 1
				feature.append(getWhiteFeature());  // 2
			}
			else {
				feature.append(getWhiteFeature());  // 1
				feature.append(getBlackFeature());  // 2
			}
			
			feature.append(getEmptyFeature());  // 3
			getGroupLibertiesFeature().forEach(s -> feature.append(s)); // 4,5,6,7
			feature.append(getKoFeature());  // 8
			feature.append(getHistoryFeature()); // 9
			feature.append(getNextColorFeature(isBlack));  // 10
			
			features.add(feature.toString());

			node = node.getChildConst();
			game.gotoNode(node);
		}
		
		return features;
	}
}

