package cl.netgamer.showskin;

import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import cl.netgamer.showskin.ConfigAccesor;
import cl.netgamer.showskin.Commands;

public class SS extends JavaPlugin
{
	// PROPERTIES
	private Logger logger;
	protected boolean autoDress;
	protected ConfigAccesor conf;
	protected ConfigurationSection lang;
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
		autoDress = getConfig().getBoolean("autoDress");
		lang = getConfig().getConfigurationSection("lang");
		// §B=cyan, §D=magenta, §E=yellow
		
		// create a star hierarchy where plugin is the center
		conf = new ConfigAccesor(this, "data.yml");
		conf.saveDefaultConfig();
		func = new Functions(this);
		new Events(this);
		getCommand("showskin").setExecutor(new Commands(this));
	}
}
