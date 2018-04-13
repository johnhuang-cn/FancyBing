package net.xdevelop.go.game;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IGame extends Remote {
	public void newGame() throws RemoteException;
	
	public void setKomi(double komi) throws RemoteException;
	
	public void play(int color, int index) throws RemoteException;
	
	public String genmove(int color) throws RemoteException;
	
	public void undo() throws RemoteException;
	
	public void timeSettings(int mainTime, int byoYomiTime, int byoYomiStones) throws RemoteException;
}
