package batch;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class BatchScript {
	public Batch plugin;
	public String name;
	public HashMap<String, String> vars = new HashMap<String, String>();
	public String[] lines;
	public int currentLine;
	public int nextLine;
	public int ticksUntilResume;
	public CommandSender debugger;
	
	public BatchScript(Batch plugin, String name) {
		this.plugin = plugin;
		this.name = name;
	}

	public void load(InputStream input) throws IOException {
		BufferedInputStream bInput = new BufferedInputStream(input);
		ArrayList<String> cLines = new ArrayList<String>();
		String currentLine = "";
		nextLine = 0;
		ticksUntilResume = 0;
		while (true) {
			int x = bInput.read();
			if (x == -1) {
				cLines.add(currentLine);
				lines = cLines.toArray(new String[cLines.size()]);
				return;
			}
			char c = (char) x;
			if (c == '\r') {

			} else if (c == '\n') {
				cLines.add(currentLine);
				currentLine = "";
			} else {
				currentLine += c;
			}
		}
	}
	
	public void tick() {
		if (--ticksUntilResume <= 0) {
			execute();
		}
	}

	public void execute() {
		currentLine = nextLine;
		nextLine = currentLine + 1;
		if (currentLine >= lines.length) {
			plugin.stop(name);
			debug("Batch '" + name + "' halted because the end of the batch was reached.");
			return;
		}
		String line = lines[currentLine];
		if (executeLine(line)) {
			execute();
		}
	}

	public boolean executeLine(String line) {
		for (String key: vars.keySet()) {
			line = line.replace("%" + key, vars.get(key));
		}
		String[] split = line.split(" ");
		if (split.length == 0) {
			return true;
		}
		String command = split[0];
		if (command.equalsIgnoreCase("RUN") && split.length >= 2) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line.substring(4));
			return true;
		} else if (command.equalsIgnoreCase("WAIT") && split.length == 2) {
			int lengthToWait;
			try {
				lengthToWait = Integer.parseInt(split[1]);
			} catch (NumberFormatException e) {
				error("Can not cast '" + split[1] + "' to a number to WAIT for.");
				return false;
			}
			ticksUntilResume = lengthToWait;
			return false;
		} else if (command.equalsIgnoreCase("LINE") && split.length == 2) {
			String lineName = split[1];
			vars.put(lineName, Integer.toString(nextLine));
			return true;
		} else if (command.equalsIgnoreCase("GOTO") && split.length == 2) {
			int lineToGoTo;
			try {
				lineToGoTo = Integer.parseInt(split[1]);
			} catch (NumberFormatException e) {
				error("Can not go to line '" + split[1] + "'.");
				return false;
			}
			nextLine = lineToGoTo;
			return true;
		} else if (command.equalsIgnoreCase("DEBUG") && split.length >= 2) {
			debug(line.substring(6));
			return true;
		} else if (command.equalsIgnoreCase("EXIT") && split.length == 1) {
			plugin.stop(name);
			return false;
		} else {
			error("Unable to execute line '" + line + "'.");
			return false;
		}
	}

	public void error(String message) {
		message = "Batch error in '" + name + "': " + message;
		plugin.getLogger().warning(message);
		if (debugger != null) {
			debugger.sendMessage(ChatColor.RED + message);
		}
		plugin.stop(name);
	}
	
	public void debug(String message) {
		if (debugger == null) {
			return;
		}
		debugger.sendMessage("Batch '" + name + "': " + message);
	}
}
