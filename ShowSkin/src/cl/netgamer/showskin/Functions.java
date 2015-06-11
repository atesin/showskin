package cl.netgamer.showskin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class Functions
{
	// PROPERTIES
	private SS ss;
	private ConfigurationSection players;
	// not needed, instead check if has some item in chest
	//private Map<String, Boolean> storing = new HashMap<String, Boolean>();
	
	// CONSTRUCTOR
	public Functions(SS ss)
	{
		this.ss = ss;
		players = ss.c.getConfig().getConfigurationSection("players");
	}
	
	// FUNCTIONS
	
	// on player join load player config or create if not exist
	protected void loadConf(Player player)
	{
		// try to load config for this player or create
		String p = player.getName();
		boolean storing;
		if (players.contains(p))
		{
			storing = players.getBoolean(p);
		}
		else
		{
			// set default and save config
			storing = ss.defaultUndress;
			players.set(p, storing);
			ss.c.saveConfig();
			
			// create chests
			createChests(player);
		}
		
		// store or retrieve armor
		if (storing) storeArmor(player);
		else retrieveArmor(player);
	}
	
	// on load config create chests
	private void createChests(Player player)
	{
		// get player position in config first
		int pos = new ArrayList<String>(players.getKeys(false)).indexOf(player.getName());
		ss.chests.clone().add(pos*2, 0, 0).getBlock().setType(Material.CHEST);
	}
	
	// get player temp chest inventory already created
	private Inventory getStore(String player)
	{
		int pos = new ArrayList<String>(players.getKeys(false)).indexOf(player);
		return ((Chest)ss.chests.clone().add(pos*2, 0, 0).getBlock().getState()).getInventory();
	}
	
	// keep armor, reveal skin
	// temp chest must be previously empty
	private void storeArmor(Player player)
	{
		Inventory i = getStore(player.getName());
		
		// check empty store for security
		// trim inventory nulls
		List<ItemStack> l = new ArrayList<ItemStack>();
		for (ItemStack w: i.getContents())
		{
			if (w == null)
				continue;
			l.add(w);
		}
		
		if (l.size() != 0)
			return;
		
		// fill chest and undress player
		i.setContents(player.getInventory().getArmorContents());
		player.getInventory().setArmorContents(null);
		player.updateInventory();
	}
	
	// recover armor, cover skin
	// player cannot be wearing armor
	protected void retrieveArmor(Player player)
	{
		Inventory i = getStore(player.getName());
		
		// dress player and empty chest
		// this relies on previous checks
		player.getInventory().setArmorContents(Arrays.copyOf(i.getContents(), 4));
		i.clear();;
		player.updateInventory();
	}
	
	// dress or undress armor if are storing or not
	protected void toggleArmor(Player player)
	{
		// check store contents
		if (hasStoredArmor(player.getName()))
			retrieveArmor(player);
		else
			storeArmor(player);
	}
	
	// check if player has some stored item (hopefully armor)
	protected boolean hasStoredArmor(String player)
	{
		Inventory i = getStore(player);
		
		// check empty store for security
		// trim inventory nulls
		List<ItemStack> l = new ArrayList<ItemStack>();
		for (ItemStack w: i.getContents())
		{
			if (w == null || w.getType() == Material.AIR)
				continue;
			l.add(w);
		}
		
		return (l.size() != 0);
	}
	
	protected void checkPlayerArmor(final Player player, ItemStack armor)
	{
		// view player matching slot
		final int slot = armorSlot(armor);
		if (slot < 0)
			return;
		
		// slot available?
		ItemStack p = player.getInventory().getArmorContents()[slot];
		if (p != null && p.getType() != Material.AIR)
			return;
		
		// schedule anonymous task
		new BukkitRunnable()
		{
			// task properties
			int tleft = 10;

			@Override
			public void run()
			{
				// countdown and check player armor, some conditions could change
				--tleft;
				if (tleft < 0 || !watchPlayerArmorTask(player, slot))
					this.cancel();
			}
		}.runTaskTimer(ss, 0, 1);
	}
	
	/**
	 * some conditions could had changed
	 * @return if there is still need to watch and continue with schedule
	 */
	private boolean watchPlayerArmorTask(Player player, int slot)
	{
		// check if player is creative or has stored armor
		if (!player.isOnline() || player.getGameMode() == GameMode.CREATIVE || !hasStoredArmor(player.getName()))
			return true;
		
		// new armor equipped?
		ItemStack piece = player.getInventory().getArmorContents()[slot];
		if (piece == null || piece.getType() == Material.AIR)
			return true;
		
		// try to pick armor or drop it
		for (ItemStack excess: player.getInventory().addItem(piece).values())
			player.getWorld().dropItem(player.getLocation(), excess);
		
		// clear armor slot and cancel schedule
		switch (slot)
		{
		case 0:
			player.getInventory().setBoots(null);
			return false;
		case 1:
			player.getInventory().setLeggings(null);
			return false;
		case 2:
			player.getInventory().setChestplate(null);
			return false;
		case 3:
			player.getInventory().setHelmet(null);
		}
		return false;
	}
	
	// for use with getArmorContents()
	private int armorSlot(ItemStack piece)
	{
		/* 
		 * pos = 0 feet / 1 legs / 2 chest / 3 head
		 * leat = 301 LEATHER_BOOTS / 300 LEATHER_LEGGINGS / 299 LEATHER_CHESTPLATE / 298 LEATHER_HELMET
		 * chai = 305 CHAINMAIL_BOOTS / 304 CHAINMAIL_LEGGINGS / 303 CHAINMAIL_CHESTPLATE / 302 CHAINMAIL_HELMET
		 * iron = 309 IRON_BOOTS / 308 IRON_LEGGINGS / 307 IRON_CHESTPLATE / 306 IRON_HELMET
		 * diam = 313 DIAMOND_BOOTS / 312 DIAMOND_LEGGINGS / 311 DIAMOND_CHESTPLATE / 310 DIAMOND_HELMET
		 * gold = 317 GOLD_BOOTS / 316 GOLD_LEGGINGS / 315 GOLD_CHESTPLATE / 314 GOLD_HELMET
		 */
		
		if (piece == null)
			return -1;
		
		switch (piece.getData().getItemType().toString())
		{
		case "LEATHER_BOOTS":
		case "CHAINMAIL_BOOTS":
		case "IRON_BOOTS":
		case "DIAMOND_BOOTS":
		case "GOLD_BOOTS":
			return 0;
		case "LEATHER_LEGGINGS":
		case "CHAINMAIL_LEGGINGS":
		case "IRON_LEGGINGS":
		case "DIAMOND_LEGGINGS":
		case "GOLD_LEGGINGS":
			return 1;
		case "LEATHER_CHESTPLATE":
		case "CHAINMAIL_CHESTPLATE":
		case "IRON_CHESTPLATE":
		case "DIAMOND_CHESTPLATE":
		case "GOLD_CHESTPLATE":
			return 2;
		case "LEATHER_HELMET":
		case "CHAINMAIL_HELMET":
		case "IRON_HELMET":
		case "DIAMOND_HELMET":
		case "GOLD_HELMET":
		case "PUMPKIN":
		case "SKULL_ITEM":
			return 3;
		default:
			return -1;
		}
	}
	
	protected int armorLevel(ItemStack wear)
	{
		int level = 0;
		switch (wear.getData().getItemType().toString())
		{
		// heads and armor
		case "PUMPKIN":
		case "SKULL_ITEM":
			++level;
		// just armor
		case "LEATHER_BOOTS":
		case "CHAINMAIL_BOOTS":
		case "IRON_BOOTS":
		case "DIAMOND_BOOTS":
		case "GOLD_BOOTS":
		case "LEATHER_LEGGINGS":
		case "CHAINMAIL_LEGGINGS":
		case "IRON_LEGGINGS":
		case "DIAMOND_LEGGINGS":
		case "GOLD_LEGGINGS":
		case "LEATHER_CHESTPLATE":
		case "CHAINMAIL_CHESTPLATE":
		case "IRON_CHESTPLATE":
		case "DIAMOND_CHESTPLATE":
		case "GOLD_CHESTPLATE":
		case "LEATHER_HELMET":
		case "CHAINMAIL_HELMET":
		case "IRON_HELMET":
		case "DIAMOND_HELMET":
		case "GOLD_HELMET":
			++level;;
		}
		return level;
	}
}