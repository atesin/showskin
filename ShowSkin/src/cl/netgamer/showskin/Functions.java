package cl.netgamer.showskin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Functions
{
	// PROPERTIES
	private SS ss;
	private Location chestsLocation;
	private List<String> players;
	// was too complicated to use the same
	private Map<String, BukkitTask> tasks;
	
	// CONSTRUCTOR
	public Functions(SS ss)
	{
		this.ss = ss;
		chestsLocation = new Location
		(
			ss.getServer().getWorld(ss.getConfig().getConfigurationSection("chestsLocation").getString("world")),
			ss.getConfig().getConfigurationSection("chestsLocation").getDouble("x"),
			ss.getConfig().getConfigurationSection("chestsLocation").getDouble("y"),
			ss.getConfig().getConfigurationSection("chestsLocation").getDouble("z")
		);
		players = ss.conf.getConfig().getStringList("players");
		tasks = new HashMap<String, BukkitTask>();
	}
	
	// FUNCTIONS
	
	// on player join load player config or create if not exist
	protected void playerLoadConf(Player player)
	{
		// load player config or create it
		String name = player.getName();
		if (!players.contains(name))
		{
			// set default and save config
			players.add(name);
			ss.conf.getConfig().set("players", players);
			ss.conf.saveConfig();
			
			// create chests
			chestCreate(player);
		}
		
		// create unequip task entry if not exists
		if (!tasks.containsKey(player.getName()))
			tasks.put(player.getName(), null);
	}
	
	// on load config create chests
	private void chestCreate(Player player)
	{
		// get player position in config first
		int pos = players.indexOf(player.getName());
		chestsLocation.clone().add(pos+pos, 0, 0).getBlock().setType(Material.CHEST);
	}
	
	// get player temp chest inventory already created
	private Inventory chestGetInventory(String player)
	{
		int pos = players.indexOf(player);
		return ((Chest)chestsLocation.clone().add(pos*2, 0, 0).getBlock().getState()).getInventory();
	}
	
	// keep armor, reveal skin
	// temp chest must be previously empty
	protected void suitUnequip(Player player, boolean message)
	{
		// clean task that triggered this action
		tasks.put(player.getName(), null);
		
		// avoid overwrites: dont unequip suit if already are storing one
		if (playerStoresSuit(player.getName()))
			return;
		
		// undress player and stores suit
		chestGetInventory(player.getName()).setContents(player.getInventory().getArmorContents());
		player.getInventory().setArmorContents(null);
		player.updateInventory();
		
		// display message?
		if (message)
			player.sendMessage("§E"+ss.lang.getString("skinShown"));
	}
	
	private void suitUnequipLater(final Player player, int equipFor, final boolean message)
	{
		// update unequip suit task (tasks entry was created before)
		if (tasks.get(player.getName()) != null)
			tasks.get(player.getName()).cancel();
		
		// creating a new and updated task
		BukkitTask task = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				suitUnequip(player, message);
			}
		}.runTaskLater(ss, equipFor);
		
		// store new task to get accessible
		tasks.put(player.getName(), task);
	}
	
	
	// recover armor, cover skin
	// player cannot be wearing armor
	protected double suitEquip(Player player, int equipFor, boolean message)
	{
		// equip suit for 0 ticks = no equip
		if (equipFor <= 0)
			return 0;
		
		// equip suit temporarily?, equip suit now and forever?, not storing scheduled?
		String name = player.getName();
		if (equipFor < 200 && tasks.get(name) != null)
			suitUnequipLater(player, equipFor, message);
		
		// no stored suit means player is wearing it, avoid overwrite
		if (!playerStoresSuit(name))
			return 0;
		
		// dress player with chest inventory, this relies on previous checks
		Inventory inv = chestGetInventory(name);
		ItemStack[] suit = Arrays.copyOf(inv.getContents(), 4);
		player.getInventory().setArmorContents(suit);
		inv.clear();
		player.updateInventory();
		
		// display message?
		if (message)
			player.sendMessage("§E"+ss.lang.getString("skinCovered"));
		
		return suitArmorValue(suit);
	}
	
	// dress or undress armor if are storing or not
	protected void suitToggle(Player player, int equipFor, boolean message)
	{
		// create unequip task entry if not exists
		String name = player.getName();
		if (!tasks.containsKey(name))
			tasks.put(name, null);
		
		// check chest contents before
		//if (playerStoresSuit(player.getName()))
		if (tasks.get(name) == null && !playerStoresSuit(name))
			suitUnequip(player, message);
		else
			suitEquip(player, equipFor, message);
	}
	
	// check if player has some stored item (hopefully armor)
	protected boolean playerStoresSuit(String player)
	{
		Inventory i = chestGetInventory(player);
		
		// count inventory items
		int l = 0;
		ItemStack[] c = i.getContents();
		for (int k = 0;k < 4;++k)
		{
			if (c[k] == null || c[k].getType() == Material.AIR)
				continue;
			++l;
		}
		
		return (l != 0);
	}
	
	// check armor slots, use with dispenser event
	protected void armorCheck(final Player player, ItemStack armor)
	{
		// view player matching slot
		final int slot = getArmorSlot(armor);
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
				if (tleft < 0 || !armorWatchTask(player, slot))
					this.cancel();
			}
		}.runTaskTimer(ss, 0, 1);
	}
	
	/**
	 * interval armor slots watch, some conditions could had changed
	 * @return if there is still need to watch and continue with schedule
	 */
	private boolean armorWatchTask(Player player, int slot)
	{
		// check if player is creative or has stored armor
		if (!player.isOnline() || player.getGameMode() == GameMode.CREATIVE || !playerStoresSuit(player.getName()))
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
	private int getArmorSlot(ItemStack piece)
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
	
	protected int getArmorLevel(ItemStack wear)
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
	
	
	// faltan los encantamientos y todo eso
	
	private double suitArmorValue(ItemStack[] suit)
	{
		if (suit == null)
			return 0;
		
		double value = 0;
		for (ItemStack piece: suit)
			switch (piece == null ? "AIR" : piece.getType().toString())
			{
			case "AIR":
				continue;
			case "DIAMOND_CHESTPLATE":
				value -= 0.32;
				continue;
			case "DIAMOND_LEGGINGS":
			case "IRON_CHESTPLATE":
				value -= 0.24;
				continue;
			case "CHAINMAIL_CHESTPLATE":
			case "GOLD_CHESTPLATE":
			case "IRON_LEGGINGS":
				value -= 0.2;
				continue;
			case "CHAINMAIL_LEGGINGS":
				value -= 0.16;
			case "DIAMOND_BOOTS":
			case "DIAMOND_HELMET":
			case "GOLD_LEGGINGS":
			case "LEATHER_CHESTPLATE":
				value -= 0.12;
				continue;
			case "CHAINMAIL_HELMET":
			case "GOLD_HELMET":
			case "IRON_BOOTS":
			case "IRON_HELMET":
			case "LEATHER_LEGGINGS":
				value -= 0.08;
				continue;
			case "CHAINMAIL_BOOTS":
			case "GOLD_BOOTS":
			case "LEATHER_BOOTS":
			case "LEATHER_HELMET":
				value -= 0.04;
			}
		return value;
	}
}