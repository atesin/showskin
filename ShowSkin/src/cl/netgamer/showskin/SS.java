package cl.netgamer.showskin;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import cl.netgamer.showskin.ConfigAccesor;
import cl.netgamer.showskin.Commands;

public class SS extends JavaPlugin
{
	// PROPERTIES
	private Logger logger;
	protected Location chests;
	protected boolean defaultUndress;
	protected boolean autoDress;
	protected boolean dressMessages;
	protected ConfigAccesor c;
	protected ConfigurationSection players;
	protected Functions f;
	
	// UTILITY
	protected void log(String msg)
	{
		logger.info(msg);
	}

	// ENABLER
	public void onEnable()
	{
		logger = getLogger();
		this.saveDefaultConfig();
		defaultUndress = getConfig().getBoolean("defaultUndress");
		autoDress = getConfig().getBoolean("autoDress");
		dressMessages = getConfig().getBoolean("dressMessages");
		chests = new Location
		(
			this.getServer().getWorld(getConfig().getConfigurationSection("chestsLocation").getString("world")),
			getConfig().getConfigurationSection("chestsLocation").getDouble("x"),
			getConfig().getConfigurationSection("chestsLocation").getDouble("y"),
			getConfig().getConfigurationSection("chestsLocation").getDouble("z")
		);
		
		// create a star hierarchy where plugin is the center
		c = new ConfigAccesor(this, "data.yml");
		c.saveDefaultConfig();
		f = new Functions(this);
		new Events(this);
		//new Commands(this);
		getCommand("showskin").setExecutor(new Commands(this));
	}
}
