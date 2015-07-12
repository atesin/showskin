package cl.netgamer.showskin;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor
{
	// PROPERTIES
	private SS ss;
	
	// CONSTRUCTOR
	public Commands(SS ss)
	{
		this.ss = ss;
	}
	
	// ugly for now but works efficiently
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args)
	{
		// previous checks
		
		// another command
		if (!(cmd.getName().equalsIgnoreCase("showskin")))
			return true;
		
		// no online player that need to show his skin
		if (!(sender instanceof Player))
			sender.sendMessage("§DMust be an online Player");
		
		// creative mode is blocked to avoid different shape inventory bugs, no needed anyway
		Player player = (Player) sender;
		if (player.getGameMode() == GameMode.CREATIVE)
			return true;
		
		// go commands
		
		// default behavior with no args
		if (args.length == 0)
			ss.func.toggleArmor(player, "command");

		return true;
	}
}
