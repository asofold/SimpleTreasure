package me.asofold.simpletreasure.configuration;

import java.util.LinkedList;
import java.util.List;

import me.asofold.simpletreasure.configuration.compatlayer.CompatConfig;

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
