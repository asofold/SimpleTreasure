package me.asofold.simpletreasure.configuration;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import me.asofold.simpletreasure.configuration.compatlayer.CompatConfig;
import me.asofold.simpletreasure.configuration.compatlayer.NewConfig;

import org.bukkit.Bukkit;
import org.bukkit.Material;

public class Settings {
	/**
	 * Default block settings, applicable if items don't define any.
	 */
	public BlockSettings defaultBlockSettings = new BlockSettings();
	/**
	 * Item settings for treasure parts...
	 */
	public List<ItemSettings> itemSettings = new LinkedList<ItemSettings>();
	/**
	 * The minimum weight that must be in a chest.
	 */
	public int minWeight = 1;
	/**
	 * The maximum weight that must be in a chest.
	 */
	public int maxWeight = 10;
	
	public int yAttempts = 5;
	
	public int xzAttempts = 1;
	
	public int maxAdd = 30;
	
	public double pAbort = 0.33;
	
	public String fileName;
	
	public Settings(String fileName) {
		this.fileName = fileName;
	}


	public void fromConfig(CompatConfig cfg, String prefix){
		Settings ref = new Settings(null);
		minWeight = cfg.getInt(prefix + "weight.min", ref.minWeight);
		maxWeight = cfg.getInt(prefix + "weight.max", ref.maxWeight);
		yAttempts = cfg.getInt(prefix + "attempts.y", ref.yAttempts);
		xzAttempts = cfg.getInt(prefix + "attempts.xz", ref.xzAttempts);
		maxAdd = cfg.getInt(prefix + "max-add", ref.maxAdd);
		pAbort = cfg.getDouble(prefix + "p-abort", ref.pAbort);
		defaultBlockSettings.fromConfig(cfg, prefix);
		itemSettings.clear();
		List<String> keys = cfg.getStringKeys(prefix + "treasures");
		for (String key : keys){
			ItemSettings item = new ItemSettings();
			item.fromConfig(cfg, prefix + "treasures."+key+".");
			if (item.items.isEmpty()) Bukkit.getServer().getLogger().warning("[SimpleTreasure] Bad treasure at: " + key);
			else itemSettings.add(item);
		}
	}
	
	public static CompatConfig getDefaultConfig(File file, String prefix){
		CompatConfig cfg = new NewConfig(file);
		Settings ref = new Settings(null);
		cfg.set(prefix + "weight.min", ref.minWeight);
		cfg.set(prefix + "weight.max", ref.maxWeight);
		cfg.set(prefix + "attempts.y", ref.yAttempts);
		cfg.set(prefix + "attempts.xz", ref.xzAttempts);
		cfg.set(prefix + "max-add", ref.maxAdd);
		cfg.set(prefix + "p-abort", ref.pAbort);
		// block settings -----------------
		cfg.set("y-min", 2);
		cfg.set("y-max", 255);
		List<String> stdBlocks = Arrays.asList(new String[]{
				"stone", "dirt", "sand", "sandstone", "gravel",
				"bedrock"});
		cfg.set("neighbours", new LinkedList<String>(stdBlocks));
		cfg.set("replace", new LinkedList<String>(stdBlocks));
		// example treasures --------
		// normal treasure:
		cfg.set("treasures.no1.items.i1.type", "iron_sword");
		cfg.set("treasures.no1.items.i1.enchantments", Arrays.asList(new String[]{"DAMAGE_ALL:2"}));
		cfg.set("treasures.no1.items.i2.type", "cookie");
		cfg.set("treasures.no1.items.i2.amount", 8);
		cfg.set("treasures.no1.random", true);
		// high treasure:
		cfg.set("treasures.no2.items.i1.type", "bow");
		cfg.set("treasures.no2.items.i2.type", "arrow");
		cfg.set("treasures.no2.items.i2.amount", 25);
		cfg.set("treasures.no2.y-min", 80);
		// water treasure:
		cfg.set("treasures.no3.items.i1.type", "sandstone");
		cfg.set("treasures.no3.items.i1.data", "1");
		cfg.set("treasures.no3.items.i1.amount", 55);
		cfg.set("treasures.no3.items.i2.type", "sandstone");
		cfg.set("treasures.no3.items.i2.data", "2");
		cfg.set("treasures.no3.items.i2.amount", 55);
		cfg.set("treasures.no3.items.i3.type", "IRON_HELMET");
		cfg.set("treasures.no3.items.i3.enchantments", Arrays.asList(new String[]{"WATER_WORKER"}));
		String p = "treasures.no3.";
		cfg.set(p + "y-min", 40);
		cfg.set(p + "y-max", 80);
		cfg.set(p + "replace", Arrays.asList(new String[]{"sand"}));
		cfg.set(p + "neighbours", Arrays.asList(new String[]{"sand", "8", "9", "ice", "sandstone"}));
		cfg.set("treasures.no3.random", true);
		// deep treasure:
		cfg.set("treasures.no4.items.i1.type", "diamond");
		cfg.set("treasures.no4.y-max", 40);
		// general:
		int tMax = 5;
		for (int i = 1 ; i < tMax; i++){
			cfg.set("treasures.no"+i+".part", tMax-i);
			cfg.set("treasures.no"+i+".weight", i);
		}
		return cfg;
	}
	
	
	/**
	 * Convenience method to allow for integers and item names (adapted from FatTnt).
	 * @param cfg
	 * @param path
	 * @return
	 */
	public static List<Integer> getIdList(CompatConfig cfg, String path){
		List<Integer> out = new LinkedList<Integer>();
		List<String> ref = cfg.getStringList(path);
		if (ref == null) return out;
		for ( Object x : ref){
			Integer id = null;
			if ( x instanceof Number){
				// just in case
				id = ((Number) x).intValue();
			} else if ( x instanceof String){
				try{
					id = Integer.parseInt((String) x);
				} catch(NumberFormatException exc) {
					Material mat = Material.matchMaterial((String) x);
					if ( mat != null){
						id = mat.getId();
					}
				}
			}
			if (id!=null){
				Material mat = Material.getMaterial(id);
				if (mat != null){
					out.add(id);
					continue;
				}
			}
			Bukkit.getServer().getLogger().warning("[SimpleTreasure] Bad item ("+path+"): "+x);
		}
		return out;
	}
}
