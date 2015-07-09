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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Functions
{
	// PROPERTIES
	private SS ss;
	private Location chestsLocation;
	//private Map<String, Map<String, Object>> actions;
	private List<String> players;
	// was too complicated to use the same
	private Map<String, BukkitTask> tasks;
	private Map<String, double[]> epfs;

	
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
		
		/* actions = new HashMap<String, Map<String, Object>>();
		actions.put("command",  ss.getConfig().getConfigurationSection("actions.command").getValues(false));
		actions.put("damage",   ss.getConfig().getConfigurationSection("actions.damage").getValues(false));
		actions.put("equip",    ss.getConfig().getConfigurationSection("actions.equip").getValues(false));
		actions.put("dispense", ss.getConfig().getConfigurationSection("actions.dispense").getValues(false));
		actions.put("creative", ss.getConfig().getConfigurationSection("actions.creative").getValues(false)); */
		
		players = ss.data.getConfig().getStringList("players");
		tasks = new HashMap<String, BukkitTask>();
		epfs = new HashMap<String, double[]>();
		epfs.put("PROTECTION_ENVIRONMENTAL", new double[]{0.04, 0.08, 0.12, 0.20});
		epfs.put("PROTECTION_FIRE",          new double[]{0.08, 0.16, 0.24, 0.36});
		epfs.put("PROTECTION_EXPLOSIONS",    new double[]{0.12, 0.20, 0.28, 0.44});
		epfs.put("PROTECTION_PROJECTILE",    new double[]{0.12, 0.20, 0.28, 0.44});
		epfs.put("PROTECTION_FALL",          new double[]{0.20, 0.32, 0.48, 0.72});
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
			ss.data.getConfig().set("players", players);
			ss.data.saveConfig();
			
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
	
	/* // keep armor, reveal skin
	// temp chest must be previously empty
	protected void suitUnequip(Player player, boolean message)
	{
		// clean task that triggered this action
		tasks.put(player.getName(), null);
		
		// avoid overwrites: dont save suit if already are saving one
		if (playerStoresSuit(player.getName()))
			return;
		
		// undress player and stores suit
		chestGetInventory(player.getName()).setContents(player.getInventory().getArmorContents());
		player.getInventory().setArmorContents(null);
		player.updateInventory();
		
		// display message?
		if (message)
			player.sendMessage("§E"+getMsg("skinShown"));
	} */
	
	/* private void suitUnequipLater(final Player player, int equipFor, final boolean message)
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
	} */
	
	
	/* // recover armor, cover skin
	// player cannot be wearing armor
	protected double suitEquip(Player player, int equipFor, boolean message)
	{
		// equip suit for 0 ticks = unequip immediately
		if (equipFor <= 0)
		{
			suitUnequip(player, message);
			return 0;
		}
		
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
	} */
	
	/* // dress or undress armor if are storing or not
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
	} */
	
	/* // check if player has some stored item (hopefully armor)
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
	} */
	
	// check armor slots, for use with dispenser event
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
	 * interval armor slots watch, some conditions could had changed, for use with dispenser event
	 * @return if there is still need to watch and continue with schedule
	 */
	private boolean armorWatchTask(Player player, int slot)
	{
		// check if player is still online and no in creative mode
		if (!player.isOnline() || player.getGameMode() == GameMode.CREATIVE)
			return true;
		
		// avoid overwrites: dont save suit if already are saving one (remember could be asynchronous)
		if (suitNumPieces(player.getName()) == 0)
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
	
	// returns armor piece matching position
	private int getArmorSlot(ItemStack piece)
	{
		// 0 = feet, 1 = legs, 2 = chest, 3 = head, -1 = unwearable
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
	
	// returns "wearability" of an item
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
	
	// returns armor value factor of given armor suit
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
	
	protected int suitNumPieces(String playerName)
	{
		// get saved armor suit and its number of pieces
		ItemStack[] suit = Arrays.copyOf(chestGetInventory(playerName).getContents(), 4);
		return suitNumPieces(suit);
	}
	
	private int suitNumPieces(ItemStack[] suit)
	{
		int numPieces = 0;
		for (ItemStack piece: suit)
			if (piece != null && !piece.getType().equals(Material.AIR))
				++numPieces;
		return numPieces;
	}
	
	private double getEnchantMitigation(String ench, List<Integer> levels)
	{
		double epf = 0;
		for (int level: levels)
		{
			// debug
			ss.log("enchant: "+ench+": "+levels);
			epf -= epfs.get(ench)[level-1];
		}
		
		// http://minecraft.gamepedia.com/Armor#Enchantments
		epf = Math.min(epf, 1.00)*Math.random()*0.50 + 0.50;
		return Math.min(epf, 0.80);
	}
	
	private void saveArmorLater2(final Player player, final Map<String, Object> set)
	{
		// update unequip suit task (tasks entry was created before)
		if (tasks.get(player.getName()) != null)
			tasks.get(player.getName()).cancel();
		
		// create a new and updated task
		BukkitTask task = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				// saveArmor2(Player player, Map<String, Object> set)
				saveArmor2(player, set);
			}
		}.runTaskLater(ss, (int)set.get("equipFor"));
		
		// store the new task to get accessible
		tasks.put(player.getName(), task);
	}
	
	private void saveArmor2(Player player, Map<String, Object> set)
	{
		// clean task already trigged this action
		tasks.put(player.getName(), null);

		// get saved armor suit and its number of pieces
		// avoid overwrites: dont save suit if already are saving one (remember could be asynchronous)
		if (suitNumPieces(player.getName()) != 0)
			return;
		
		// unequip armor suit and save it
		chestGetInventory(player.getName()).setContents(player.getInventory().getArmorContents());
		player.getInventory().setArmorContents(null);
		player.updateInventory();

		// display unequip message?
		if ((boolean)set.get("equipMsg"))
			player.sendMessage("§E"+getMsg("saveArmor"));
	}
	
	// when comes some damage/equip/command event
	protected Map<DamageModifier, Double> checkDamage(Player player, String cause, String action)
	{
		// get action settings
		Map<String, Object> set = ss.getConfig().getConfigurationSection("actions."+action).getValues(false);

		// get saved armor suit and its number of pieces
		ItemStack[] suit = Arrays.copyOf(chestGetInventory(player.getName()).getContents(), 4);
		int numPieces = suitNumPieces(suit);
		
		// debug
		ss.log("saved suit: "+suit);
		
		// manage toggle armor equip, explicit so no damage taken
		// if armor equipped save it now, if saved equip it
		if (cause.equals("TOGGLE") && numPieces == 0)
		{
			saveArmor2(player, set);
			return null;
		}
		
		// prepare damage reductions for return
		Map<DamageModifier, Double> reductions = new HashMap<DamageModifier, Double>();
		
		// player are wearing suit permanently: do nothing
		if (numPieces == 0 && !tasks.containsKey(player.getName()))
			return reductions;
		
		// if player saved a suit and got damage: equip armor with no reductions
		if (numPieces == 0 || cause.equals("TOGGLE") || cause.equals("NO_DAMAGE"))
			return reductions;
		
		// equip armor suit for 0 ticks = do nothing
		if ((int)set.get("equipFor") <= 0)
			return reductions;
		
		// calculate armor damage reduction
		double armorValue = 0.00;
		switch (cause)
		{
		//case "FALL":
		case "ENTITY_ATTACK":
		case "PROJECTILE":
		case "FIRE":
		case "LAVA":
		case "CONTACT":
		case "ENTITY_EXPLOSION":
		case "BLOCK_EXPLOSION":
		case "LIGHTNING":
		case "FALLING_BLOCK":
		case "THORNS":
			armorValue = suitArmorValue(player.getInventory().getArmorContents());
			if (armorValue != 0.00)
				reductions.put(DamageModifier.ARMOR, armorValue);
		}
		
		// calculate enchantments damage reduction
		Map<Enchantment, List<Integer>> enchants = getSuitEnchants(suit);
		double magicValue = 0.00;
		for (Enchantment ench: enchants.keySet())
		{
			switch (ench.toString())
			{
			case "PROTECTION_ENVIRONMENTAL":
				// protection, doesn't reduce damage from the Void, the /kill command, and hunger damage.
				if (cause.equals("STARVATION") || cause.equals("SUICIDE") || cause.equals("VOID"))
					continue;
				magicValue -= getEnchantMitigation(ench.toString(), enchants.get(ench));
				continue;
			case "PROTECTION_FIRE":
				// fire protection against lava contact, fire contact and burning
				if (!cause.equals("LAVA") && !cause.equals("FIRE") && !cause.equals("FIRE_TICK"))
					continue;
				magicValue -= getEnchantMitigation(ench.toString(), enchants.get(ench));
				continue;
			case "PROTECTION_EXPLOSIONS":
				// blast protection 
				if (!cause.equals("ENTITY_EXPLOSION") && !cause.equals("BLOCK_EXPLOSION"))
					continue;
				magicValue -= getEnchantMitigation(ench.toString(), enchants.get(ench));
				continue;
			case "PROTECTION_PROJECTILE":
				// projectile protection against arrows, fireballs, snowballs, eggs, wither skulls, etc
				if (!cause.equals("PROJECTILE"))
					continue;
				magicValue -= getEnchantMitigation(ench.toString(), enchants.get(ench));
				continue;
			case "PROTECTION_FALL":
				// feather falling, protection against falling from height and ender pearls burning
				if (!cause.equals("FALL"))
					continue;
				magicValue -= getEnchantMitigation(ench.toString(), enchants.get(ench));
				continue;
			}
		}
		if (magicValue != 0.00)
			reductions.put(DamageModifier.MAGIC, magicValue);
		
		// falling block calc at the end, multiply all reductions
		if (cause.equals("FALLING_BLOCK") && suit[3] != null)
		{
			for (DamageModifier mod: reductions.keySet())
				reductions.put(mod, reductions.get(mod) * 0.75);
			reductions.put(DamageModifier.HARD_HAT, -0.25);
		}
		
		// equipping armor suit is worth if there is some reduction
		if (armorValue != 0.00 || magicValue != 0.00)
		{
			// equip suit now
			player.getInventory().setArmorContents(suit);
			chestGetInventory(player.getName()).clear();
			player.updateInventory();
			
			// equip it temporarily?
			if ((int)set.get("equipFor") < 200 && tasks.get(player.getName()) != null)
				saveArmorLater2(player, set);
			
			// display equip message if corresponds
			if ((boolean)set.get("equipMsg"))
				player.sendMessage("§E"+getMsg("saveArmor"));
		}
			
		// all done
		return reductions;
	}
	
	private String getMsg(String key)
	{
		String msg = ss.getConfig().getString("lang."+key);
		if (msg == null)
			return key;
		else
			return msg;
	}
	
	private Map<Enchantment, List<Integer>> getSuitEnchants(ItemStack[] suit)
	{
		// a map with multiple values, like multimap
		Map<Enchantment, List<Integer>> enchants = new HashMap<Enchantment, List<Integer>>();
		for (ItemStack piece: suit)
		{
			// could be replaced?
			for (Enchantment ench: piece.getEnchantments().keySet())
			{
				// debug
				ss.log("Enchant name: "+ench.getName());
				ss.log("Enchant string: "+ench.toString());
				
				if (!enchants.containsKey(ench))
					enchants.put(ench, new ArrayList<Integer>());
				enchants.get(ench).add(piece.getEnchantmentLevel(ench));
			}
		}
		return enchants;
	}
}