package cl.netgamer.showskin;

import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class Events implements Listener
{
	// PROPERTIES
	private SS ss;
	ConfigurationSection onArmorEquipped;
	ConfigurationSection onArmorDispensed;
	ConfigurationSection onCreativeMode;
	ConfigurationSection onDamageTaken;
	ConfigurationSection onPlayerDeath;
	
	// CONSTRUCTOR
	Events(SS ss)
	{
		this.ss = ss;
		ss.getServer().getPluginManager().registerEvents(this, ss);
		onArmorEquipped = ss.getConfig().getConfigurationSection("onArmorEquipped");
		onArmorDispensed = ss.getConfig().getConfigurationSection("onArmorDispensed");
		onCreativeMode = ss.getConfig().getConfigurationSection("onCreativeMode");
		onDamageTaken = ss.getConfig().getConfigurationSection("onDamageTaken");
		onPlayerDeath = ss.getConfig().getConfigurationSection("onPlayerDeath");
	}
	
	// LISTENERS
	
	// when player enters the server, load armor storing preferences
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		ss.func.playerLoadConf(e.getPlayer());
	}
	
	// there is no inventory events, so to detect armor equip must listen:
	// InventoryClickEvent, PlayerInteractEvent (right click held armor), BlockDispenseEvent
	
	// when some player change contents by clicking on some inventory slot, possible armor wearing
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e)
	{
		// just for players, players cant click on other players inventory
		if (!(e.getInventory().getHolder() instanceof Player))
			return;
		
		// creative has a weird inventory and no armor need
		// check if player is creative or has stored armor
		Player player = (Player) e.getInventory().getHolder();
		if (player.getGameMode() == GameMode.CREATIVE || ss.func.suitNumPieces(player.getName()) == 0)
			return;
		
		// bug: all player inventories are type CRAFTING
		// do whatever corresponds according slot type
		switch (e.getSlotType().toString())
		{
		case "CONTAINER":
		case "QUICKBAR":
			// player shift clicked an armor item (except pumpkin or mob head)
			if (e.getClick().isShiftClick())
				// check item wear level
				if (ss.func.getArmorLevel(e.getCurrentItem()) == 1)
					break;
			return;
		case "ARMOR":
			// check cursor item wear level
			if (ss.func.getArmorLevel(e.getCursor()) > 0)
				break;
		default:
			// you cant craft armor in player crafting inventory, doesnt fit
			return;
		}
		
		// already saving armor suit: revert equip attempt, do default actions
		e.setCancelled(true);
		ss.func.checkDamage(player, "NO_DAMAGE", "equip");
	}
	
	// when some player "uses" some held item, possible armor wearing
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		// player did not right click
		if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		// player is not holding an item
		if (!e.hasItem()) 
			return;
		
		// item is not wearable by shift click (armor)
		if (ss.func.getArmorLevel(e.getItem()) != 1)
			return;
		
		// is more complicated, if player is in front of a block/entity
		// where items can be placed, if have space, etc
		
		// player is not creative nor storing armor
		Player player = e.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE || ss.func.suitNumPieces(player.getName()) == 0)
			return;
		
		// already saving armor suit: revert equip attempt, do default actions
		e.setCancelled(true);
		ss.func.checkDamage(player, "NO_DAMAGE", "equip");
	}
	
	// when some dispenser fires some item, possible armor wearing
	@EventHandler
	public void onBlockDispense(BlockDispenseEvent e)
	{
		/*
		 * findings:
		 * player must be just next, on, or under dispenser
		 * player must have armor slot empty
		 * equiping lasts 1/2 sec aprox.
		 * dispenser does not dispense pumpkins or mob heads
		 */
		
		// is armor item?
		if (ss.func.getArmorLevel(e.getItem()) != 1)
			return;
		
		// find and watch nerby players
		Location loc = e.getBlock().getLocation();
		for (Player player: loc.getWorld().getPlayers())
		{
			if (loc.distanceSquared(player.getLocation()) <= 4)
				ss.func.armorCheck(player, e.getItem());
		}
	}
	
	// should recover armor on creative because weird inventory behavior
	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent e)
	{
		// only when player switching to creative mode storing armor
		if (e.getNewGameMode() == GameMode.CREATIVE && ss.func.suitNumPieces(e.getPlayer().getName()) != 0)
			ss.func.checkDamage(e.getPlayer(), "NO_DAMAGE", "creative");
	}
	
	// listen damage events before they occurs
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e)
	{
		// just watch player damages
		if (e.getEntityType() != EntityType.PLAYER)
			return;
		
		// debug
		printModifiers(e);
		
		// equip armor and get damage mofidifiers, if not equipped
		Map<DamageModifier, Double> modifiers = ss.func.checkDamage((Player)e.getEntity(), e.getCause().toString(), "damage");
		if (modifiers == null)
			return;
		
		// rewrite damage modifiers
		for (DamageModifier modifier: modifiers.keySet())
			e.setDamage(modifier, modifiers.get(modifier));
	}
	
	// debug util
	private void printModifiers(EntityDamageEvent e)
	{
		 // getDamage(DamageModifier type)
		 // DamageModifier.values();
		ss.log("Damage cause: "+e.getCause());
		double d;
		for (DamageModifier m: DamageModifier.values())
		{
			d = e.getDamage(m);
			if (d == 0)
				continue;
			ss.log("- "+m+": "+d);
		}
		ss.log("- Final damage: "+e.getFinalDamage());
	}	
}