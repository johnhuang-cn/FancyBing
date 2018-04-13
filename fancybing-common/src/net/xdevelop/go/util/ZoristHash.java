package net.xdevelop.go.util;

import java.util.Random;

public class ZoristHash {
	private final static long SEED = 2018411L;
	private static int[][] table = new int[361][2];
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
			for (int j = 0; j < 2; j++) {
				table[i][j] = ran.nextInt(); 
			}
		}
	}
	
	/**
	 * Get hash value by board points
	 * @param points 361 points, 1 black, 0 empty, -1 white
	 * @return
	 */
	public static int hash(int[] points) {
		int h = 0;
		for (int i = 0; i < points.length; i++) {
			if (points[i] == 1) { // black stone
				h ^= table[i][0];
			}
			else if (points[i] == -1) { // white stone
				h ^= table[i][1];
			}
		}
		return h;
	}
	
	public static int emptyHash() {
		return 0;
	}
	
	/**
	 * Increment version
	 * @param hash original hash
	 * @param index current move index
	 * @param color 
	 * @return
	 */
	public static int hash(int hash, int index, int color) {
		assert color == -1 || color == 1;
		
		if (color == 1) { // black stone
			hash ^= table[index][0];
		}
		else if (color == -1) { // white stone
			hash ^= table[index][1];
		}
		return hash;
	}
}
