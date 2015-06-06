package cl.netgamer.showskin;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
	
	// there is no inventory events, so to detect armor equip must listen
	// InventoryClickEvent, PlayerInteractEvent (right click held armor), BlockDispenseEvent
	
	// when some player change contents by clicking on some inventory slot, possible armor wearing
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e)
	{
		// just for players, players cant click on other players inventory
		if (!(e.getInventory().getHolder() instanceof Player))
			return;
		
		// creative has a weird inventory and no armor need
		Player player = (Player) e.getInventory().getHolder();
		if (player.getGameMode() == GameMode.CREATIVE)
			return;
		
		// check if player has stored armor
		if (!ss.f.areStoringArmor(player.getName()))
			return;
		
		// bug: all player inventories are type CRAFTING
		// do whatever corresponds according slot type
		switch (e.getSlotType().toString())
		{
		case "CONTAINER":
		case "QUICKBAR":
			// player shift clicked an armor item (except pumpkin or mob head)
			if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT)
				// check item wear level
				if (ss.f.wearLevel(e.getCurrentItem()) == 1)
					break;
			return;
		case "ARMOR":
			// check cursor item wear level
			if (ss.f.wearLevel(e.getCursor()) == 2)
				break;
		default:
			// you cant craft armor in player crafting inventory, doesnt fit
			return;
		}
		
		// revert equip attempt
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
		if (ss.f.wearLevel(e.getItem()) != 1)
			return;
		
		// is more complicated, if player is in front of a block/entity
		// where items can be placed, if have space, etc
		
		// player is not storing armor
		if (!ss.f.areStoringArmor(e.getPlayer().getName()))
			return;
		
		// all checks ok, deny action
		e.setCancelled(true);
		if (ss.autoDress)
			ss.f.retrieveArmor(e.getPlayer());
	}
	
	// when some dispenser fires some item, possible armor wearing
	@EventHandler
	public void onBlockDispense(BlockDispenseEvent e)
	{
		return;
	}
	
	// should recover armor on creative because weird inventory behavior
	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent e)
	{
		// only when player storing armor switching to creative mode
		if (e.getNewGameMode() != GameMode.CREATIVE || !ss.f.areStoringArmor(e.getPlayer().getName()))
			return;
		
		ss.f.retrieveArmor(e.getPlayer());
		return;
	}
}
