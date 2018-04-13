package net.xdevelop.go.policynet;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.nd4j.linalg.api.ndarray.INDArray;

import net.xdevelop.go.Config;

/**
 * Because of DL4J GC performance issue, 
 * if put network evaluate in the same project,
 * it would be very slow. So implemented it as RMI arch. 
 */
public class RemoteEvaluator implements IPolicyNet {
	private static RemoteEvaluator[] instance;
	private IPolicyNet policyNet;
	
	static {
		instance = new RemoteEvaluator[Config.NETWORK_THREADS_NUM];
		try {
			for (int i = 0; i < Config.NETWORK_THREADS_NUM; i++) {
				instance[i] = new RemoteEvaluator(i);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    private RemoteEvaluator(int i) throws Exception {
        Registry registry = null;  
        registry = LocateRegistry.getRegistry("127.0.0.1", Config.POLICYNET_RMI_PORT + i);
    	policyNet = (IPolicyNet) registry.lookup(Config.NAME + "Policy"); 
    }
    
    public static RemoteEvaluator getRemotePolicyNet(int i) {
    	return instance[i];
    }
    
    public static RemoteEvaluator getRemotePolicyNet() {
    	return instance[0];
    }
    
    public INDArray[] evaluate(INDArray features) throws RemoteException {
    	if (Config.IN_OPEN) {
    		return policyNet.evaluateOpen(features);
    	}
    	else if (Config.IN_MID) {
    		return policyNet.evaluateMid(features);
    	}
    	else {
    		return policyNet.evaluateEnd(features);
    	}
    }
    
    public INDArray[] evaluateMid(INDArray features) throws RemoteException {
    	return policyNet.evaluateMid(features);
    }
    
    public INDArray[] evaluateOpen(INDArray features) throws RemoteException {
    	return policyNet.evaluateOpen(features);
    }
    
    public INDArray[] evaluateEnd(INDArray features) throws RemoteException {
    	return policyNet.evaluateEnd(features);
    }
}
