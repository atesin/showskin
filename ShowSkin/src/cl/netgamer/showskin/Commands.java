package cl.netgamer.showskin;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor
{
	// PROPERTIES
	private SS ss;
	int equipFor;
	boolean equipMsg;
	
	// CONSTRUCTOR
	public Commands(SS ss)
	{
		this.ss = ss;
		equipFor = ss.getConfig().getConfigurationSection("onCommandSent").getInt("equipFor");
		equipMsg = ss.getConfig().getConfigurationSection("onCommandSent").getBoolean("equipMsg");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args)
	{
		// previous checks
		
		
		if (!(cmd.getName().equalsIgnoreCase("showskin")))
		{
			// not "showskin" command
			return true;
		}
		
		// NOT REALLY TRUE .. if issues "movechests"
		if (!(sender instanceof Player))
		{
			sender.sendMessage("§DMust be an online Player");
		}
		
		Player player = (Player) sender;
		if (player.getGameMode() == GameMode.CREATIVE)
			return true;
		
		// go commands
		
		// default behavior with no args
		if (args.length == 0)
		{
			ss.func.suitToggle(player, equipFor, equipMsg);
			return true;
		}
		
		
		
		
		
		return true;
	}
}
