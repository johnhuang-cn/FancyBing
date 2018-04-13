package net.xdevelop.go;

public class Config {
	public final static String VERSION = "1.00";
	public final static String NAME = "FancyBing";
	
	// RMI related
	public final static int POLICYNET_RMI_PORT = 9000; 
	public final static int GTP_RMI_PORT = 8888; // GTP service port
	public final static int EVALUATE_BATCH_SIZE = 8; // GPU evaluate batch size 每次发给GPU预测的batch size
	
	// Board related
	public final static int BOARD_SIZE = 19;
	public final static int BOARD_INDEX_NUM = 361;
	public final static int MAX_MOVE = 720;
	public final static int PASS = -1;
	public final static int FEATURE_CHANNELS = 10;
	public final static int FEATURE_COLUMNS = BOARD_INDEX_NUM * FEATURE_CHANNELS;
	
	// Search related
	public final static float PUCT_C = 1f ;
	public static int POLICY_NETWORK_MAX_TOP_N = 12;
	public final static int POLICY_NETWORK_TOP_N = 2;
	public final static float POLICY_NETWORK_TOP_PROBABILITY = 1f;
	public final static int V_LOSS = 3;
	public static int AVG_EVALUATE_SPEED = 500;
	
	public final static int UCT_THREADS_NUM = 12;
	public final static int NETWORK_THREADS_NUM = 4;
	
	public static boolean IN_MID = false; // in mid game phase
	public static boolean IN_OPEN = true; // opening phase
}
