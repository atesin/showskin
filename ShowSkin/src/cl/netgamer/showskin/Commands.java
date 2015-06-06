package cl.netgamer.showskin;

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
		
		// go commands
		
		// default behavior with no args
		if (args.length == 0)
		{
			ss.f.toggleArmor(player);
			return true;
		}
		
		
		
		
		
		return true;
	}
}
