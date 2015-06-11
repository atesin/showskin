package cl.netgamer.showskin;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class Events implements Listener
{
	// PROPERTIES
	private SS ss;
	
	// CONSTRUCTOR
	public Events(SS ss)
	{
		this.ss = ss;
		ss.getServer().getPluginManager().registerEvents(this, ss);
	}
	
	// LISTENERS
	
	// when player enters the server, load armor storing preferences
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		ss.f.loadConf(e.getPlayer());
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
		if (player.getGameMode() == GameMode.CREATIVE || !ss.f.hasStoredArmor(player.getName()))
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
				if (ss.f.armorLevel(e.getCurrentItem()) == 1)
					break;
			return;
		case "ARMOR":
			// check cursor item wear level
			if (ss.f.armorLevel(e.getCursor()) > 0)
				break;
		default:
			// you cant craft armor in player crafting inventory, doesnt fit
			return;
		}
		
		// revert equip attempt
		// delete meta
		e.setCancelled(true);
		if (ss.autoDress)
			ss.f.retrieveArmor(player);
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
		if (ss.f.armorLevel(e.getItem()) != 1)
			return;
		
		// is more complicated, if player is in front of a block/entity
		// where items can be placed, if have space, etc
		
		// player is not creative nor storing armor
		Player player = e.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE || !ss.f.hasStoredArmor(player.getName()))
			return;
		
		// all checks ok, deny action
		// delete meta
		e.setCancelled(true);
		if (ss.autoDress)
			ss.f.retrieveArmor(player);
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
		if (ss.f.armorLevel(e.getItem()) != 1)
			return;
		
		// find and watch nerby players
		Location loc = e.getBlock().getLocation();
		for (Player player: loc.getWorld().getPlayers())
		{
			if (loc.distanceSquared(player.getLocation()) <= 4)
				ss.f.checkPlayerArmor(player, e.getItem());
		}
	}
	
	// should recover armor on creative because weird inventory behavior
	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent e)
	{
		// only when player switching to creative mode storing armor
		if (e.getNewGameMode() == GameMode.CREATIVE && ss.f.hasStoredArmor(e.getPlayer().getName()))
			ss.f.retrieveArmor(e.getPlayer());
	}
}
