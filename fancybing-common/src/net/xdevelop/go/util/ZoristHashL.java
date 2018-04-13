package net.xdevelop.go.util;

import java.util.Random;

public class ZoristHashL {
	private final static long SEED = 1234567L;
	private static long[][] table = new long[361][3];
	static {
		init();
	}
	  /* constant indices
       white_pawn := 1
       white_rook := 2
       # etc.
       black_king := 12
   
   function init_zobrist():
       # fill a table of random numbers/bitstrings
       table := a 2-d array of size 64×12
       for i from 1 to 64:  # loop over the board, represented as a linear array
           for j from 1 to 12:      # loop over the pieces
               table[i][j] = random_bitstring()
   
   function hash(board):
       h := 0
       for i from 1 to 64:      # loop over the board positions
           if board[i] != empty:
               j := the piece at board[i], as listed in the constant indices, above
               h := h XOR table[i][j]
       return h*/
	
	private static void init() {
		Random ran = new Random(SEED);
		for (int i = 0; i < 361; i ++) {
			for (int j = 0; j < 3; j++) {
				table[i][j] = ran.nextLong(); 
			}
		}
	}
	
	/**
	 * Get hash value by board points
	 * @param points 361 points, 1 black, 0 empty, -1 white
	 * @return
	 */
	public static long hash(int[] points) {
		long h = 0;
		for (int i = 0; i < points.length; i++) {
			if (points[i] == 0) { // empty point
				h ^= table[i][0];
			}
			else if (points[i] == 1) { // black stone
				h ^= table[i][1];
			}
			else if (points[i] == -1) { // white stone
				h ^= table[i][2];
			}
		}
		return h;
	}
	
	public static long emptyHash() {
		long h = 0;
		for (int i = 0; i < table.length; i++) {
			h ^= table[i][0];
		}
		return h;
	}
	
	/**
	 * Increment version
	 * @param hash original hash
	 * @param index current move index
	 * @param color 
	 * @return
	 */
	public static long hash(long hash, int index, int color) {
		assert color == -1 || color == 1;
		hash ^= table[index][0];
		
		if (color == 1) { // black stone
			hash ^= table[index][1];
		}
		else if (color == -1) { // white stone
			hash ^= table[index][2];
		}
		return hash;
	}
}
