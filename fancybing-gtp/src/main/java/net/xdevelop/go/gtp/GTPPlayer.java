package net.xdevelop.go.gtp;

public class GTPPlayer {

	public static void main(String[] args) throws Exception {
		int port = 8888;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		Gtp gtp = new Gtp(System.in, System.out, port);
		gtp.loop();
	}
}
