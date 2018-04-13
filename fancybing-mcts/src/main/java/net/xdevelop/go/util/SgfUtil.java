package net.xdevelop.go.util;

import net.xdevelop.go.Config;

public final class SgfUtil {
	/**
	 * 转换为棋盘坐标
	 * Index to board coordinate 
	 * @param index
	 * @return
	 */
	public final static String index2String(int index) {
		if (index == -1) return "PASS";
		
		int y = index / Config.BOARD_SIZE;
		int x = index - y * Config.BOARD_SIZE;
		char xChar = (char)('A' + x);
        if (xChar >= 'I')
            ++xChar;
        return xChar + Integer.toString(y + 1);
	}
	
	/**
	 * 转换为SGF棋谱的坐标
	 * Index to SGF coordinate
	 * @param index
	 * @return
	 */
	public final static String index2Sgf(int index) {
		int y = index / Config.BOARD_SIZE;
		int x = index - y * Config.BOARD_SIZE;
		y = Config.BOARD_SIZE - y - 1;
		x = 'a' + x;
        y = 'a' + y;
        return "" + (char)x + (char)y;
	}
}
