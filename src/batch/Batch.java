package batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public class Batch extends JavaPlugin implements Runnable {
	public HashMap<String, BatchScript> running;
	public File scriptsFolder;

	@Override
	public void onEnable() {
		running = new HashMap<String, BatchScript>();
		scriptsFolder = new File(getDataFolder(), "scripts");
		scriptsFolder.mkdirs();
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this, 1, 1);
		List<String> startScripts = getConfig().getStringList("startup");
		if (startScripts != null) {
			for (String script: startScripts) {
				startBatch(script, Bukkit.getConsoleSender());
			}
		} else {
			String startScript = getConfig().getString("startup");
			if (startScript != null) {
				startBatch(startScript, Bukkit.getConsoleSender());
			}
		}
	}

	public void batchListRequested(CommandSender sender) {
		Permission permission = getPermission("batch.list");
		permission.setDefault(PermissionDefault.OP);
		permission.addParent("batch.*", true);
		if (!sender.hasPermission(permission)) {
			sender.sendMessage(ChatColor.RED + "You do not have permission to list scripts.");
			return;
		}
		sender.sendMessage(ChatColor.YELLOW + "Running Scripts (" + running.size() + "):");
		String sc = "";
		for (String key : running.keySet()) {
			sc += (key + ", ");
		}
		if (sc.length() > 0) {
			sc = sc.substring(0, sc.length() - 2);
		}
		sender.sendMessage(ChatColor.YELLOW + sc);
		ArrayList<String> allScripts = new ArrayList<String>();
		for (String file : scriptsFolder.list()) {
			if (file.endsWith(".txt")) {
				allScripts.add(file.substring(0, file.length() - 4));
			}
		}
		sender.sendMessage("All Scripts (" + allScripts.size() + "): ");
		sc = "";
		for (String script : allScripts) {
			sc += script + ", ";
		}
		if (sc.length() > 0) {
			sc = sc.substring(0, sc.length() - 2);
		}
		sender.sendMessage(ChatColor.YELLOW + sc);
	}
	
	public Permission getPermission(String name) {
		Permission permission = Bukkit.getPluginManager().getPermission(name);
		if (permission == null) {
			permission = new Permission(name, PermissionDefault.OP);
			Bukkit.getPluginManager().addPermission(permission);
		}
		return permission;
	}
	
	public BatchScript startBatch(String scriptName, CommandSender sender) {
		scriptName = scriptName.toLowerCase();
		Permission permission = getPermission("batch.run." + scriptName);
		permission.setDefault(PermissionDefault.OP);
		permission.addParent("batch.run.*", true);
		permission.addParent("batch.*", true);
		if (!sender.hasPermission(permission)) {
			sender.sendMessage(ChatColor.RED + "You don't have permission to run script '" + scriptName + "'.");
			return null;
		}
		if (running.containsKey(scriptName)) {
			sender.sendMessage(ChatColor.RED + "Script '" + scriptName + "' is already running.");
			return null;
		}
		File batchFile = new File(scriptsFolder, scriptName + ".txt");
		FileInputStream input;
		try {
			input = new FileInputStream(batchFile);
		} catch (FileNotFoundException e) {
			sender.sendMessage(ChatColor.RED + "Batch file '" + scriptName + "' does not exist.");
			return null;
		}
		BatchScript script = new BatchScript(this, scriptName);
		try {
			script.load(input);
		} catch (IOException e) {
			e.printStackTrace();
			sender.sendMessage(ChatColor.RED + "Error loading batch script. See server.log for details.");
		}
		sender.sendMessage(ChatColor.GREEN + "Started '" + scriptName + "'.");
		running.put(scriptName, script);
		return script;
	}
	
	public void stopBatch(String scriptName, CommandSender sender) {
		scriptName = scriptName.toLowerCase();
		Permission permission = getPermission("batch.stop." + scriptName);
		permission.addParent("batch.stop.*", true);
		permission.addParent("batch.*", true);
		if (!sender.hasPermission(permission)) {
			sender.sendMessage("You don't have permission to stop script '" + scriptName + "'.");
			return;
		}
		running.remove(scriptName);
		sender.sendMessage(ChatColor.GREEN + "Stopped '" + scriptName + "'.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length < 1) {
			return false;
		}
		if (args[0].equalsIgnoreCase("run") && args.length == 2) {
			startBatch(args[1], sender);
			return true;
		}
		if (args[0].equalsIgnoreCase("stop") && args.length == 2) {
			stopBatch(args[1], sender);
			return true;
		}
		if (args[0].equalsIgnoreCase("debug") && args.length == 2) {
			BatchScript script = startBatch(args[1], sender);
			if (script != null) {
				script.debugger = sender;
			}
			return true;
		}
		if (args[0].equalsIgnoreCase("list") && args.length == 1) {
			batchListRequested(sender);
			return true;
		}
		return false;
	}

	public void stop(String name) {
		running.remove(name);
	}

	@Override
	public void run() {
		for (BatchScript script: running.values()) {
			script.tick();
		}
	}
}
