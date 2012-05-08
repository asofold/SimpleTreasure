package me.asofold.simpletreasure.configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.asofold.simpletreasure.configuration.compatlayer.CompatConfig;

public class BlockSettings {
	/**
	 * Chests must be at least at this level.
	 */
	public Integer yMin = 0;
	/**
	 * Chests must be at most this high.
	 */
	public Integer yMax = 255;
	/**
	 * These block types must be orthogonally adjactant to the chest.
	 */
	public Set<Integer> allowedNeighbours = null;
	/**
	 * Only these block types may be replaced by a chest.
	 */
	public Set<Integer> allowedReplace = null;
	
	public void fromConfig(CompatConfig cfg, String prefix){
		yMin = cfg.getInt(prefix + "y-min", null);
		yMax = cfg.getInt(prefix + "y-max", null);
		List<Integer> neighbours =  Settings.getIdList(cfg, prefix + "neighbours");
		if (neighbours.isEmpty()) allowedNeighbours = null;
		else {
			allowedNeighbours = new HashSet<Integer>();
			allowedNeighbours.addAll(neighbours);
		}
		List<Integer> replace =  Settings.getIdList(cfg, prefix + "replace");
		if (replace.isEmpty()) allowedReplace = null;
		else {
			allowedReplace= new HashSet<Integer>();
			allowedReplace.addAll(replace);
		}
	}

}
