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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Events implements Listener
{
	// PROPERTIES
	private SS ss;
	
	// CONSTRUCTOR
	Events(SS ss)
	{
		ss.getServer().getPluginManager().registerEvents(this, ss);
		this.ss = ss;
	}
	
	// LISTENERS
	
	// when player enters the server, load armor storing preferences
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		ss.func.loadPlayerData(e.getPlayer());
	}
	
	// there is no inventory events, so to detect armor equip must listen:
	// InventoryClickEvent, PlayerInteractEvent (right click held armor), BlockDispenseEvent
	
	// WHEN A PLAYER MODIFY HIS INVENTORY BY CLICKING IT, POSSIBLE ARMOR EQUIP ATTEMPT
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
				if (ss.func.getItemWearability(e.getCurrentItem()) == 1)
					break;
			return;
		case "ARMOR":
			// check cursor item wear level
			if (ss.func.getItemWearability(e.getCursor()) > 0)
				break;
		default:
			// you cant craft armor in player crafting inventory, doesnt fit
			return;
		}
		
		// already saving armor suit: revert equip attempt, do default actions
		e.setCancelled(true);
		player.updateInventory();
		ss.func.checkEquipArmor(player, "FORCE", "equip");
	}
	
	// WHEN A PLAYER "USES" SOME HELD ITEM, POSSIBLE ARMOR WEARING ATTEMPT
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		// continue event if: no right click
		if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		// not holding an item
		if (!e.hasItem()) 
			return;
		
		// item is not wearable by shift click (is not an armor)
		if (ss.func.getItemWearability(e.getItem()) != 1)
			return;
		
		// pending: if player is in front of a block/entity that can be right clicked
		
		// is in creative mode
		Player player = e.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE)
			return;
		
		// has no saved armor
		if (ss.func.suitNumPieces(player.getName()) == 0)
			return;
		
		// revert equip attempt, reflect inventory no-changes, do default actions
		e.setCancelled(true);
		player.updateInventory();
		ss.func.checkEquipArmor(player, "FORCE", "equip");
	}
	
	// WHEN SOME DISPENSER FIRES SOME ITEM, POSSIBLE ARMOR EQUIP ATTEMPT WITH A DISPENSER
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
		if (ss.func.getItemWearability(e.getItem()) != 1)
			return;
		
		// find and watch nerby players
		Location loc = e.getBlock().getLocation();
		for (Player player: loc.getWorld().getPlayers())
		{
			if (loc.distanceSquared(player.getLocation()) <= 4)
				ss.func.armorWatch(player, e.getItem());
		}
	}
	
	// BLOCK PLUGIN ON CREATIVE MODE TO PREVENT INVENTORY PROBLEMS (NO NEEDED ON CREATIVE ANYWAY)
	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent e)
	{
		// only when player switching to creative mode storing armor
		if (e.getNewGameMode() == GameMode.CREATIVE)
			ss.func.checkEquipArmor(e.getPlayer(), "FORCE", "force");
	}
	
	// WHEN PLAYER DIES EQUIP ARMOR IMMEDIATELY DO DROP IT (thanks to Kaezoncito for his report)
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e)
	{
		ss.func.checkEquipArmor(e.getEntity(), "FORCE", "force");
	}
	
	// WATCH DAMAGE EVENTS BEFORE THEY OCCURS
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e)
	{
		// just watch player damages
		if (e.getEntityType() != EntityType.PLAYER)
			return;
		
		// equip armor and get damage mofidifiers, if not equipped
		Map<DamageModifier, Double> modifiers = ss.func.checkEquipArmor((Player)e.getEntity(), e.getCause().toString(), "damage");
		
		// rewrite damage modifiers
		for (DamageModifier modifier: modifiers.keySet())
			e.setDamage(modifier, e.getDamage()*modifiers.get(modifier));
	}
	
	// CLEAR PLAYER DATA ON LOGOUT TO PREVENT MEMORY LEAKS
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		ss.func.clearPlayer(e.getPlayer().getName());
	}
	
	// ARMOR REDUCTIONS DEBUG UTILITY
	/* private void printModifiers(EntityDamageEvent e)
	{
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
	} */
}