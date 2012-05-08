package me.asofold.simpletreasure.configuration.compatlayer;

import java.util.Map;

public class ConfigUtil {
	
	public static final int canaryInt = Integer.MIN_VALUE +7;
	public static final long canaryLong = Long.MIN_VALUE + 7L;
	public static final double canaryDouble = Double.MIN_VALUE*.7;
	
	public static String stringPath( String path){
		return stringPath(path, '.');
	}
	
	public static String stringPath( String path , char sep ){
		String useSep = (sep=='.')?"\\.":""+sep;
		String[] split = path.split(useSep);
		StringBuilder builder = new StringBuilder();
		builder.append(stringPart(split[0]));
		for (int i = 1; i<split.length; i++){
			builder.append(sep+stringPart(split[i]));
		}
		return builder.toString();
	}
	
	/**
	 * Aimed at numbers in paths.
	 * @param cfg
	 * @param path
	 * @return
	 */
	public static String bestPath(CompatConfig cfg, String path){
		return bestPath(cfg, path, '.');
	}
			
			
	/**
	 * Aimed at numbers in paths.
	 * @param cfg
	 * @param path
	 * @param sep
	 * @return
	 */
	public static String bestPath(CompatConfig cfg, String path, char sep){
		String useSep = (sep=='.')?"\\.":""+sep;
		String[] split = path.split(useSep);
		String res;
		if (cfg.hasEntry(split[0]) )res = split[0];
		else{
			res = stringPart(split[0]);
			if ( !cfg.hasEntry(res)) return path;
		}
		for (int i = 1; i<split.length; i++){
			if (cfg.hasEntry(res+sep+split[i]) ) res += sep+split[i];
			else{
				res += sep+stringPart(split[i]);
				if ( !cfg.hasEntry(res)) return path;
			}
		}
		return res;
	}
	
	public static String stringPart(String input){
		try{
			Double.parseDouble(input);
			return "'"+input+"'";
		} catch (NumberFormatException e){
		}
		try{
			Long.parseLong(input);
			return "'"+input+"'";
		} catch (NumberFormatException e){
		}
		try{
			Integer.parseInt(input);
			return "'"+input+"'";
		} catch (NumberFormatException e){
		}
		return input;
	}
	
	public static boolean forceDefaults(CompatConfig defaults, CompatConfig config){
		Map<String ,Object> all = defaults.getValuesDeep();
		boolean changed = false;
		for ( String path : all.keySet()){
			if ( !config.hasEntry(path)){
				config.setProperty(path, defaults.getProperty(path, null));
				changed = true;
			}
		}
		return changed;
	}
}
