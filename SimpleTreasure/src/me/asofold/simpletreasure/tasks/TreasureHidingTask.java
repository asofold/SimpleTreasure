package me.asofold.simpletreasure.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import me.asofold.simpletreasure.configuration.ItemSettings;
import me.asofold.simpletreasure.configuration.Settings;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class TreasureHidingTask implements Runnable {
	int taskId = -1;
	
	final Settings settings;
	final int tries;
	int success = 0;
	int done = 0;
	final World world;
	final int cX;
	final int cZ;
	final int radius;
	
	final CommandSender notify;
	long tsNotify;
	
	final String taskName;
	
	private final Random random = new Random(System.currentTimeMillis()-23781);
	
	private final ItemSettings[] itemSettings;
	private final int[] parts;
	
	public TreasureHidingTask(World world, int x, int z, int tries, int radius,
			Settings settings, CommandSender notify) {
		this.settings = settings;
		this.world = world;
		cX = x;
		cZ = z;
		this.tries = tries;
		this.radius = radius;
		
		this.notify = notify;
		tsNotify = System.currentTimeMillis();
		
		taskName = "hide("+world.getName()+"/"+cX+","+cZ+":"+radius+"/"+tries+"x / "+settings.fileName+")";
		
		int len = settings.itemSettings.size();
		itemSettings = new ItemSettings[len];
		parts = new int[len];
		settings.itemSettings.toArray(itemSettings);
		int partSum = 0;
		for (int i = 0; i< len; i++){
			partSum += itemSettings[i].part;
			parts[i] = partSum;
		}
	}

	@Override
	public void run() {
		try{
			hideOne();
		}
		catch( Throwable t){
			if (notify != null) notify("[SimpleTreasure] Abort on error ("+t.getMessage()+"): "+taskName);
			cancel();
			Bukkit.getServer().getLogger().warning("[SimpleTreasure] Exeption on hiding ("+taskName+"):");
			t.printStackTrace();
		}
		done ++;
		if (done >= tries){
			if (notify != null) notify("[SimpleTreasure] Finished "+success+" chests: "+taskName);
			cancel();
		}
		else if (notify != null){
			long ts = System.currentTimeMillis();
			if (ts-tsNotify > 10000){
				tsNotify = ts;
				notify("[SimpleTreasure] Hidden "+success+" with "+done+"/"+tries+" tries: "+taskName);
			}
		}
	}

	private void notify(String message) {
		if (notify == null) return;
		try{
			notify.sendMessage(message);
		}
		catch(Throwable t){
			
		}
	}

	public boolean register(Plugin plugin) {
		cancel();
		taskId = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 3, 3);
		return taskId != -1;
	}
	
	public void cancel(){
		if (taskId != -1){
			Bukkit.getServer().getScheduler().cancelTask(taskId);
			taskId = -1;
		}
	}
	
	private final int findIndex(int[] parts, int part){
		if (part<0 || part > parts[parts.length-1]) return random.nextInt(parts.length);
		if (parts.length == 1) return parts[0];
		// TODO: binary search or similar...
		for (int i = 0; i< parts.length; i++){
			if (parts[i] >= part) return i;
		}
		return parts.length-1;
	}
	
	/**
	 * Find a valid block for this try.<br>
	 * Actually this does select a random treasure and checks in the allowed range for that one.
	 * @return
	 */
	private final Block findBlock(final Set<Chunk> chunks) {
		final int x = cX + random.nextInt(radius);
		final int z = cZ + random.nextInt(radius);
		
		// Use an example treasure as basic starting point.
		final int part = random.nextInt(parts[parts.length-1]+1);
		final int index = findIndex(parts, part);
		final ItemSettings treasure = itemSettings[index];
		final Set<Integer> replace = (treasure.blockSettings.allowedReplace == null)?settings.defaultBlockSettings.allowedReplace:treasure.blockSettings.allowedReplace;
		final Set<Integer> neighbours = (treasure.blockSettings.allowedNeighbours == null)?settings.defaultBlockSettings.allowedNeighbours:treasure.blockSettings.allowedNeighbours;
		final int yMin = getMinY(treasure);
		final int yMax = getMaxY(treasure, x, z);
		
		final int attempts; // TODO: config ! 
		if (yMin> yMax) return null;
		else if (yMin == yMax) attempts = 1;
		else attempts = settings.yAttempts;
		
		for (int i = 0; i<attempts; i++){
			final int y = yMin + random.nextInt(yMax- yMin + 1);
			if (!isValidChestPos(chunks, x, y, z, replace, neighbours)) continue;
			return world.getBlockAt(x, y, z);
		}
		return null;
	}
	
	private final boolean isValidChestPos(final Set<Chunk> chunks, final int x, final int y, final int z,
			final Set<Integer> replace, final Set<Integer> neighbours) {
		if (!checkId(chunks, x, y, z, replace)) return false;
		if (!neighbours.contains(world.getBlockTypeIdAt(x, y - 1, z))) return false;
		if (!neighbours.contains(world.getBlockTypeIdAt(x, y + 1, z))) return false;
		if (!checkId(chunks, x + 1, y, z, neighbours)) return false;
		if (!checkId(chunks, x - 1, y, z, neighbours)) return false;
		if (!checkId(chunks, x, y, z + 1, neighbours)) return false;
		if (!checkId(chunks, x, y, z - 1, neighbours)) return false;
		return true;
	}

	private final boolean checkId(final Set<Chunk> chunks, final int x, final int y, final int z, Set<Integer> allowed){
		final Chunk chunk = world.getChunkAt(x,z);
		if (!chunk.isLoaded()) chunk.load();
		chunks.add(chunk);
		return allowed.contains(world.getBlockTypeIdAt(x, y, z));
	}
	
	private int getMinY(ItemSettings treasure){
		if (treasure.blockSettings.yMin != null) return treasure.blockSettings.yMin;
		else if (settings.defaultBlockSettings.yMin != null) return settings.defaultBlockSettings.yMin;
		else return 0;
	}
	
	/**
	 * 
	 * @param treasure
	 * @param x
	 * @param z
	 * @return -1 on failure
	 */
	private int getMaxY(ItemSettings treasure, int x , int z){
		final int maxY = world.getHighestBlockYAt(x, z);
		final int ref;
		if (treasure.blockSettings.yMax != null) ref = treasure.blockSettings.yMax;
		else if (settings.defaultBlockSettings.yMax != null) ref = settings.defaultBlockSettings.yMax;
		else ref = maxY;
		return Math.min(maxY, ref);
	}
	
	private void hideOne() {
		Set<Chunk> chunks = new HashSet<Chunk>();
		for (int i = 0; i<settings.xzAttempts; i++){
			 final Block block = findBlock(chunks);
			 if (block != null){
				 ArrayList<ItemSettings> validTreasures = getValidTreasures(chunks, block);
				 // if (validTreasures.isEmpty()) continue; // TODO: internal error !
				 // -> rather heave it place an empty chest ?
				 Chest chest = makeChest(block);
				 populateChest(chest, validTreasures);
				 success ++;
				 break;
			 }
		}
		for (Chunk chunk : chunks){
			world.unloadChunk(chunk);
		}
	}

	/**
	 * Just make the block a chest.
	 * @param block
	 * @return
	 */
	private Chest makeChest(Block block) {
		block.setTypeIdAndData(Material.CHEST.getId(), (byte) 0, false);
		block.getState().update();
		return (Chest) block.getState();
	}
	
	/**
	 * Get a list of valid treasures for the position.
	 * @param block
	 * @return
	 */
	private final ArrayList<ItemSettings> getValidTreasures(final Set<Chunk> chunks, final Block block) {
		ArrayList<ItemSettings> valid = new ArrayList<ItemSettings>(itemSettings.length);
		final int x = block.getX();
		final int y = block.getY();
		final int z = block.getZ();
		for (int i = 0; i < itemSettings.length; i++){
			final ItemSettings treasure = itemSettings[i];
			final Set<Integer> replace = (treasure.blockSettings.allowedReplace == null)?settings.defaultBlockSettings.allowedReplace:treasure.blockSettings.allowedReplace;
			final Set<Integer> neighbours = (treasure.blockSettings.allowedNeighbours == null)?settings.defaultBlockSettings.allowedNeighbours:treasure.blockSettings.allowedNeighbours;
			final int yMin = getMinY(treasure);
			final int yMax = getMaxY(treasure, x, z);
			if (yMin > yMax) continue;
			if (yMin > y || yMax < y) continue;
			if (isValidChestPos(chunks, x, y, z, replace, neighbours)) valid.add(treasure);
		}
		return valid;
	}

	private final void populateChest(final Chest chest, final ArrayList<ItemSettings> validTreasures) {
		// TODO something faster ?
		if (validTreasures.isEmpty()) return;
		final int[] parts = new int[validTreasures.size()];
		int partSum = 0;
		for (int i = 0; i<parts.length; i++){
			partSum += validTreasures.get(i).part;
			parts[i] = partSum;
		}
		// create random parts:
		final int nLeft = settings.maxAdd;
		final int[] rand = new int[nLeft];
		for (int i = 0; i< nLeft; i++){
			rand[i] = random.nextInt(parts[parts.length-1] + 1);
		}
		// replace parts by indices:
		Arrays.sort(rand); // lol
		int startIndex = 0;
		for (int i = 0; i < nLeft; i++){
			int part = rand[i];
			for (int j = startIndex; j<parts.length; j++){
				if (parts[j] >= part){
					startIndex = j;
					rand[i] = j;
					break;
				}
			}
		}
		
		// finally fill in items with random choice from indices!
		int weight = 0; // total weight used.
		final Inventory inv = chest.getInventory();
		boolean chestFull = false;
		for (int i = 0; i<nLeft; i++){
			boolean hasAdded = false;
			final int j = random.nextInt(nLeft);
			final int index = rand[j];
			final ItemSettings treasure = validTreasures.get(index);
			// TODO: add, break if adding not possible.
			for (ItemStack stack : treasure.items){
				ItemStack ref = stack.clone();
				if (treasure.random){
					int amount = random.nextInt(ref.getAmount()+1);
					if (amount == 0) continue;
					ref.setAmount(amount);
				}
				hasAdded = true;
				HashMap<Integer, ItemStack> added = inv.addItem(ref);
				if (added == null || added.isEmpty()){
					// ok
				} else{
					chestFull = true;
					break;
				}
			}
			if (!hasAdded) continue;
			if (chestFull) break;
			weight += treasure.weight;
			if (weight >= settings.maxWeight) break;
			else if (weight >= settings.minWeight){
				if (random.nextDouble() < settings.pAbort) break;
			}
			// else : continue !
		}
	}

	
}
