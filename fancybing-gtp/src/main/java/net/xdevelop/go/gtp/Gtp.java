package net.xdevelop.go.gtp;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Locale;

import net.xdevelop.go.game.IGame;

/**
 * https://www.lysator.liu.se/~gunnar/gtp/gtp2-spec-draft2/gtp2-spec.html
 */
public class Gtp {
	private final static int GTP_RMI_PORT = 8888;
	private final static int BOARD_SIZE = 19;
	
	private final static String OK = "=%d ";
	private final static String ERR = "?%d ";
	
	private static int protocol_version = 2;
	private static String version = "1.0";
	private static String name = "FancyBing";
	
	// list of known commands
	private String[] commands = { "boardsize", "clear_board", "genmove", "known_command", "komi", "list_commands",
			"name", "play", "protocol_version", "version", "undo", "time_settings", "showboard" };

	private InputStream in;
	private OutputStream out;
	private IGame game;
	
	public Gtp(InputStream in, OutputStream out) throws Exception {
		this(in, out, GTP_RMI_PORT);
	}
	
	public Gtp(InputStream in, OutputStream out, int port) throws Exception {
		this.in = in;
		this.out = out;
		
		// regist RMI service
        Registry registry = null; 
        registry = LocateRegistry.getRegistry("127.0.0.1", port);
    	game = (IGame) registry.lookup("FancyBing"); 
	}

	// get a command
	public void loop() {
		BufferedReader reader;
		String line;

		try {
			reader = new BufferedReader(new InputStreamReader(in));
			while (!(line = reader.readLine()).equals("quit")) {
				// remove "cr"
				line = line.replace((char) 13, (char) 32);
				
				// replace "horizontal tab" to "space"
				line = line.replace((char) 9, (char) 32);
				
				// remove whitespaces
				line = line.trim();

				if (line.startsWith("#") || (line.equals(""))) {
					// discard
				} 
				else {
					// process command
					processCommand(line);
				}
			}
			reader.close();
		} catch (IOException e) {
			e.toString();
		}

	}

	// process a command
	public void processCommand(String cmdLine) {
		String response = "";

		String[] token = cmdLine.split(" ");
		int id;
		String cmd;
		String[] args;
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		try {
			id = Integer.parseInt(token[0]);
			cmd = token[1];
			args = new String[token.length - 2];
			for (int i = 0; i < args.length; i++) {
				args[i] = token[i + 2];
			}
		} catch (NumberFormatException e) {
			id = -1;
			cmd = token[0];
			args = new String[token.length - 1];
			for (int i = 0; i < args.length; i++) {
				args[i] = token[i + 1];
			}
		}

		try {
			if (cmd.equals("protocol_version")) {
				response = cmd_protocol_version();
			} else if (cmd.equals("name")) {
				response = cmd_name();
			} else if (cmd.equals("version")) {
				response = cmd_version();
			} else if (cmd.equals("known_command")) {
				response = cmd_known_command(token[1]);
			} else if (cmd.equals("list_commands")) {
				response = cmd_list_commands();
			} else if (cmd.equals("boardsize")) {
				response = cmd_boardsize(Integer.parseInt(token[1]));
			} else if (cmd.equals("clear_board")) {
				response = cmd_clear_board();
			} else if (cmd.equals("komi")) {
				response = cmd_komi(token[1]);
			} else if (cmd.equals("play")) {
				response = cmd_play(args[0], args[1]);
			} else if (cmd.equals("genmove")) {
				response = cmd_genmove(args[0]);
			} else if (cmd.equals("undo")) {
				response = cmd_undo();
			} else if (cmd.equals("final_status_list")) { // kgs
				response = cmd_final_status_list();
			} else if (cmd.equals("time_settings")) {
				response = cmd_time_settings(args[0], args[1], args[2]);
			} else {
				response = cmd_unknown();
			}
		} catch (Exception e1) {
			response = ERR + e1.toString();
		}
		
		// display response
		try {
			if (id >= 0) {
				writer.write(String.format(response + "\n\n", id));
			}
			else {
				writer.write(response.replaceFirst("%d", "") + "\n\n");
			}
			writer.flush();
		} catch (IOException e) {
			System.out.println("? " + e.toString());
		}
	}
	
	private String cmd_protocol_version() {
		return OK + Integer.toString(protocol_version);
	}

	private String cmd_name() {
		return OK + name;
	}
	
	private String cmd_version() {
		return OK + version;
	}
	
	private String cmd_known_command(String cmd) {
		for (int i = 0; i < commands.length; i++) {
			if (commands[i].equals(cmd)) {
				return OK + Boolean.toString(true);
			}
		}
		return OK + Boolean.toString(false);
	}

	// list supported commands
	public String cmd_list_commands() {
		String list = "";

		for (int i = 0; i < commands.length; i++) {
			if (i == 0) {
				list += commands[i];
			} else {
				list += "\n";
				list += commands[i];
			}
		}
		return OK + list;
	}

	// set board size
	public String cmd_boardsize(int size) throws Exception {
		if (size != 19) {
			throw new Exception("Doesn't support size " + size);
		}
		return OK;
	}

	// set komi
	public String cmd_komi(String new_komi) throws RemoteException {
		game.setKomi(Double.parseDouble(new_komi));
		return OK;
	}

	// clear a board and initialize a game
	public String cmd_clear_board() throws RemoteException {
		game.newGame();
		return OK;
	}

	// opponent's move
	// move: "white h10", "B F5", "w pass"
	public String cmd_play(String color, String vertex) throws RemoteException {
		try {
			game.play(parseColor(shortColor(color)), parsePoint(vertex));
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
		return OK;
	}

	// generate a move
	// this is where you call your go engine
	// color: "w", "white", "b", "black"
	public String cmd_genmove(String color) throws RemoteException {
		return OK + game.genmove(parseColor(shortColor(color)));
	}

	public String cmd_final_status_list() {
		return OK;
	}
	
	public String cmd_unknown() {
		return ERR + "unknown command";
	}
	
	public String cmd_undo() throws RemoteException {
		game.undo();
		return OK;
	}
	
	public String cmd_time_settings(String mainTime, String boyoYomiTime, String boyoYomiStones) throws RemoteException {
		game.timeSettings(Integer.parseInt(mainTime), Integer.parseInt(boyoYomiTime), Integer.parseInt(boyoYomiStones));
		return OK;
	}
	
	private String shortColor(String color) {
		if ("w white".indexOf(color.toLowerCase()) >= 0) {
			color = "w";
		}
		else if ("b black".indexOf(color.toLowerCase()) >= 0) {
			color = "b";
		}
		return color;
	}
	
	public final static int parsePoint(String string) throws Exception {
		string = string.trim().toUpperCase(Locale.ENGLISH);
		if (string.equals("PASS"))
			return -1;
		if (string.length() < 2)
			throw new Exception("Invalid point " + string);
		char xChar = string.charAt(0);
		if (xChar >= 'J')
			--xChar;
		int x = xChar - 'A';
		int y;
		try {
			y = Integer.parseInt(string.substring(1)) - 1;
		} catch (NumberFormatException e) {
			throw new Exception("Invalid point " + string);
		}
		if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE)
			throw new Exception("Invalid point " + string);

		return y * BOARD_SIZE + x;
	}
	
	public final static int parseColor(String s) {
		if ("B".equalsIgnoreCase(s)) {
			return 1;
		}
		else if ("W".equalsIgnoreCase(s)) {
			return -1;
		}
		return 1;
	}
}
