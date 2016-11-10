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
	private List<String> players;
	private Map<String, Object> creativeAction = new HashMap<String, Object>();
	private Map<String, BukkitTask> tasks = new HashMap<String, BukkitTask>();
	private Map<String, int[]> armorStatus = new HashMap<String, int[]>();
	private Map<String, double[]> epfs = new HashMap<String, double[]>();
	// \u00A7B=cyan, \u00A7E=yellow, \u00A7D=magenta
	String[] colors = new String[]{"\u00A7D", "\u00A7E", "\u00A7B"};
	String[] pieceNames;
	String[] pieceTitles;
	String[] statusLines;

	
	// CONSTRUCTOR
	public Functions(SS ss)
	{
		this.ss = ss;
		chestsLocation = new Location
		(
			ss.getServer().getWorld(ss.getConfig().getString("chestsLocation.world")),
			ss.getConfig().getDouble("chestsLocation.x"),
			ss.getConfig().getDouble("chestsLocation.y"),
			ss.getConfig().getDouble("chestsLocation.z")
		);
		players = ss.data.getConfig().getStringList("players");
		creativeAction.put("equipFor", (int) 200);
		creativeAction.put("equipMsg", (boolean) true);
		creativeAction.put("statusMsg", (boolean) false);
		epfs.put("PROTECTION_ENVIRONMENTAL", new double[]{0.04, 0.08, 0.12, 0.20});
		epfs.put("PROTECTION_FIRE",          new double[]{0.08, 0.16, 0.24, 0.36});
		epfs.put("PROTECTION_EXPLOSIONS",    new double[]{0.12, 0.20, 0.28, 0.44});
		epfs.put("PROTECTION_PROJECTILE",    new double[]{0.12, 0.20, 0.28, 0.44});
		epfs.put("PROTECTION_FALL",          new double[]{0.20, 0.32, 0.48, 0.72});
		String[] s = new String[0];
		pieceNames = ss.getConfig().getStringList("lang.pieceNames").toArray(s);
		pieceTitles = ss.getConfig().getStringList("lang.pieceTitles").toArray(s);
		statusLines = ss.getConfig().getStringList("lang.statusLines").toArray(s);
	}
	
	// FUNCTIONS
	
	// CREATE NEW PLAYER CONFIGURATION ON LOGIN
	protected void loadPlayerData(Player player)
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
	
	// CREATE PLAYER TEMP CHEST
	private void chestCreate(Player player)
	{
		// get player position in config first
		int pos = players.indexOf(player.getName());
		chestsLocation.clone().add(pos+pos, 0, 0).getBlock().setType(Material.CHEST);
	}
	
	// GET PLAYER TEMP CHEST
	// get player temp chest inventory already created
	private Inventory getTempInventory(String player)
	{
		int pos = players.indexOf(player);
		return ((Chest)chestsLocation.clone().add(pos*2, 0, 0).getBlock().getState()).getInventory();
	}
	
	// CHECK PLAYER ARMOR SLOTS AT INTERVALS, FOR USE WITH DISPENSER EVENTS
	protected void armorWatch(final Player player, ItemStack armor)
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
	
	// CHECK PLAYER ARMOR SLOTS TASK, FOR USE WITH DISPENSER EVENTS
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
	
	// DROP ARMOR SUIT WHEN PLAYER DIES
	void dropSuit(Player player)
	{
		// get previous
		Location loc = player.getLocation();
		Inventory inv = getTempInventory(player.getName());
		
		// loop suit to drop pieces
		for (ItemStack piece: Arrays.copyOf(inv.getContents(), 4))
			loc.getWorld().dropItemNaturally(loc, piece);
		
		// clear temp intentory
		inv.clear();
	}
	
	// GET ARMOR SLOT POSITION
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
		//case "ELYTRA":
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
	
	/** Check item wearability
	 * 0: unwearable item
	 * 1: just wearable by dragging explicitly into armor slots (e.g. mob heads)
	 * 2: also wearable by using it (right click, shift click) or by a dispenser (e.g. normal armor)
	 */
	protected int getItemWearability(ItemStack wear)
	{
		switch (wear.getData().getItemType().toString())
		{
		// heads
		case "PUMPKIN":
		case "SKULL_ITEM":
			return 1;
		// armor pieces
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
		//case "ELYTRA":
			return 2;
		}
		// not equippable as armor
		return 0;
	}
	
	// GET ARMOR VALUE OF GIVEN ARMOR SUIT
	private double getArmorReduction(ItemStack[] suit)
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
	
	// GET NUMBER OF ARMOR PIECES THE PLAYER HAS SAVED
	protected int suitNumPieces(String playerName)
	{
		// get saved armor suit and its number of pieces
		ItemStack[] suit = Arrays.copyOf(getTempInventory(playerName).getContents(), 4);
		return suitNumPieces(suit);
	}
	
	// GET NUMBER OF ARMOR PIECES OF GIVEN ARMOR SUIT
	private int suitNumPieces(ItemStack[] suit)
	{
		int numPieces = 0;
		for (ItemStack piece: suit)
			if (piece != null && !piece.getType().equals(Material.AIR))
				++numPieces;
		return numPieces;
	}
	
	// GET PROTECTION ENCHANTMENTS DAMAGE REDUCTION VALUE
	private double getMagicReduction(String ench, List<Integer> levels)
	{
		double epf = 0.0;
		for (int level: levels)
			epf -= epfs.get(ench)[level-1];
		
		// http://minecraft.gamepedia.com/Armor#Enchantments
		epf = Math.min(epf, 1.0)*Math.random()*0.5 + 0.5;
		return Math.min(epf, 0.8);
	}
	
	// SCHEDULE UNEQUIP AND SAVE CURRENT ARMOR SUIT
	private void saveArmorLater(final Player player, final Map<String, Object> act)
	{
		// update save armor task
		if (tasks.containsKey(player.getName()))
			if (tasks.get(player.getName()) != null)
				tasks.get(player.getName()).cancel();
		
		// create a new and updated task
		BukkitTask task = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				saveArmor(player, act);
			}
		}.runTaskLater(ss, (int)act.get("equipFor"));
		
		// store the new task to get accessible
		tasks.put(player.getName(), task);
	}
	
	// SAVE ARMOR NOW, REMEMBER COULD BE ASYNCRONOUS
	private void saveArmor(Player player, Map<String, Object> act)
	{
		// clean task that triggered this action if possible
		if (tasks.containsKey(player.getName()))
			if (tasks.get(player.getName()) != null)
				tasks.get(player.getName()).cancel();
		tasks.remove(player.getName());
				
		// get saved armor suit and its number of pieces
		// avoid overwrites: dont save suit if already are saving one (remember could be asynchronous)
		int numPieces = suitNumPieces(player.getName());
		if (numPieces != 0)
			return;
		
		// unequip armor suit and save it
		ItemStack[] suit = player.getInventory().getArmorContents();
		getTempInventory(player.getName()).setContents(suit);
		player.getInventory().setArmorContents(null);
		player.updateInventory();

		// display unequip message?
		if ((boolean)act.get("equipMsg"))
			player.sendMessage("\u00A7B"+ss.getConfig().getString("lang.saveArmor"));
		
		// display status messages
		if ((boolean)act.get("statusMsg"))
			printStatus(player, suit);
	}
	
	// TOGGLE ARMOR EQUIP, TRIGGERED BY COMMAND SO NO REAL DAMAGE
	protected void toggleArmor(Player player, String action)
	{
		// get suit and number of pieces fisrt to pass ahead
		ItemStack[] suit = Arrays.copyOf(getTempInventory(player.getName()).getContents(), 4);
		int numPieces = suitNumPieces(suit);
		
		// save armor only when equipped permanently
		if (numPieces == 0 && !tasks.containsKey(player.getName()))
			saveArmor(player, ss.getConfig().getConfigurationSection("actions."+action).getValues(false));
		else
			checkEquipArmor(player, "FORCE", action);
	}
	
	// PROCESS EQUIP SAVED ARMOR REQUEST
	protected Map<DamageModifier, Double> checkEquipArmor(Player player, String cause, String action)
	{
		// fix suit if null
		ItemStack[] suit = Arrays.copyOf(getTempInventory(player.getName()).getContents(), 4);
		return checkEquipArmor(player, cause, action, suit);
	}
	
	// PROCESS EQUIP SAVED ARMOR REQUEST
	protected Map<DamageModifier, Double> checkEquipArmor(Player player, String cause, String action, ItemStack[] suit)
	{
		// get action settings
		Map<String, Object> act = action == "force" ? creativeAction : ss.getConfig().getConfigurationSection("actions."+action).getValues(false);

		// get saved armor suit pieces number
		int numPieces = suitNumPieces(suit);
		
		// prepare damage reductions for return
		Map<DamageModifier, Double> reductions = new HashMap<DamageModifier, Double>();
		
		// debug
		//ss.log("> number pieces: "+numPieces);
		//ss.log("> task contains: "+tasks.containsKey(player.getName()));
		
		// if equipped permanently do nothing
		if (numPieces == 0 && !tasks.containsKey(player.getName()))
			return reductions;
		
		// equip armor suit for 0 ticks = unequip now
		if ((int)act.get("equipFor") <= 0)
		{
			saveArmor(player, act);
			return reductions;
		}
		//ss.log("+ do wear");

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
			armorValue = getArmorReduction(suit);
			if (armorValue != 0.00)
				reductions.put(DamageModifier.ARMOR, armorValue);
		}
		
		// calculate enchantments damage reduction
		Map<Enchantment, List<Integer>> enchants = getSuitEnchants(suit);
		double magicValue = 0.00;
		for (Enchantment ench: enchants.keySet())
		{
			switch (ench.getName())
			{
			case "PROTECTION_ENVIRONMENTAL":
				// protection, doesn't reduce damage from the Void, the /kill command, and hunger damage.
				if (!cause.equals("STARVATION") && !cause.equals("SUICIDE") && !cause.equals("VOID") && !cause.equals("FORCE"))
					magicValue -= getMagicReduction(ench.getName(), enchants.get(ench));
				continue;
			case "PROTECTION_FIRE":
				// fire protection against lava contact, fire contact and burning
				if (cause.equals("LAVA") || cause.equals("FIRE") || cause.equals("FIRE_TICK"))
					magicValue -= getMagicReduction(ench.getName(), enchants.get(ench));
				continue;
			case "PROTECTION_EXPLOSIONS":
				// blast protection 
				if (cause.equals("ENTITY_EXPLOSION") || cause.equals("BLOCK_EXPLOSION"))
					magicValue -= getMagicReduction(ench.getName(), enchants.get(ench));
				continue;
			case "PROTECTION_PROJECTILE":
				// projectile protection against arrows, fireballs, snowballs, eggs, wither skulls, etc
				if (cause.equals("PROJECTILE"))
					magicValue -= getMagicReduction(ench.getName(), enchants.get(ench));
				continue;
			case "PROTECTION_FALL":
				// feather falling, protection against falling from height and ender pearls burning
				if (cause.equals("FALL"))
					magicValue -= getMagicReduction(ench.getName(), enchants.get(ench));
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
		if (armorValue != 0.00 || magicValue != 0.00 || cause.equals("FORCE"))
		{
			// equip suit now just if saving to avoid loss of equipped armor
			if (numPieces != 0)
			{
				// armor status already saved, avoid calculate twice
				// equip saved armor
				player.getInventory().setArmorContents(suit);
				getTempInventory(player.getName()).clear();
				player.updateInventory();
				
				// display equip message if worths
				if ((boolean)act.get("equipMsg"))
					player.sendMessage("\u00A7B"+ss.getConfig().getString("lang.equipArmor"));
			}
		}
			
		// equipped armor temporarily (update task) or permanently (cancel task)
		if ((int)act.get("equipFor") < 200)
			saveArmorLater(player, act);
		else if (tasks.containsKey(player.getName()))
		{
			if (tasks.get(player.getName()) != null)
				tasks.get(player.getName()).cancel();
			tasks.remove(player.getName());
			armorStatus.remove(player.getName());
		}
			
		// all done
		return reductions;
	}
	
	// GET FULL SUIT ENCHANTMENTS
	private Map<Enchantment, List<Integer>> getSuitEnchants(ItemStack[] suit)
	{
		// a map with multiple values, like multimap
		Map<Enchantment, List<Integer>> enchants = new HashMap<Enchantment, List<Integer>>();
		for (ItemStack piece: suit)
		{
			// prevent iteration exceptions
			if (piece != null)
			{
				for (Enchantment ench: piece.getEnchantments().keySet())
				{
					if (!enchants.containsKey(ench))
						enchants.put(ench, new ArrayList<Integer>());
					enchants.get(ench).add(piece.getEnchantmentLevel(ench));
				}
			}
		}
		return enchants;
	}
	
	// PRINT ARMOR STATUS ON PLAYER CHAT AREA
	private void printStatus(Player player, ItemStack[] suit)
	{
		// get saved old armor status if available
		if (!armorStatus.containsKey(player.getName()))
			armorStatus.put(player.getName(), new int[]{3, 3, 3, 3});
		int[] status = armorStatus.get(player.getName());
		
		// loop armor suit pieces
		int old;
		float dur;
		int sta;
		for (int slot = 0; slot < suit.length; ++slot)
		{
			// get piece durability
			ItemStack piece = suit[slot];
			old = status[slot];
			if (piece == null || piece.getType() == Material.AIR)
				dur = 0.0f;
			else
				dur = (float)(piece.getDurability()) / (float)(piece.getType().getMaxDurability());
			
			// status level
			sta = dur == 1.0 ? 0 : dur >= 0.8 ? 1 : dur >= 0.5 ? 2 : 3;
			
			// catch changes in armor piece durability
			if (old > sta)
			{
				// correct armor values if piece is destroyed
				if (sta == 0)
					suit[slot] = null;
				int armorPoints = (int)(getArmorReduction(suit) * -25);
				int col = armorPoints <= 4 ? 0 : armorPoints <= 10 ? 1 : 2;
				
				// send info to player
				player.sendMessage(
					colors[sta]+String.format(statusLines[sta], pieceNames[slot], pieceTitles[slot])+", "+
					colors[col]+String.format(ss.getConfig().getString("lang.armorStatus"), armorPoints)
				);
			}
			// save old level for later use
			status[slot] = sta;
		}
	}
	
	// CLEAR PLAYER DATA ON LOGOUT TO PREVENT MEMORY LEAKS
	protected void clearPlayer(String name)
	{
		armorStatus.remove(name);
		tasks.remove(name);
	}
}