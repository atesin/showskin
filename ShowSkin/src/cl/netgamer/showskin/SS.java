package cl.netgamer.showskin;

import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import cl.netgamer.showskin.ConfigAccesor;
import cl.netgamer.showskin.Commands;

public class SS extends JavaPlugin
{
	// PROPERTIES
	private Logger logger = getLogger();
	protected ConfigAccesor data;
	protected ConfigurationSection actions;
	protected Functions func;
	
	// DEBUG UTILITY
	protected void log(String msg)
	{
		logger.info(msg);
	}

	// ENABLER
	public void onEnable()
	{
		// create a star hierarchy where plugin is the center
		this.saveDefaultConfig();
		data = new ConfigAccesor(this, "data.yml");
		data.saveDefaultConfig();
		func = new Functions(this);
		new Events(this);
		getCommand("showskin").setExecutor(new Commands(this));
	}
}
