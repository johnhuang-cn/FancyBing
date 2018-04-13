package net.xdevelop.go.policynet;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.nd4j.linalg.api.ndarray.INDArray;

public interface IPolicyNet extends Remote {
	public INDArray[] evaluateOpen(INDArray features) throws RemoteException;
	public INDArray[] evaluateMid(INDArray features) throws RemoteException;
	public INDArray[] evaluateEnd(INDArray features) throws RemoteException;
}
