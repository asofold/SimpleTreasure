package me.asofold.bpl.simpletreasure;

import java.io.File;

import me.asofold.bpl.simpletreasure.configuration.Settings;
import me.asofold.bpl.simpletreasure.configuration.compatlayer.CompatConfig;
import me.asofold.bpl.simpletreasure.configuration.compatlayer.ConfigUtil;
import me.asofold.bpl.simpletreasure.configuration.compatlayer.NewConfig;
import me.asofold.bpl.simpletreasure.tasks.TreasureHidingTask;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Command based treasure hiding plugin!
 * @author mc_dev
 *
 */
public class SimpleTreasure extends JavaPlugin{
	
	/**
	 * Example names to be taken from the jar, if not existent.
	 */
	public static String[] exampleFileNames = new String[]{
		"default.yml",
		"chainmail.yml",
		"epic.yml",
	};
	
	Settings settings = new Settings("<none>");

	@Override
	public void onEnable() {
		Server server = getServer();
		try{
			onReload(server.getConsoleSender());
		}
		catch (Throwable t){
			server.getLogger().severe("[SimpleTreasure] Failed to load configuration: ");
			t.printStackTrace();
		}
		super.onEnable();
	}

	/**
	 * Reload default configuration, add example settings if not present.
	 * @param consoleSender
	 */
	public void onReload(CommandSender sender) {
		writeExampleFiles();
		onReload(sender, "config.yml");
	}

	/**
	 * 
	 * @param file
	 * @param name
	 */
	public void writeExampleFiles() {
		File dataFolder = getDataFolder();
		File configFile = new File(dataFolder, "config.yml");
		if (!configFile.exists()){
			String content = ConfigUtil.fetchResource(SimpleTreasure.class, "resources/default.yml");
			if (content != null){
				if (ConfigUtil.writeFile(configFile, content))	System.out.println("[SimpleTreasure] Added example configuration: config.yml");
			}
		}
		File examplesFolder = new File(dataFolder, "examples");
		if (!examplesFolder.exists()) examplesFolder.mkdirs();
		for (String name : exampleFileNames){
			File file = new File(examplesFolder, name);
			if (!file.exists()){
				String content = ConfigUtil.fetchResource(SimpleTreasure.class, "resources/"+name);
				if (content != null){
					if (ConfigUtil.writeFile(file, content)) System.out.println("[SimpleTreasure] Added example configuration: " + name);
				}
			}
		}
	}



	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (args.length == 0) return false;
		String cmd = args[0].trim().toLowerCase();
		int len = args.length;
		if (cmd.equals("reload") && len == 1){
			if (checkPerm(sender, "simpletreasure.reload")) onReload(sender);
			return true;
		}
		else if (cmd.equals("reload") && len == 2){
			if (checkPerm(sender, "simpletreasure.reload")) onReload(sender, args[1].trim());
			return true;
		}
		else if (cmd.equals("info")){
			if (checkPerm(sender, "simpletreasure.info")) onInfo(sender);
			return true;
		}
		else if (cmd.equals("abort")){
			if (checkPerm(sender, "simpletreasure.abort")) onAbort(sender);
			return true;
		}
		else if (cmd.equals("hide") && (len == 3 || len == 4)){
			if (!checkPerm(sender, "simpletreasure.hide")) return true;
			if (!checkPlayer(sender)) return true; 
			int checkIndex;
			Settings settings;
			if (len == 3){
				checkIndex = 1;
				settings = this.settings;
			}
			else{
				checkIndex = 2;
				settings = getSettings(sender, args[1].trim());
				if (settings == null) return true;
			}
			int tries = -1;
			try{
				tries = Integer.parseInt(args[checkIndex]);
			}
			catch (NumberFormatException e){
			}
			if (tries <= 0){
				sender.sendMessage("[SimpleTreasure] Bad number (tries): " + tries);
				return false;
			}
			int radius = -1;
			try{
				radius = Integer.parseInt(args[checkIndex + 1]);
			}
			catch (NumberFormatException e){
			}
			if (radius < 1){
				sender.sendMessage("[SimpleTreasure] Bad number (radius): " + tries);
				return false;
			}
			onHide( (Player) sender, tries, radius, settings);
			return true;
		}
		else if (cmd.equals("hide") && (len == 6 || len == 7)){
			// Hide for any CommandSender (including null):
			if (sender != null && !checkPerm(sender, "simpletreasure.hide")) return true;
			int checkIndex;
			Settings settings;
			if (len == 6){
				checkIndex = 1;
				settings = this.settings;
			}
			else{
				checkIndex = 2;
				settings = getSettings(sender, args[1].trim());
				if (settings == null) return true;
			}
			int tries = -1;
			try{
				tries = Integer.parseInt(args[checkIndex]);
			}
			catch (NumberFormatException e){
			}
			if (tries <= 0){
				sender.sendMessage("[SimpleTreasure] Bad number (tries): " + tries);
				return false;
			}
			int radius = -1;
			try{
				radius = Integer.parseInt(args[checkIndex + 1]);
			}
			catch (NumberFormatException e){
			}
			if (radius < 1){
				sender.sendMessage("[SimpleTreasure] Bad number (radius): " + tries);
				return false;
			}
			World world = getServer().getWorld(args[checkIndex + 2]);
			if (world == null){
				sender.sendMessage("[SimpleTreasure] Bad world: " + args[checkIndex + 2]);
				return false;
			}
			Integer x = null;
			try{
				x = Integer.parseInt(args[checkIndex + 3]);
			}
			catch (NumberFormatException exc){
				sender.sendMessage("[SimpleTreasure] Bad x-coordinate: " + args[checkIndex + 3]);
				return false;
			}
			Integer z = null;
			try{
				z = Integer.parseInt(args[checkIndex + 4]);
			}
			catch (NumberFormatException exc){
				sender.sendMessage("[SimpleTreasure] Bad z-coordinate: " + args[checkIndex + 4]);
				return false;
			}
			onHide(world, x, z, tries, radius, settings, sender);
			return true;
		}
		return false;
	}

	private void onAbort(CommandSender sender) {
		getServer().getScheduler().cancelTasks(this);
		sender.sendMessage("[SimpleTreasure] Aborted all tasks, if existent.");
	}

	public void onReload(CommandSender sender, String fileName) {
		Settings settings = getSettings(sender, fileName);
		if (settings != null) this.settings = settings;
	}
	
	/**
	 * Get settings form a configuration file from the SimpleTreasure plugin folder.
	 * @param sender
	 * @param fileName
	 * @return
	 */
	public Settings getSettings(CommandSender sender, String fileName) {
		File file = new File(getDataFolder(), fileName);
		Settings settings = new Settings(fileName);
		if (!file.exists()){
			sender.sendMessage("[SimpleTreasure] File does not exist: "+fileName);
			return null;
		} 
		else{
			CompatConfig cfg = new NewConfig(file);
			cfg.load();
			settings.fromConfig(cfg, "");
			// some defaults:
			if (settings.defaultBlockSettings.allowedNeighbours == null){
				sender.sendMessage("[SimpleTreasure] Neighbours must be set.");
				return null;
			}
			if (settings.defaultBlockSettings.allowedReplace == null){
				sender.sendMessage("[SimpleTreasure] Replace must be set.");
				return null;
			}
			sender.sendMessage("[SimpleTreasure] Settings loaded from: "+fileName);
			return settings;
		}
	}

	public void onInfo(CommandSender sender) {
		sender.sendMessage("[SimpleTreasure] File = "+settings.fileName+" | Treasures = "+settings.itemSettings.size());
	}
	
	private void onHide(Player player, int tries, int radius, Settings settings) {
		Location loc = player.getLocation();
		onHide(loc.getWorld(), loc.getBlockX(), loc.getBlockZ(), tries, radius, settings, player);
	}
	
	/**
	 * API method for hiding treasures, this will start a task that will distribute server load over time (!).
	 * @param world World to hide treasures in.
	 * @param x X-coordinate of the center.
	 * @param z Z-coordinate of the center.
	 * @param tries Number of attempts to hide a chest (depending on settings several heights will be considered at a random x-z position.
	 * @param radius Radius around center where to hide chests.
	 * @param settings Settings to apply.
	 * @param notify CommandSender to send status messages to.
	 */
	public void onHide(World world, int x, int z, int tries, int radius, Settings settings, CommandSender notify){
		if (settings.itemSettings.isEmpty()){
			if (notify != null) notify.sendMessage("[SimpleTreasure] No treasure defined!");
			return;
		}
		// Start a new hiding task:
		TreasureHidingTask task = new TreasureHidingTask(world, x, z, tries, radius, settings, notify);
		if (!task.register(this) && notify != null) notify.sendMessage("[SimpleTreasure] Failed to start the task for hiding the treasures.");
		else if (notify != null) notify.sendMessage("[SimpleTreasure] Started the task for hiding the treasures ("+settings.fileName+").");
	}
	
	public static boolean checkPlayer(CommandSender sender){
		if (sender instanceof Player) return true;
		else{
			sender.sendMessage("[SimpleTreasure] Only players can perform this action.");
			return false;
		}
	}
	
	public static boolean checkPerm(CommandSender sender, String perm){
		if (hasPermission(sender, perm)) return true;
		else{
			sender.sendMessage("[SimpleTreasure] You don't have permission.");
			return false;
		}
	}
	
	public static boolean hasPermission(CommandSender sender, String perm){
		return (sender.isOp() || sender.hasPermission(perm));
	}
	
}
