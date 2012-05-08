package me.asofold.simpletreasure;

import java.io.File;

import me.asofold.simpletreasure.configuration.Settings;
import me.asofold.simpletreasure.configuration.compatlayer.CompatConfig;
import me.asofold.simpletreasure.configuration.compatlayer.NewConfig;
import me.asofold.simpletreasure.tasks.TreasureHidingTask;

import org.bukkit.Server;
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
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()){
			Settings.getDefaultConfig(file, "").save();
			sender.sendMessage("[SimpleTreasure] Example configuration created.");
		}
		onReload(sender, "config.yml");
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
		if (settings.itemSettings.isEmpty()){
			player.sendMessage("[SimpleTreasure] No treasure defined!");
			return;
		}
		// Start a new hiding task:
		TreasureHidingTask task = new TreasureHidingTask(player.getLocation(), tries, radius, settings, player);
		if (!task.register(this)) player.sendMessage("[SimpleTreasure] Failed to start the task for hiding the treasures.");
		else player.sendMessage("[SimpleTreasure] Started the task for hiding the treasures ("+settings.fileName+").");
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
