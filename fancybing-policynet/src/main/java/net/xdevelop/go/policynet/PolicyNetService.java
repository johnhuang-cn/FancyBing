package net.xdevelop.go.policynet;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.jita.conf.CudaEnvironment;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import net.xdevelop.go.Global;

public class PolicyNetService extends UnicastRemoteObject implements IPolicyNet {
	private static final long serialVersionUID = 8106231551583845967L;
	
	private final static String OPEN_NETWORK_MODEL_NAME = "ResNetwork_open.zip"; // for opening
	private final static String MID_NETWORK_MODEL_NAME = "ResNetwork_mid.zip"; // for mid
	private final static String END_NETWORK_MODEL_NAME = "ResNetwork_end.zip"; // for end
	
	private ComputationGraph midModel;
	private ComputationGraph openModel;
	private ComputationGraph endModel;
	
	public PolicyNetService() throws RemoteException {
		try {
			openModel = loadComputationGraph(OPEN_NETWORK_MODEL_NAME);
			midModel = loadComputationGraph(MID_NETWORK_MODEL_NAME);
			endModel = midModel; //loadComputationGraph(END_NETWORK_MODEL_NAME);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static ComputationGraph loadComputationGraph(String fn) throws Exception {
    	File f = new File(System.getProperty("user.dir") + "/model/" + fn);
    	System.out.println("Loading model " + f);
    	ComputationGraph model = ModelSerializer.restoreComputationGraph(f);
	
		return model;
    }
	
	@Override
	public INDArray[] evaluateMid(INDArray features) throws RemoteException {
		try {
			return midModel.output(features);
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}
	
	@Override
	public INDArray[] evaluateOpen(INDArray features) throws RemoteException {
		try {
			return openModel.output(features);
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}
	
	@Override
	public INDArray[] evaluateEnd(INDArray features) throws RemoteException {
		try {
			return endModel.output(features);
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
	}
	
	public static void main(String[] args) {
		Nd4j.getMemoryManager().setAutoGcWindow(2000);
		CudaEnvironment.getInstance().getConfiguration()
			.setMaximumDeviceCacheableLength(1024 * 1024 * 1024L)
			.setMaximumDeviceCache(2L * 1024 * 1024 * 1024L)
			.setMaximumHostCacheableLength(1024 * 1024 * 1024L)
			.setMaximumHostCache(8L * 1024 * 1024 * 1024L);
		
		// Register services, bind services in multi ports for better performance
		Registry registry = null;
		for (int i = 0; i < Global.NETWORK_THREADS_NUM; i++) {
			try {
				registry = LocateRegistry.createRegistry(Global.POLICYNET_RMI_PORT + i);
				PolicyNetService policyNet = new PolicyNetService();
				registry.rebind(Global.NAME + "Policy", policyNet);
				
				System.out.println("Bind FancyBingPolicy server on " + (Global.POLICYNET_RMI_PORT + i));
				System.out.println("FancyBingPolicy server started.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
