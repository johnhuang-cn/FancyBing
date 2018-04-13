package net.xdevelop.go.mcts;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import net.xdevelop.go.BoardConstants;
import net.xdevelop.go.Config;
import net.xdevelop.go.game.IGame;
import net.xdevelop.go.model.Board;
import net.xdevelop.go.util.SgfUtil;

public class Game extends UnicastRemoteObject implements IGame {
	private static final long serialVersionUID = 3074787842076718364L;
	
	private static float KOMI = 6.5f;
	
	private MCTS mcts;
	private Board board;
	private ArrayList<Integer> historyMove = new ArrayList<Integer>();
	private int moveNumber = 0;
	
	public Game(boolean pondering) throws RemoteException {
		super();
		BoardConstants.init();
		init();
		mcts = MCTS.getInstance(pondering);
	}
	
	private void init() {
		board = new Board();
		historyMove.clear();
		historyMove.add(-1); // discard the first position, so that the number are same as normal manual 第一手不用，以便让序号和棋谱一致
		moveNumber = 0;
	}

	@Override
	public void newGame() throws RemoteException {
		MCTS.stopPondering();
		mcts.reset();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		
		if (historyMove.size() > 1) {
			if (board != null) {
				board.init();
			}
			init();
		}
	}

	@Override
	public void setKomi(double komi) throws RemoteException {
		KOMI = (float) komi;
	}
	
	public static float getKomi() {
		return KOMI;
	}

	@Override
	public void play(int color, int index) throws RemoteException {
		try {
			moveNumber++;
			historyMove.add(index);
			board.play(color, index);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RemoteException(e.getMessage());
		}
	}

	@Override
	public String genmove(int toMoveColor) throws RemoteException {
		try {
			int index = 300 + (int) (System.currentTimeMillis() % 2); // first move random pick 300 or 301 (O16/R16) 第一手小目 星随机
			if (moveNumber > 0) {
				index = mcts.genmove(toMoveColor, board, historyMove.get(moveNumber), historyMove.get(moveNumber - 1), moveNumber);
			}
			play(toMoveColor, index);
			if (historyMove.get(moveNumber) == Config.PASS && index == Config.PASS) {
				System.out.println("Game end");
			}
			return SgfUtil.index2String(index);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RemoteException(e.getMessage());
		}
	}

	@Override
	public void undo() throws RemoteException {
		ArrayList<Integer> historyBak = new ArrayList<Integer>();
		historyBak.addAll(historyMove);
		historyBak.remove(moveNumber);
		newGame();
		for (int i = 1; i < historyBak.size(); i++) {
			if (i % 2 == 1) {
				play(Board.BLACK, historyBak.get(i));
			}
			else {
				play(Board.WHITE, historyBak.get(i));
			}
		}
	}

	@Override
	public void timeSettings(int mainTime, int boyoYomiTime, int boyoYomiStones) throws RemoteException {
		mcts.setThinkingTime(boyoYomiTime);
	}
}
