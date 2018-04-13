package net.xdevelop.go.mcts;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import net.xdevelop.go.Config;

public class FancyBingService {
	public static void main(String[] args) {
		if (args.length == 0) {
			start(Config.GTP_RMI_PORT, true);
		}
		else if (args.length == 1) {
			start(Integer.parseInt(args[0]), true);
		}
		else {
			start(Integer.parseInt(args[0]), Boolean.parseBoolean(args[1]));
		}
	}
	
	public static void start(int port, boolean pondering) {
		Registry registry = null;
		try {
			registry = LocateRegistry.createRegistry(port);
			Game game = new Game(pondering);
			registry.rebind(Config.NAME, game);

			System.out.println("Bind server on " + port);
			System.out.println(Config.NAME + " server started.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
