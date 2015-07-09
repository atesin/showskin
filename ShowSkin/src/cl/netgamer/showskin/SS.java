package cl.netgamer.showskin;

import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import cl.netgamer.showskin.ConfigAccesor;
import cl.netgamer.showskin.Commands;

public class SS extends JavaPlugin
{
	// PROPERTIES
	private Logger logger;
	protected ConfigAccesor data;
	protected ConfigurationSection actions;
	protected Map<String, Object> lang;
	protected Functions func;
	
	// DEBUG UTILITY
	protected void log(String msg)
	{
		logger.info(msg);
	}

	// ENABLER
	public void onEnable()
	{
		logger = getLogger();
		this.saveDefaultConfig();
		lang = getConfig().getConfigurationSection("lang").getValues(false);
		// §B=cyan, §D=magenta, §E=yellow
		
		// create a star hierarchy where plugin is the center
		data = new ConfigAccesor(this, "data.yml");
		data.saveDefaultConfig();
		func = new Functions(this);
		new Events(this);
		getCommand("showskin").setExecutor(new Commands(this));
	}
}
