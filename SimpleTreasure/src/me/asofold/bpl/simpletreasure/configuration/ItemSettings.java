package me.asofold.bpl.simpletreasure.configuration;

import java.util.LinkedList;
import java.util.List;

import me.asofold.bpl.simpletreasure.configuration.compatlayer.CompatConfig;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class ItemSettings {
	/**
	 * Items added with this treasure.
	 */
	public List<ItemStack> items = new LinkedList<ItemStack>();
	/**
	 * Weight for this treasure, used for limiting the items in one chest.
	 */
	public int weight = 1;
	/**
	 * Frequency part, used for determining which item to choose.
	 */
	public int part = 1;
	
	/**
	 * If to use 1...stack-size.
	 */
	public boolean random = false;
	
	public BlockSettings blockSettings = new BlockSettings();
	
	public void fromConfig(CompatConfig cfg, String prefix){
		weight = cfg.getInt(prefix + "weight", 1);
		part = cfg.getInt(prefix + "part", 1);
		random = cfg.getBoolean(prefix + "random" , false);
		blockSettings.fromConfig(cfg, prefix);
		items.clear();
		List<String> keys = cfg.getStringKeys(prefix + "items");
		for (String key : keys){
			ItemStack stack = readItemStack(cfg, prefix + "items." + key + ".");
			if (stack == null) Bukkit.getServer().getLogger().warning("[SimpleTreasure] Bad item at: " + prefix + "items."+key);
			else items.add(stack);
		}
	}
	
	/**
	 * 
	 * @param cfg
	 * @param prefix
	 * @return null if invalid.
	 */
	public static ItemStack readItemStack(CompatConfig cfg, String prefix){
		String type = cfg.getString(prefix + "type", null);
		Material mat;
		if (type == null) return null;
		try{
			mat = Material.getMaterial(Integer.parseInt(type.trim()));
		}
		catch (Throwable t){
			mat = Material.matchMaterial(type.trim());
		}
		if (mat == null) return null;
		int data = cfg.getInt(prefix + "data", 0);
		int amount = cfg.getInt(prefix + "amount", 1);
		ItemStack stack;
		try{
			if (mat.isBlock()) stack = new ItemStack(mat, amount, (short) 0, (byte) data);
			else stack = new ItemStack(mat, amount, (short) data, (byte) 0);
		}
		catch (Throwable t){
			return null;
		}
		if (cfg.contains(prefix + "enchantments")){
			List<String> inputs = cfg.getStringList(prefix + "enchantments", null);
			if (inputs != null){
				int i = 0;
				for (String input : inputs){
					i++;
					String[] split = input.split(":");
					int level = -1; // default: max level
					if (split.length > 2) Bukkit.getServer().getLogger().warning("[SimpleTreasure] Bad enchantment (more than 1x':') at: " + prefix + "enchantments / "+i+": "+input);
					else if (split.length == 2){
						try{
							level = Integer.parseInt(split[1].trim());
						}
						catch (NumberFormatException e){
							Bukkit.getServer().getLogger().warning("[SimpleTreasure] Bad enchantment level (set to 1) at: " + prefix + "enchantments / "+i+": "+input);
							level = 1;
						}
					}
					Enchantment ench = Enchantment.getByName(split[0].toUpperCase());
					if (ench == null) Bukkit.getServer().getLogger().warning("[SimpleTreasure] Bad enchantment name at: " + prefix + "enchantments / "+i+": "+input);
					else{
						if (level<0) level = ench.getMaxLevel();
						try{
							stack.addEnchantment(ench, Math.min(level, ench.getMaxLevel()));
						}
						catch (Throwable t){
							Bukkit.getServer().getLogger().warning("[SimpleTreasure] Invalid enchantment for "+mat.toString()+" at: " + prefix + "enchantments / "+i+": "+input);
						}
					}
				}
			}
		}
		return stack;
	}
}
