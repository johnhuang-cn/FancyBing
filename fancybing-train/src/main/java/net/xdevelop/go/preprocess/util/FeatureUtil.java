package net.xdevelop.go.preprocess.util;

import static net.sf.gogui.go.GoColor.EMPTY;

import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;

public class FeatureUtil {
	public static int countGroupLiberties(ConstBoard board, GoPoint point) {
		int liberties = 0;
		PointList stones = new PointList();
		PointList adjacents = new PointList();
		
		adjacents.clear();
		stones.clear();
		board.getStones(point, board.getColor(point), stones);
		for (GoPoint p : stones) {
			board.getAdjacent(p).forEach(po -> {if (!adjacents.contains(po)) adjacents.add(po);});
		}
		
		liberties = 0;
		for (GoPoint p : adjacents) {
			if (board.getColor(p) == EMPTY) {
				liberties++;
			}
		}
		
		return liberties;
	}
	
	public static int countIntersectionLiberties(ConstBoard board, GoPoint point) {
		ConstPointList adjacents = board.getAdjacent(point);
		int liberties = 0;
		for (GoPoint p : adjacents) {
			liberties += board.getColor(p) == EMPTY ? 1 : 0;
		}
		
		return liberties;
	}
}
