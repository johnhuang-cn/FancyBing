package net.xdevelop.go.mcts;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.xdevelop.go.model.ValueCache;

public class PolicyCache {
	public static ConcurrentHashMap<Integer, ValueCache> CACHE = new ConcurrentHashMap<Integer, ValueCache>();
	public static int CUR_MOVE_NUMBER = 0;
	
	public static void put(int hash, int[][] moves, float score, int moveNumber) {
		ValueCache estCache = new ValueCache();
		estCache.score = score;
		estCache.moves = moves;
		estCache.moveNumber = moveNumber;
		CACHE.put(hash, estCache);
	}
	
	public static ValueCache get(int hash) {
		return CACHE.get(hash);
	}
	
	public static void clearAll() {
		CACHE.clear();
	}
	
	public static void setCurrentMoveNumber(int mv) {
		CUR_MOVE_NUMBER = mv;
	}
	
	public static void clearCache() {
		int count = 0;
		Iterator<Entry<Integer, ValueCache>> it = CACHE.entrySet().iterator();
		while (it.hasNext()) {
			ValueCache cache = it.next().getValue();
			if (cache.moveNumber < CUR_MOVE_NUMBER) {
				count++;
				it.remove();
			}
		}
		
		System.err.println(String.format("Removed %d, CacheSize: %d ", count, CACHE.size())); // use err.print because it can print red message in eclipse
	}
}
