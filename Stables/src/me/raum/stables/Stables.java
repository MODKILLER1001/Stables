package me.raum.stables;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Boat;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Golem;
import org.bukkit.entity.Horse;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Squid;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Weather;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Listener;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class Stables
  extends JavaPlugin
  implements Listener
{
  Connection conn;
  static ArrayList<String> randomNames = new ArrayList();
  private FileConfiguration localConfig = null;
  private File localConfigFile = null;
  public File randomNameFile;
  public YamlConfiguration randomNameConfig;
  public static Economy economy = null;
  static boolean setup;
  static boolean outOfDate = false;
  static String currentVersion = "";
  public static Stables plugin;
  ResultSet rs;
  private WorldGuardPlugin worldguard;
  
  public void convertConfig()
  {
    if (getConfig().contains("randomNames"))
    {
      this.randomNameConfig.set("randomNames", getConfig().getStringList("randomNames"));
      getConfig().set("randomNames", null);
    }
  }
  
  public void msg(CommandSender s, String msg, String replace)
  {
    msg = msg.toString().replaceAll("%1", replace);
    s.sendMessage(msg.toString().replaceAll("&([0-9A-Fa-f])", "§$1"));
  }
  
  public void msg(CommandSender s, String msg)
  {
    s.sendMessage(msg.toString().replaceAll("&([0-9A-Fa-f])", "§$1"));
  }
  
  public void onEnable()
  {
    plugin = this;
    setup = false;
    getServer().getPluginManager().registerEvents(new EventListener(), this);
    this.randomNameFile = new File(getDataFolder(), "randomNames.yml");
    this.randomNameConfig = new YamlConfiguration();
    
    LoadConfiguration();
    
    SetupRecipes();
    if (!setup) {
      OpenDatabase();
    }
    setupEconomy();
    updateCheck();
    checkWorldGuard();
    if (outOfDate)
    {
      getServer().getLogger().info("Stables is currently out of date! You are using version " + plugin.getDescription().getVersion() + " - the newst version is " + currentVersion);
      getServer().getLogger().info("You can download the newest version at http://dev.bukkit.org/bukkit-plugins/stables/files/");
    }
  }
  
  public void onDisable()
  {
    CloseDatabase();
  }
  
  public void setConfig(String line, Object set)
  {
    if (!getConfig().contains(line)) {
      getConfig().set(line, set);
    }
  }
  
  public void setLang(String line, String local)
  {
    if (!getlocalConfig().contains(line)) {
      getlocalConfig().set(line, local);
    }
  }
  
  public void LoadConfiguration()
  {
    convertConfig();
    randomNames.clear();
    
    setConfig("general.Debug", Boolean.valueOf(false));
    setConfig("general.BlockAll", Boolean.valueOf(false));
    setConfig("general.AllowPVPMountedDamage", Boolean.valueOf(true));
    setConfig("general.PVPDamage", Boolean.valueOf(true));
    setConfig("general.EnviromentDamage", Boolean.valueOf(true));
    setConfig("general.OwnerDamage", Boolean.valueOf(false));
    setConfig("general.MobDamage", Boolean.valueOf(false));
    setConfig("general.Theft", Boolean.valueOf(false));
    setConfig("general.CheckUpdates", Boolean.valueOf(true));
    
    setConfig("general.MaxOwned.default", Integer.valueOf(10));
    setConfig("general.ProtectUnclaimed", Boolean.valueOf(false));
    setConfig("general.Save", Integer.valueOf(10));
    setConfig("general.showUnownedWarning", Boolean.valueOf(true));
    
    setConfig("MySQL.useSQLite", Boolean.valueOf(true));
    setConfig("MySQL.useMySQL", Boolean.valueOf(false));
    setConfig("MySQL.database", "YourDBName");
    setConfig("MySQL.host", "localhost");
    setConfig("MySQL.user", "root");
    setConfig("MySQL.password", "abcd1234");
    setConfig("MySQL.port", Integer.valueOf(0));
    setConfig("MySQL.prefix", "stables_");
    
    setConfig("horses.tame.AllowMaxNamed", Boolean.valueOf(false));
    setConfig("horses.allowFind", Boolean.valueOf(false));
    setConfig("horses.allowTP", Boolean.valueOf(false));
    setConfig("horses.allowSummon", Boolean.valueOf(false));
    
    setConfig("horses.AutoOwn", Boolean.valueOf(false));
    setConfig("horses.AutoSaddle", Boolean.valueOf(false));
    setConfig("horses.NoTagRename", Boolean.valueOf(false));
    
    setConfig("horses.spawn.health.max", Integer.valueOf(30));
    setConfig("horses.spawn.health.min", Integer.valueOf(15));
    

    setConfig("horses.lure.allow", Boolean.valueOf(true));
    setConfig("horses.lure.delay", Integer.valueOf(10));
    setConfig("horses.lure.disabled", "world_nether, world_the_end");
    setConfig("horses.lure.enabled", "world");
    setConfig("horses.lure.useEnabled", Boolean.valueOf(false));
    
    setConfig("lure.396.chance", Integer.valueOf(50));
    setConfig("lure.396.type", Integer.valueOf(1));
    setConfig("lure.396.health.max", Integer.valueOf(30));
    setConfig("lure.396.health.min", Integer.valueOf(15));
    







    setConfig("stable.cost", Integer.valueOf(0));
    setConfig("stable.useCommand", Boolean.valueOf(false));
    setConfig("stable.timeout", Integer.valueOf(10));
    setConfig("stable.disabled", "world_nether, world_the_end");
    setConfig("stable.useEnabled", Boolean.valueOf(false));
    setConfig("stable.enabled", "world");
    
    setConfig("recipe.usePerms", Boolean.valueOf(false));
    setConfig("recipe.hardMode", Boolean.valueOf(false));
    setConfig("recipe.saddle", Boolean.valueOf(true));
    setConfig("recipe.nametag", Boolean.valueOf(true));
    setConfig("recipe.armor.iron", Boolean.valueOf(true));
    setConfig("recipe.armor.gold", Boolean.valueOf(true));
    setConfig("recipe.armor.diamond", Boolean.valueOf(true));
    
    addRandomNames();
    saveConfig();
    getlocalConfig();
    setupLanguage();
  }
  
  public void reloadlocalConfig()
  {
    if (this.localConfigFile == null) {
      this.localConfigFile = new File(getDataFolder(), "language.yml");
    }
    this.localConfig = YamlConfiguration.loadConfiguration(this.localConfigFile);
  }
  
  public FileConfiguration getlocalConfig()
  {
    if (this.localConfig == null) {
      reloadlocalConfig();
    }
    return this.localConfig;
  }
  
  public void savelocalConfig()
  {
    if ((this.localConfig == null) || (this.localConfigFile == null)) {
      return;
    }
    try
    {
      getlocalConfig().save(this.localConfigFile);
    }
    catch (IOException ex)
    {
      error("Could not save config file to " + this.localConfigFile);
    }
  }
  
  public String getLang(String phrase, Object var)
  {
    if (getlocalConfig().contains(phrase))
    {
      String msg = getlocalConfig().getString(phrase);
      if (var != null) {
        msg = msg.replaceAll("%1", var.toString());
        
      }
      return msg;
    }
    return "Localization Error: " + phrase + " missing.";
  }
  
  public void SetupRecipes()
  {
    if (getConfig().getBoolean("recipe.saddle"))
    {
      ShapedRecipe r = new ShapedRecipe(new ItemStack(Material.SADDLE));
      r.shape(new String[] {
        "LLL", "LIL", "I I" });
      
      r.setIngredient('L', Material.LEATHER);
      r.setIngredient('I', Material.IRON_INGOT);
      getServer().addRecipe(r);
      getServer().getLogger().info(getLang("RECIPE_ADDED", null) + " " + r.getResult().getType());
    }
    if (getConfig().getBoolean("recipe.nametag"))
    {
      ShapedRecipe r = new ShapedRecipe(new ItemStack(Material.NAME_TAG));
      r.shape(new String[] {
        "  S", " P ", "P  " });
      
      r.setIngredient('S', Material.STRING);
      r.setIngredient('P', Material.PAPER);
      getServer().addRecipe(r);
      getServer().getLogger().info(getLang("RECIPE_ADDED", null) + " " + r.getResult().getType());
    }
    if (getConfig().getBoolean("recipe.armor.iron"))
    {
      ShapedRecipe r = new ShapedRecipe(new ItemStack(Material.IRON_BARDING));
      r.shape(new String[] {
        "  I", "ILI", "III" });
      
      r.setIngredient('L', Material.WOOL, -1);
      if (getConfig().getBoolean("recipe.hardMode")) {
        r.setIngredient('I', Material.IRON_BLOCK);
      } else {
        r.setIngredient('I', Material.IRON_INGOT);
      }
      getServer().addRecipe(r);
      getServer().getLogger().info(getLang("RECIPE_ADDED", null) + " " + r.getResult().getType());
    }
    if (getConfig().getBoolean("recipe.armor.gold"))
    {
      ShapedRecipe r = new ShapedRecipe(new ItemStack(Material.GOLD_BARDING));
      r.shape(new String[] {
        "  I", "ILI", "III" });
      

      r.setIngredient('L', Material.WOOL, -1);
      if (getConfig().getBoolean("recipe.hardMode")) {
        r.setIngredient('I', Material.GOLD_BLOCK);
      } else {
        r.setIngredient('I', Material.GOLD_INGOT);
      }
      getServer().addRecipe(r);
      getServer().getLogger().info(getLang("RECIPE_ADDED", null) + " " + r.getResult().getType());
    }
    if (getConfig().getBoolean("recipe.armor.diamond"))
    {
      ShapedRecipe r = new ShapedRecipe(new ItemStack(Material.DIAMOND_BARDING));
      r.shape(new String[] {
        "  I", "ILI", "III" });
      


      r.setIngredient('L', Material.WOOL, -1);
      if (getConfig().getBoolean("recipe.hardMode")) {
        r.setIngredient('I', Material.DIAMOND_BLOCK);
      } else {
        r.setIngredient('I', Material.DIAMOND);
      }
      getServer().addRecipe(r);
      getServer().getLogger().info(getLang("RECIPE_ADDED", null) + " " + r.getResult().getType());
    }
  }
  
  private void checkWorldGuard()
  {
    Plugin plug = getServer().getPluginManager().getPlugin("WorldGuard");
    if ((plug == null) || (!(plug instanceof WorldGuardPlugin))) {
      this.worldguard = null;
    }
    this.worldguard = ((WorldGuardPlugin)plug);
  }
  
  private void setupEconomy()
  {
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
      return;
    }
    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
      return;
    }
    economy = (Economy)rsp.getProvider();
  }
  
  public String HorseName(String id, LivingEntity horse)
  {
    if (id == null) {
      id = horse.getUniqueId().toString();
    }
    String name = null;
    

    this.rs = queryDB("SELECT named FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE uid='" + id + "'");
    if (this.rs != null) {
      name = getResultString(1);
    }
    if (name == null)
    {
      if ((horse != null) && (horse.getCustomName() != null))
      {
        debug("Not found in database " + id);
        return horse.getCustomName();
      }
      return "Unknown";
    }
    return name.replace("`", "'");
  }
  
  public String HorseOwner(String id)
  {
    this.rs = queryDB("SELECT owneruuid FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE uid='" + id + "'");
    if (this.rs == null) {
      return null;
    }
    String owner = getResultString(1);
    
    return owner;
  }
  
  public String HorseOwnerName(String id)
  {
    this.rs = queryDB("SELECT owner FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE uid='" + id + "'");
    if (this.rs == null) {
      return null;
    }
    String owner = getResultString(1);
    
    return owner;
  }
  
  public void syntax(CommandSender s, String str)
  {
    msg(s, getLang("SYNTAX", str));
  }
  
  public void error(Object msg)
  {
    Bukkit.getServer().getLogger().severe("Stables: " + msg.toString());
  }
  
  void local(CommandSender s, String str)
  {
    msg(s, getLang(str, null));
  }
  
  public void debug(Object msg)
  {
    if (plugin.getConfig().getBoolean("general.Debug")) {
      Bukkit.getServer().getLogger().info("Stables DEBUG: " + msg.toString());
    }
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
  {
    if (cmd.getName().equalsIgnoreCase("view"))
    {
      if (!(sender instanceof Player))
      {
        local(sender, "NO_CONSOLE");
        return true;
      }
      Player p = (Player)sender;
      viewStables(p);
      return true;
    }
    if (cmd.getName().equalsIgnoreCase("recover"))
    {
      if (!(sender instanceof Player))
      {
        local(sender, "NO_CONSOLE");
        return true;
      }
      Player p = (Player)sender;
      recoverStables(p, args, 0);
      return true;
    }
    if (cmd.getName().equalsIgnoreCase("rename"))
    {
      if (args.length < 1)
      {
        syntax(sender, "/stables rename (horsename)");
        return true;
      }
      if (!(sender instanceof Player))
      {
        local(sender, "NO_CONSOLE");
        return true;
      }
      Player p = (Player)sender;
      if (!getConfig().getBoolean("horses.NoTagRename"))
      {
        local(sender, "COMMAND_DISABLED");
        return true;
      }
      String uid = findHorse(args, 0, p.getUniqueId().toString());
      if (uid == null)
      {
        local(sender, "HORSE_UNKNOWN");
        return true;
      }
      debug("Horse found.");
      
      List<Entity> entitylist = p.getNearbyEntities(20.0D, 20.0D, 20.0D);
      for (int t = 0; t < entitylist.size(); t++) {
        if (((Entity)entitylist.get(t)).getUniqueId().toString().equals(uid))
        {
          String newName = getRandomName();
          LivingEntity l = (LivingEntity)entitylist.get(t);
          l.setCustomName(newName);
          nameHorse(uid, newName);
          
          local(sender, "NEW_NAME");
          return true;
        }
      }
      local(sender, "RENAME_NOT_FOUND");
      return true;
    }
    if (cmd.getName().equalsIgnoreCase("ro"))
    {
      if (!(sender instanceof Player))
      {
        local(sender, "NO_CONSOLE");
        return true;
      }
      Player p = (Player)sender;
      if (!perm(p, "stables.remove"))
      {
        local(sender, "NO_PERM");
        return true;
      }
      p.setMetadata("stables.removeowner", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
      local(p, "HIT_REMOVE");
      return true;
    }
    if (cmd.getName().equalsIgnoreCase("spawnhorse"))
    {
      if (!(sender instanceof Player))
      {
        local(sender, "NO_CONSOLE");
        return true;
      }
      Player p = (Player)sender;
      if (!perm(p, "stables.spawn"))
      {
        local(sender, "NO_PERM");
        return true;
      }
      if (args.length == 0)
      {
        spawnHorse(p.getLocation(), false, false);
        p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Random horse spawned!");
        return true;
      }
      if (args.length == 1)
      {
        if (args[0].equals("zombie"))
        {
          p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Zombie horse spawned.");
          spawnHorse(p.getLocation(), true, false);
          return true;
        }
        if (args[0].equals("skeleton"))
        {
          p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Skeleton horse spawned.");
          spawnHorse(p.getLocation(), false, true);
          return true;
        }
        return false;
      }
      return false;
    }
    if (cmd.getName().equalsIgnoreCase("stables"))
    {
      if ((args.length == 0) || ((args.length >= 1) && (args[0].equalsIgnoreCase("help"))))
      {
        msg(sender,"===============" + ChatColor.GREEN + " [" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "===============" );
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "AddRider - " + ChatColor.RESET + getLang("CMD_ADD", null));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "DelRider - " + ChatColor.RESET + getLang("CMD_DEL", null));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "List - " + ChatColor.RESET + getLang("CMD_LIST", null));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Abandon - " + ChatColor.RESET + getLang("CMD_ABANDON", null));
        if (getConfig().getBoolean("stable.useCommand"))
        {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "View - " + ChatColor.RESET + getLang("CMD_VIEW", null));
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Store - " + ChatColor.RESET + getLang("CMD_STORE", null));
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Recover - " + ChatColor.RESET + getLang("CMD_RECOVER", null));
        }
        if (getConfig().getBoolean("horses.NoTagRename")) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Rename - " + ChatColor.RESET + getLang("CMD_RENAME", null));
        }
        if ((perm((Player)sender, "stables.find")) || (getConfig().getBoolean("horses.allowFind"))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Find - " + ChatColor.RESET + getLang("CMD_FIND", null));
        }
        if ((perm((Player)sender, "stables.tp")) || (getConfig().getBoolean("horses.allowTP"))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "TP - " + ChatColor.RESET + getLang("CMD_TP", null));
        }
        if ((perm((Player)sender, "stables.summon")) || (getConfig().getBoolean("horses.allowSummon"))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Summon - " + ChatColor.RESET + getLang("CMD_SUMMON", null));
        }
        if ((!(sender instanceof Player)) || (((sender instanceof Player)) && (perm((Player)sender, "stables.info")))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "check - " + ChatColor.RESET + getLang("CMD_CHECK", null));
        }
        if ((!(sender instanceof Player)) || (((sender instanceof Player)) && (perm((Player)sender, "stables.remove")))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "removeowner - " + ChatColor.RESET + getLang("CMD_RO", null));
        }
        if ((!(sender instanceof Player)) || (((sender instanceof Player)) && (perm((Player)sender, "stables.name")))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "name - " + ChatColor.RESET + getLang("CMD_NAME", null));
        }
        if ((!(sender instanceof Player)) || (((sender instanceof Player)) && (perm((Player)sender, "stables.list")))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "listall - " + ChatColor.RESET + getLang("CMD_LISTALL", null));
        }
        if ((!(sender instanceof Player)) || (((sender instanceof Player)) && (perm((Player)sender, "stables.clear")))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "clearhorses - " + ChatColor.RESET + getLang("CMD_CLEAR", null));
        }
        if ((!(sender instanceof Player)) || (((sender instanceof Player)) && (perm((Player)sender, "stables.admin")))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "reload - " + ChatColor.RESET + getLang("CMD_RELOAD", null));
        }
        if ((!(sender instanceof Player)) || (((sender instanceof Player)) && (perm((Player)sender, "stables.admin")))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "save - " + ChatColor.RESET + getLang("CMD_SAVE", null));
        }
        if ((!(sender instanceof Player)) || (((sender instanceof Player)) && (perm((Player)sender, "stables.admin")))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "config - " + ChatColor.RESET + getLang("CMD_CONFIG", null));
        }
        if ((!(sender instanceof Player)) || (((sender instanceof Player)) && (perm((Player)sender, "stables.admin")))) {
          msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "convert - " + ChatColor.RESET + getLang("CMD_CONVERT", null));
        }
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("version")))
      {
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "Stables, by raum266, modified by externo6 - version " + plugin.getDescription().getVersion());
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("report")))
      {
        msg(sender, "===============" + ChatColor.GREEN + " [" + ChatColor.DARK_GREEN + "Stables Current Config" + ChatColor.GREEN + "] " + ChatColor.WHITE + "===============");
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Debug Mode: " + ChatColor.RESET  + getConfig().getBoolean("general.Debug"));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Block All: " + ChatColor.RESET  + getConfig().getBoolean("general.BlockAll"));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Block PVP: " + ChatColor.RESET  + getConfig().getBoolean("general.PVPDamage"));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Mounted PVP Damage: " + ChatColor.RESET  + getConfig().getBoolean("general.AllowPVPMountedDamage"));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Block Environment: " + ChatColor.RESET  + getConfig().getBoolean("general.EnviromentDamage"));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Block Owner: " + ChatColor.RESET  + getConfig().getBoolean("general.OwnerDamage"));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Block Mob: " + ChatColor.RESET  + getConfig().getBoolean("general.MobDamage"));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Allow Theft: " + ChatColor.RESET  + getConfig().getBoolean("general.Theft"));
        msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Save Time: " + ChatColor.RESET  + getConfig().getInt("general.Save"));
        return true;
      }
      if ((args.length >= 1) && ((args[0].equalsIgnoreCase("check")) || (args[0].equalsIgnoreCase("info"))))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        if (!perm(p, "stables.info"))
        {
          local(sender, "NO_PERM");
          return true;
        }
        p.setMetadata("stables.checkinfo", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
        local(p, "CHECK_HIT");
        return true;
      }
      if ((args.length >= 1) && (args[0].equals("name")))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        if (!perm(p, "stables.name"))
        {
          local(sender, "NO_PERM");
          return true;
        }
        if (args.length < 4)
        {
          syntax(p, "/stables name (exact player name) \"(old name)\" \"(new name)\" - The quotes are required!");
          return true;
        }
        String pname = args[1];
        int Start = 2;
        String a = "";
        while (Start < args.length)
        {
          a = a + " " + args[Start];
          Start++;
        }
        a = a.trim();
        String[] names = a.split("\"");
        if (names.length != 4)
        {
          syntax(p, "/stables name (exact player name) \"(old name)\" \"(new name)\" - The quotes are required!");
          return true;
        }
        String[] oldname = names[1].split(" ");
        String newname = names[3];
        String owneruuid = null;
        

        UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(new String[] { pname }));
        Map<String, UUID> response = null;
        try
        {
          response = fetcher.call();
          

          owneruuid = ((UUID)response.get(pname)).toString();
        }
        catch (Exception e)
        {
          getLogger().warning("Exception while running UUIDFetcher");
          msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "Unknown player.");
          return true;
        }
        String uid = findHorse(oldname, 0, owneruuid);
        if (uid == null)
        {
          local(sender, "HORSE_UNKNOWN");
          return true;
        }
        List<Entity> entitylist = p.getNearbyEntities(20.0D, 20.0D, 20.0D);
        for (int t = 0; t < entitylist.size(); t++) {
          if (((Entity)entitylist.get(t)).getUniqueId().toString().equals(uid))
          {
            LivingEntity l = (LivingEntity)entitylist.get(t);
            l.setCustomName(newname);
            nameHorse(uid, newname);
            local(sender, "NEW_NAME");
            return true;
          }
        }
        local(sender, "RENAME_NOT_FOUND");
        
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("list")))
      {
        String owner = sender.getName();
        msg(sender, "======" + ChatColor.GREEN + " [" + ChatColor.DARK_GREEN + owner + " Owns the Following Horses" + ChatColor.GREEN + "] " + ChatColor.WHITE + "======");
        this.rs = queryDB("SELECT uid, tamed, named, x, y, z FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE owner='" + owner + "'");
        try
        {
          while (this.rs.next()) {
            msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Name: " + ChatColor.RESET + this.rs.getString(3).replace("`", "'"));
          }
        }
        catch (SQLException e)
        {
          error("SQL Error - List");
          debug(e.getStackTrace());
        }
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("abandon")))
      {
        if (args.length < 2)
        {
          msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "/stables abandon (horsename)");
          return true; 
        }
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        
        String uid = findHorse(args, 1, p.getUniqueId().toString());
        if (uid == null)
        {
          local(sender, "HORSE_UNKNOWN");
          return true;
        }
        List<Entity> entitylist = p.getLocation().getWorld().getEntities();
        for (int t = 0; t < entitylist.size(); t++) {
          if (((Entity)entitylist.get(t)).getUniqueId().toString().equals(uid))
          {
            LivingEntity l = (LivingEntity)entitylist.get(t);
            l.setCustomName(null);
            removeHorse(uid);
            local(sender, "HORSE_ABANDON");
            return true;
          }
        }
        removeHorse(uid);
        
        local(sender, "HORSE_ABANDON_NOT_FOUND");
        return true;
      }
      if ((args.length >= 1) && ((args[0].equalsIgnoreCase("teleport")) || (args[0].equalsIgnoreCase("tp"))))
      {
        if (args.length < 2)
        {
          syntax(sender, "/stables tp (horsename)");
          return true;
        }
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        if ((!getConfig().getBoolean("horses.allowTP")) && (!perm(p, "stables.tp")))
        {
          local(sender,"COMMAND_DISABLED");
          return true;
        }
        String uid = findHorse(args, 1, p.getUniqueId().toString());
        if (uid == null)
        {
          local(sender, "HORSE_UNKNOWN");
          return true;
        }
        debug("Horse found - checking for location ...");
        Location loc = getHorseLocation(uid);
        if (loc == null)
        {
          local(sender, "HORSE_NOT_FOUND");
          return true;
        }
        p.teleport(loc);
        
        local(sender, "TP_FOUND");
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("summon")))
      {
        if (args.length < 2)
        {
          syntax(sender, "/stables summon (horsename)");
          return true;
        }
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        if ((!getConfig().getBoolean("horses.allowSummon")) && (!perm(p, "stables.summon")))
        {
          local(sender, "COMMAND_DISABLED");
          return true;
        }
        String uid = findHorse(args, 1, p.getUniqueId().toString());
        if (uid == null)
        {
          local(sender, "HORSE_UNKNOWN");
          return true;
        }
        debug("Horse found - searching for location ...");
        
        List<Entity> entitylist = p.getLocation().getWorld().getEntities();
        for (int t = 0; t < entitylist.size(); t++) {
          if (((Entity)entitylist.get(t)).getUniqueId().toString().equals(uid))
          {
            ((Entity)entitylist.get(t)).teleport(p.getLocation());
            
            local(sender, "SUMMON_HORSE");
            saveLocation((Horse)entitylist.get(t));
            return true;
          }
        }
        local(sender, "HORSE_NOT_FOUND");
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("dismount")))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        if (!perm((Player)sender, "stables.dismount"))
        {
          local(sender, "NO_PERM");
          return true;
        }
        if (args.length != 2)
        {
          syntax(sender, "/stables dismount (player)");
          return true;
        }
        Player target = Bukkit.getServer().getPlayer(args[1]);
        if (target == null)
        {
          msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  args[0].toString() + " is not here!");
          return true;
        }
        if (target.getVehicle() != null)
        {
          debug("Removing player from vehicle");
          
          target.getVehicle().getPassenger().leaveVehicle();
        }
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "Done!");
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("find")))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        if ((!getConfig().getBoolean("horses.allowFind")) && (!perm((Player)sender, "stables.find")))
        {
          local(sender, "COMMAND_DISABLED");
          return true;
        }
        if (args.length < 2)
        {
          syntax(sender, "/stables find (horsename)");
          return true;
        }
        String uid = findHorse(args, 1, p.getUniqueId().toString());
        if (uid == null)
        {
          local(sender, "HORSE_UNKNOWN");
          return true;
        }
        debug("Horse found - checking for location ...");
        Location loc = getHorseLocation(uid);
        if (loc == null)
        {
          local(sender, "HORSE_NOT_FOUND");
          return true;
        }
        if (loc.getWorld() != p.getLocation().getWorld())
        {
          local(sender, "HORSE_WRONG_WORLD");
          return true;
        }
        p.setCompassTarget(loc);
        local(sender, "COMPASS_LOCKED");
        
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("listall")))
      {
        if (((sender instanceof Player)) && (!perm((Player)sender, "stables.info")))
        {
          local(sender, "NO_PERM");
          return true;
        }
        if (args.length == 1)
        {
          local(sender, "LIST_NOARG");
          return true;
        }
        String owner;
        
        if (getServer().getPlayerExact(args[1]) == null)
        {
          OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
          if (!player.hasPlayedBefore())
          {
            local(sender, "UNKNOWN_OWNER");
            return true;
          }
          owner = player.getName();
        }
        else
        {
          owner = getServer().getPlayerExact(args[1].toString()).getName();
        }
        msg(sender, "======" + ChatColor.GREEN + " [" + ChatColor.DARK_GREEN + owner + " Owns the Following Horses" + ChatColor.GREEN + "] " + ChatColor.WHITE + "======");
        this.rs = queryDB("SELECT uid, tamed, named, x, y, z FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE owner='" + owner + "'");
        try
        {
          while (this.rs.next()) {
            msg(sender, ChatColor.GREEN.toString() + ChatColor.BOLD + "Name: " + ChatColor.RESET + this.rs.getString(3).replace("`", "'"));
          }
        }
        catch (SQLException e)
        {
          error("SQL Error - ListAll");
          debug(e.getStackTrace());
        }
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("clearhorses")))
      {
        if (((sender instanceof Player)) && (!perm((Player)sender, "stables.clear")))
        {
          local(sender, "NO_PERM");
          return true;
        }
        if (args.length == 1)
        {
          local(sender, "REMOVE_NOARG");
          return true;
        }
        if (getServer().getPlayerExact(args[1]) == null)
        {
          local(sender, "UNKNOWN_OWNER");
          return true;
        }
        String owner = getServer().getPlayerExact(args[1]).getName();
        
        writeDB("DELETE FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE owner='" + owner + "'");
        writeDB("DELETE FROM " + getConfig().getString("MySQL.prefix") + "riders WHERE owner='" + owner + "'");
        writeDB("DELETE FROM " + getConfig().getString("MySQL.prefix") + "stable WHERE owner='" + owner + "'");
        

        msg(sender , ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "Horses cleared.");
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("addrider")))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        if (args.length == 1)
        {
          p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Who do you want to add as a rider?");
          return true;
        }
        p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Punch the horse you want to add the rider to.");
        p.setMetadata("stables.addrider", new FixedMetadataValue(plugin, args[1].toLowerCase()));
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("delrider")))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        if (args.length == 1)
        {
          p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Who do you want to delete as a rider?");
          return true;
        }
        p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Punch the horse you want to delete the rider from.");
        p.setMetadata("stables.delrider", new FixedMetadataValue(plugin, args[1].toLowerCase()));
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("removechest")))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Hit the horse you wish to remove the chest of.");
        p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "WARNING: Anything remaining in the chest will be DESTROYED!");
        p.setMetadata("stables.removechest", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("store")))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        if (!getConfig().getBoolean("stable.useCommand"))
        {
          msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "You are unable to do that.");
          return true;
        }
        commandStore((Player)sender);
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("view")))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        viewStables((Player)sender);
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("recover")))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        recoverStables((Player)sender, args, 1);
        return true;
      }
      if ((args.length >= 1) && ((args[0].equalsIgnoreCase("ro")) || (args[0].equalsIgnoreCase("removeowner"))))
      {
        if (!(sender instanceof Player))
        {
          local(sender, "NO_CONSOLE");
          return true;
        }
        Player p = (Player)sender;
        if (!perm(p, "stables.remove"))
        {
          local(sender, "NO_PERM");
          return true;
        }
        p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Punch the horse you want to remove the owner of.");
        p.setMetadata("stables.removeowner", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("reload")))
      {
        if (!sender.hasPermission("stables.admin"))
        {
          local(sender, "NO_PERM");
          return true;
        }
        reloadConfig();
        reloadlocalConfig();
        
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Stables configuration reloaded.");
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("save")))
      {
        if (!sender.hasPermission("stables.admin"))
        {
          local(sender, "NO_PERM");
          return true;
        }
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Saved.");
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("convert")))
      {
        if (!sender.hasPermission("stables.admin"))
        {
          local(sender, "NO_PERM");
          return true;
        }
        ConvertDatabase(sender);
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("uuid-convert")))
      {
        if (!sender.hasPermission("stables.admin"))
        {
          local(sender, "NO_PERM");
          return true;
        }
        UUIDConversion(sender);
        return true;
      }
      if ((args.length >= 1) && (args[0].equalsIgnoreCase("config")))
      {
        if (!sender.hasPermission("stables.admin"))
        {
          local(sender, "NO_PERM");
          return true;
        }
        changeConfig(sender, args);
        return true;
      }
    }
    return false;
  }
  
  public void UUIDConversion(CommandSender s)
  {
    msg(s, "Beginning conversion .... this may take some time!");
    ConvertRiders(s);
    ConvertOwners(s);
    ConvertStables(s);
    msg(s, "Finished converting UUIDs");
  }
  
  private void ConvertRiders(CommandSender s)
  {
    ArrayList<String> names = new ArrayList();
    msg(s, "Converting horse riders ...");
    
    String query = "SELECT owner, owneruuid FROM " + getConfig().getString("MySQL.prefix") + "riders;";
    debug(query);
    this.rs = queryDB(query);
    try
    {
      while (this.rs.next())
      {
        String name = this.rs.getString("owner");
        if (this.rs.getString("owneruuid") != null) {
          debug("UUID entered for " + name + " already .. skipping!");
        } else if (!names.contains(name)) {
          names.add(name);
        }
      }
    }
    catch (Exception e)
    {
      error("SQL Error - UUIDConversion");
    }
    msg(s, "Converting " + names.size() + " entries");
    while (names.size() > 0)
    {
      query = "UPDATE " + getConfig().getString("MySQL.prefix") + "riders SET owneruuid= '" + getServer().getOfflinePlayer((String)names.get(0)).getUniqueId() + "' WHERE owner='" + (String)names.get(0) + "';";
      writeDB(query);
      debug(query);
      names.remove(0);
    }
    names.clear();
    
    msg(s, "Converting horse riders x2 ...");
    this.rs = queryDB("SELECT rider, rideruuid FROM " + getConfig().getString("MySQL.prefix") + "riders");
    try
    {
      while (this.rs.next())
      {
        String name = this.rs.getString("rider");
        if (this.rs.getString("rideruuid") != null) {
          debug("UUID entered for " + name + " already .. skipping!");
        } else if (!names.contains(name)) {
          names.add(name);
        }
      }
    }
    catch (Exception e)
    {
      error("SQL Error - UUIDConversion");
    }
    msg(s, "Converting " + names.size() + " entries");
    while (names.size() > 0)
    {
      query = "UPDATE " + getConfig().getString("MySQL.prefix") + "riders SET rideruuid= '" + getServer().getOfflinePlayer((String)names.get(0)).getUniqueId() + "' WHERE rider='" + (String)names.get(0) + "';";
      writeDB(query);
      debug(query);
      names.remove(0);
    }
  }
  
  private void ConvertStables(CommandSender s)
  {
    ArrayList<String> names = new ArrayList();
    
    msg(s, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "Converting horse stables ...");
    this.rs = queryDB("SELECT owner, owneruuid FROM " + getConfig().getString("MySQL.prefix") + "stables");
    try
    {
      while (this.rs.next())
      {
        String name = this.rs.getString("owner");
        if (this.rs.getString("owneruuid") != null) {
          debug("UUID entered for " + name + " already .. skipping!");
        } else if (!names.contains(name)) {
          names.add(name);
        }
      }
    }
    catch (Exception e)
    {
      error("SQL Error - UUIDConversion");
    }
    msg(s, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "Converting " + names.size() + " entries");
    while (names.size() > 0)
    {
      String query = "UPDATE " + getConfig().getString("MySQL.prefix") + "stables SET owneruuid= '" + getServer().getOfflinePlayer((String)names.get(0)).getUniqueId() + "' WHERE owner='" + (String)names.get(0) + "';";
      writeDB(query);
      debug(query);
      names.remove(0);
    }
    names.clear();
  }
  
  private void ConvertOwners(CommandSender s)
  {
    ArrayList<String> names = new ArrayList();
    

    msg(s, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "Converting horse owners ...");
    this.rs = queryDB("SELECT owner, owneruuid FROM " + getConfig().getString("MySQL.prefix") + "horses");
    try
    {
      while (this.rs.next())
      {
        String name = this.rs.getString("owner");
        if (this.rs.getString("owneruuid") != null) {
          debug("UUID entered for " + name + " already .. skipping!");
        } else if (!names.contains(name)) {
          names.add(name);
        }
      }
    }
    catch (Exception e)
    {
      error("SQL Error - UUIDConversion");
    }
    msg(s, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "Converting " + names.size() + " entries");
    while (names.size() > 0)
    {
      String query = "UPDATE " + getConfig().getString("MySQL.prefix") + "horses SET owneruuid= '" + getServer().getOfflinePlayer((String)names.get(0)).getUniqueId() + "' WHERE owner='" + (String)names.get(0) + "';";
      writeDB(query);
      debug(query);
      names.remove(0);
    }
  }
  
  public void viewStables(Player p)
  {
    int num = 0;
    msg(p, getLang("STABLES_LISTING", null));
    this.rs = queryDB("SELECT name FROM " + getConfig().getString("MySQL.prefix") + "stable WHERE owner='" + p.getName() + "'");
    try
    {
      while (this.rs.next())
      {
        String name = this.rs.getString(1).replaceAll("`", "\"");
        num++;
        p.sendMessage(num + ") Name: " + name);
      }
      p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Use /stables recover # to retrieve a horse.");
      return;
    }
    catch (SQLException e)
    {
      e.printStackTrace();
      error("SQL Error - View");
    }
  }
  
  public void recoverStables(Player p, String[] args, int arg)
  {
    ArrayList<String> a = new ArrayList();
    if (disabledWorld("stable.useEnabled", "stable.disabled", p.getWorld().getName()))
    {
      p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "You are unable to do that here!");
      return;
    }
    if (!enabledWorld("stable.useEnabled", "stable.enabled", p.getWorld().getName()))
    {
      p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "You are unable to do that here.");
      return;
    }
    if (this.worldguard != null)
    {
      debug("Checking worldguard ....");
      RegionManager r = this.worldguard.getRegionManager(p.getWorld());
      
      ApplicableRegionSet set = r.getApplicableRegions(p.getLocation());
      if (r.size() != 0) {
        if (!set.allows(DefaultFlag.MOB_SPAWNING))
        {
          debug("WG: No mob spawning here!");
          local(p, "RECOVER_WG");
          return;
        }
      }
    }
    if ((!p.getWorld().getAllowAnimals()) || (!p.getWorld().getAllowMonsters()))
    {
      debug("World Settings - No mobs");
      local(p, "RECOVER_WG");
      return;
    }
    if ((!getConfig().getBoolean("stable.useCommand")) && (!atStable(p.getLocation(), Integer.valueOf(5))))
    {
      p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "You are not close enough to the stables for that.");
      return;
    }
    if (args != null) {
      while (arg + 1 <= args.length)
      {
        a.add(args[arg]);
        arg++;
      }
    }
    if ((args == null) || (a.size() == 0))
    {
      int num = 0;
      this.rs = queryDB("SELECT name FROM " + getConfig().getString("MySQL.prefix") + "stable WHERE owner='" + p.getName() + "'");
      try
      {
        msg(p, getLang("STABLES_LISTING", null));
        while (this.rs.next())
        {
          num++;
          p.sendMessage(num + ") " + this.rs.getString("name").replaceAll("`", "'"));
        }
        if (num != 0)
        {
          msg(p, getLang("STABLES_RECOVERWHO", null));
          msg(p, getLang("STABLES_RECOVERWHO2", null));
        }
        else
        {
          p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "None!");
        }
        return;
      }
      catch (SQLException e)
      {
        p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "You do not have any horses in your stables!");
        return;
      }
    }
    String horseName = "";
    arg = 0;
    while (arg < a.size())
    {
      horseName = horseName + " " + ((String)a.get(arg)).replaceAll(";", "-");
      arg++;
    }
    String query = "SELECT uid, health, type, chested, bred, variant, temper, tamed, saddled, armoritem, name, str FROM " + getConfig().getString("MySQL.prefix") + "stable WHERE owner='" + p.getName() + "' AND name LIKE '" + horseName.replaceAll("'", "`").trim() + "%'";
    debug(query);
    this.rs = queryDB(query);
    try
    {
      if (this.rs.next())
      {
        String name = this.rs.getString(11).replaceAll("`", "'");
        msg(p, getLang("STABLES_RETURNOK", name));
        HorseModifier hm = HorseModifier.spawn(p.getLocation());
        Horse h = (Horse)hm.getHorse();
        String chest = this.rs.getString(4);
        String bred = this.rs.getString(5);
        String tamed = this.rs.getString(8);
        String saddle = this.rs.getString(9);
        

        hm.setChested(chest.equals("1"));
        
        h.setBreed(bred.equals("1"));
        hm.setTamed(true);
        h.setTamed(true);
        h.setDomestication(h.getMaxDomestication());
        h.setOwner(p);
        if (saddle.equals("1")) {
          h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        }
        Double health = Double.valueOf(this.rs.getDouble(2));
        
        hm.setType(HorseModifier.HorseType.fromId(this.rs.getInt(3)));
        hm.setVariant(HorseModifier.HorseVariant.fromId(this.rs.getInt(6)));
        hm.setTemper(this.rs.getInt(7));
        if (this.rs.getInt(10) != 0)
        {
          ItemStack armor = new ItemStack(this.rs.getInt(10), 1);
          h.getInventory().setArmor(armor);
        }
        h.setCustomName(name);
        h.setJumpStrength(this.rs.getDouble(12));
        h.setMaxHealth(health.doubleValue());
        h.setHealth(health.doubleValue());
        String newID = hm.getHorse().getUniqueId().toString();
        String oldID = this.rs.getString(1);
        
        writeDB("UPDATE " + getConfig().getString("MySQL.prefix") + "horses SET uid = '" + newID + "' WHERE uid='" + oldID + "';");
        writeDB("UPDATE " + getConfig().getString("MySQL.prefix") + "riders SET uid = '" + newID + "' WHERE uid='" + oldID + "';");
        writeDB("DELETE FROM " + getConfig().getString("MySQL.prefix") + "stable WHERE uid='" + oldID + "';");
        return;
      }
      recoverStables(p, null, 0);
      
      return;
    }
    catch (SQLException e)
    {
      error("SQL Error - Recover");
      e.printStackTrace();
    }
  }
  
  public boolean perm(Player p, String pe)
  {
    if (pe.contains("punish")) {
      return p.hasPermission(pe);
    }
    return (p.hasPermission(pe)) || (p.hasPermission("stables.admin"));
  }
  
  public boolean isHorse(Entity entity)
  {
    if ((entity instanceof Horse)) {
      return true;
    }
    if (!(entity instanceof LivingEntity)) {
      return false;
    }
    if (((entity instanceof Sheep)) || ((entity instanceof Cow)) || ((entity instanceof Minecart)) || ((entity instanceof Boat)) || ((entity instanceof MushroomCow)) || ((entity instanceof Pig)) || ((entity instanceof Wolf)) || ((entity instanceof Bat)) || ((entity instanceof Chicken)) || ((entity instanceof Blaze)) || ((entity instanceof CaveSpider)) || ((entity instanceof Creeper)) || ((entity instanceof EnderDragon)) || ((entity instanceof Enderman)) || ((entity instanceof Ghast)) || ((entity instanceof Giant)) || ((entity instanceof Golem)) || ((entity instanceof HumanEntity)) || ((entity instanceof IronGolem)) || ((entity instanceof MagmaCube)) || ((entity instanceof Monster)) || ((entity instanceof Ocelot)) || ((entity instanceof PigZombie)) || ((entity instanceof Player)) || ((entity instanceof Silverfish)) || ((entity instanceof Skeleton)) || ((entity instanceof Slime)) || ((entity instanceof Snowman)) || ((entity instanceof Spider)) || ((entity instanceof Squid)) || ((entity instanceof Villager)) || ((entity instanceof Witch)) || ((entity instanceof Wither)) || ((entity instanceof Zombie)) || ((entity instanceof Item)) || ((entity instanceof ExperienceOrb)) || ((entity instanceof Painting)) || ((entity instanceof Arrow)) || ((entity instanceof Snowball)) || ((entity instanceof Fireball)) || ((entity instanceof SmallFireball)) || ((entity instanceof EnderPearl)) || ((entity instanceof EnderSignal)) || ((entity instanceof ThrownExpBottle)) || ((entity instanceof ItemFrame)) || ((entity instanceof WitherSkull)) || ((entity instanceof TNTPrimed)) || ((entity instanceof FallingBlock)) || ((entity instanceof Firework)) || ((entity instanceof Boat)) || ((entity instanceof Minecart)) || ((entity instanceof EnderCrystal)) || ((entity instanceof Egg)) || ((entity instanceof Weather)) || ((entity instanceof Player))) {
      return false;
    }
    debug("Entity: " + entity.getClass());
    return true;
  }
  
  public void changeConfig(CommandSender sender, String[] args)
  {
    boolean found = false;
    if (args.length == 1)
    {
      msg(sender, "===============" + ChatColor.GREEN + " [" + ChatColor.DARK_GREEN + "Stables Config Settings" + ChatColor.GREEN + "] " + ChatColor.WHITE + "===============");
      msg(sender, ChatColor.DARK_GREEN + "To view current values use " + ChatColor.WHITE + "/stables report" );
      msg(sender, ChatColor.GREEN + "The following options are set as a toggle:");
      msg(sender, "/stables config Debug");
      msg(sender, "/stables config Block");
      msg(sender, "/stables config Lure");
      msg(sender, "/stables config Virtual");
      msg(sender, "/stables config Recipe");
      msg(sender, "/stables config Name");
      return;
    }
    if ((args.length >= 2) && (args[1].equalsIgnoreCase("debug")))
    {
      getConfig().set("general.Debug", Boolean.valueOf(!getConfig().getBoolean("general.Debug")));
      msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Toggled: Debug message is now " + getConfig().getBoolean("general.Debug"));
      saveConfig();
      reloadConfig();
      found = true;
      return;
    }
    if ((args.length >= 2) && (args[1].equalsIgnoreCase("Name")))
    {
      msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "This is only editable directly in the config for now."); return;
    }
    String set;
    if ((args.length >= 2) && (args[1].equalsIgnoreCase("Lure")))
    {
      if (args.length != 4)
      {
        syntax(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "/stables config lure (option) (setting)");
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Valid lure options are: allow, chance, item, delay, disabled, min, max");
        return;
      }
      String option = args[2].toLowerCase();
      set = args[3];
      String str1;
      switch ((str1 = option).hashCode())
      {
      case -1361636556: 
        if (str1.equals("chance")) {
        //  break label405;
        }
        break;
      case 107876: 
        if (str1.equals("max")) {
         // break label405;
        }
        break;
      case 108114: 
        if (str1.equals("min")) {
         // break label405;
        }
        break;
      case 3242771: 
        if (str1.equals("item")) {
         // break label405;
        }
        break;
      case 92906313: 
        if (str1.equals("allow")) {
         // break label405;
        }
        break;
      case 95467907: 
      case 270940796: 
        if ((str1.equals("delay"))){ // || ((goto 403) && (str1.equals("disabled")))) {
         // break label405;
        }
      }
      found = false;
      label405:
      if (!found)
      {
        syntax(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE +  "/stables config lure (option) (setting)");
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Valid lure options are: allow, chance, item, delay, disabled, min, max");
        return;
      }
      saveConfig(); return;
    }
    String option;
    if ((args.length >= 2) && (args[1].equalsIgnoreCase("Virtual")))
    {
      if (args.length != 4)
      {
        syntax(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "/stables config virtual (option) (setting)");
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Valid Virtual Stable options are: cost, command, timeout, disabled");
        return;
      }
      option = args[2];
      set = args[3];
    }
    else if ((args.length >= 2) && (args[1].equalsIgnoreCase("Recipe")))
    {
      if (args.length != 3)
      {
        syntax(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "/stables config recipe (option)");
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Valid recipe options are: saddle, nametag, ironarmor, goldarmor, diamondarmor, usePerms");
        return;
      }
      option = args[2];
    }
    else if ((args.length >= 2) && (args[1].equalsIgnoreCase("block")))
    {
      if (args.length != 3)
      {
        syntax(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "/stables config block (type)");
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Valid block types are: all, pvp, environment, owner, mob");
        return;
      }
      String type = args[2];
      if (type.equals("all"))
      {
        getConfig().set("general.BlockAll", Boolean.valueOf(!getConfig().getBoolean("general.BlockAll")));
      }
      else if (type.equals("pvp"))
      {
        getConfig().set("general.PVPDamage", Boolean.valueOf(!getConfig().getBoolean("general.PVPDamage")));
      }
      else if (type.equals("environment"))
      {
        getConfig().set("general.EnvironmentDamage", Boolean.valueOf(!getConfig().getBoolean("general.EnvironmentDamage")));
      }
      else if (type.equals("owner"))
      {
        getConfig().set("general.OwnerDamage", Boolean.valueOf(!getConfig().getBoolean("general.OwnerDamage")));
      }
      else if (type.equals("mob"))
      {
        getConfig().set("general.MobDamage", Boolean.valueOf(!getConfig().getBoolean("general.MobDamage")));
      }
      else
      {
        syntax(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "/stables config block (type)");
        msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Valid block types are: all, pvp, environment, owner, mob");
        return;
      }
      msg(sender, "Damage config is now:");
      msg(sender, "Block ALL: " + getConfig().getBoolean("general.BlockAll"));
      msg(sender, "Block PVP: " + getConfig().getBoolean("general.PVPDamage"));
      msg(sender, "Block Mob: " + getConfig().getBoolean("general.MobDamage"));
      msg(sender, "Block Owner: " + getConfig().getBoolean("general.OwnerDamage"));
      msg(sender, "Block Enviroment: " + getConfig().getBoolean("general.EnvironmentDamage"));
      found = true;
    }
    else if ((args.length >= 2) && (args[1].equals("max")))
    {
      if (args.length != 4)
      {
        syntax(sender, "is /stables config max (#) (amount)");
        return;
      }
      String perm = args[2].toString();
      set = args[3].toString();
    }
    if (!found)
    {
      msg(sender, ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "That is an invalid option.");
      return;
    }
    saveConfig();
    reloadConfig();
  }
  
  public boolean atStable(Location loc, Integer radius)
  {
    World world = loc.getWorld();
    for (int y = 1; y > -radius.intValue(); y--) {
      for (int x = 1; x > -radius.intValue(); x--) {
        for (int z = 1; z > -radius.intValue(); z--)
        {
          Block scan = world.getBlockAt((int)loc.getX() + x, (int)loc.getY() + y, (int)loc.getZ() + z);
          if (((scan.getType() == Material.WALL_SIGN ? 1 : 0) | (scan.getType() == Material.SIGN_POST ? 1 : 0)) != 0)
          {
            Sign sign = (Sign)scan.getState();
            String stablesign = ChatColor.stripColor(sign.getLine(0));
            if (stablesign.equals("[Stables]"))
            {
              debug("Stables found nearby...");
              return true;
            }
          }
        }
      }
    }
    return false;
  }
  
  public void removeHorse(String id)
  {
    plugin.writeDB("DELETE FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE uid='" + id + "'");
    plugin.writeDB("DELETE FROM " + getConfig().getString("MySQL.prefix") + "riders WHERE uid='" + id + "'");
    plugin.writeDB("DELETE FROM " + getConfig().getString("MySQL.prefix") + "stable WHERE uid='" + id + "'");
  }
  
  public boolean disabledWorld(String config, String list, String world)
  {
    if (getConfig().getBoolean(config)) {
      return false;
    }
    String[] worlds = getConfig().getString(list).split(",");
    String[] as;
    int j = (as = worlds).length;
    for (int i = 0; i < j; i++)
    {
      String check = as[i];
      debug("Checking '" + check.trim() + "' vs '" + world + "'");
      if (world.equalsIgnoreCase(check.trim())) {
        return true;
      }
    }
    return false;
  }
  
  public boolean enabledWorld(String check, String list, String world)
  {
    if (!getConfig().getBoolean(check)) {
      return true;
    }
    String[] worlds = getConfig().getString(list).split(",");
    String[] as;
    int j = (as = worlds).length;
    for (int i = 0; i < j; i++)
    {
      String check2 = as[i];
      debug("Enabled Checking '" + check2.trim() + "' vs '" + world + "'");
      if (world.equalsIgnoreCase(check2.trim())) {
        return true;
      }
    }
    return false;
  }
  
  public void stableHorse(LivingEntity horse, String owner)
  {
    HorseModifier h = new HorseModifier(horse);
    Horse he = (Horse)horse;
    if (disabledWorld("stable.useEnabled", "stable.disabled", horse.getWorld().getName()))
    {
      local(getServer().getPlayer(owner), "DISABLED_WORLD");
      return;
    }
    if (!enabledWorld("stable.useEnabled", "stable.enabled", horse.getWorld().getName()))
    {
      local(getServer().getPlayer(owner), "DISABLED_WORLD");
      return;
    }
    if ((!getServer().getPlayer(owner).hasPermission("stables.free")) && (economy != null) && (getConfig().getDouble("stable.cost") > 0.0D))
    {
      double cost = getConfig().getInt("stable.cost");
      if (economy.getBalance(owner) < cost)
      {
        getServer().getPlayer(owner).sendMessage(getLang("TOO_POOR", Double.valueOf(cost)));
        return;
      }
      getServer().getPlayer(owner).sendMessage(getLang("FEE_COLLECT", Double.valueOf(cost)));
      economy.withdrawPlayer(owner, cost);
    }
    local(getServer().getPlayer(owner), "MASTER_STORE");
    

    String owneruuid = "'" + HorseOwner(horse.getUniqueId().toString()) + "'";
    
    String id = horse.getUniqueId().toString();
    String name = horse.getCustomName().replaceAll("'", "`");
    int type = h.getType().getId();
    int var = h.getVariant().getId();
    int armor = h.getArmorItem().getTypeId();
    int temper = h.getTemper();
    double str = he.getJumpStrength();
    double health = h.getHorse().getMaxHealth();
    int tame = 0;
    int saddle = 0;
    int chest = 0;
    int bred = 0;
    if (h.isTamed()) {
      tame = 1;
    }
    if (h.isChested()) {
      chest = 1;
    }
    if (h.isBred()) {
      bred = 1;
    }
    if (he.getInventory().getSaddle() != null) {
      saddle = 1;
    }
    String query = "INSERT INTO " + getConfig().getString("MySQL.prefix") + "stable (name, owner, uid,health,type,chested,bred,variant,temper,tamed,saddled,armoritem, str, owneruuid) VALUES( '" + name + "', '" + owner + "', '" + id + "'," + health + ", " + type + ", " + chest + ", " + bred + "," + var + "," + temper + ", " + tame + ", " + saddle + ", " + armor + "," + str + "," + owneruuid + " );";
    debug(query);
    writeDB(query);
    
    horse.remove();
  }
  
  boolean canRide(LivingEntity e, Player p)
  {
    if (perm(p, "stables.ride")) {
      return true;
    }
    if (isRider(p.getUniqueId().toString(), e.getUniqueId().toString())) {
      return true;
    }
    return isOwner(p.getUniqueId().toString(), e.getUniqueId().toString());
  }
  
  boolean canTame(Player player)
  {
    int num = 100;
    int owned = 0;
    boolean notame = false;
    String query = "SELECT * FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE owneruuid='" + player.getUniqueId().toString() + "'";
    debug(query);
    this.rs = queryDB(query);
    try
    {
      while (this.rs.next()) {
        owned++;
      }
    }
    catch (SQLException|NullPointerException e)
    {
      error("SQL Error - canTame");
    }
    if (owned == 0)
    {
      debug(player.getName() + " owns 0 horses.");
      return true;
    }
    debug(player.getName() + " owns " + owned + " horses.");
    for (; num > 0; num--) {
      if (player.hasPermission("stables.max." + num))
      {
        debug("Found VIP permission : " + num);
        if ((num < 1) || (owned < num)) {
          break;
        }
        notame = true;
        break;
      }
    }
    if (num <= 0)
    {
      debug("No special perms found - using default MaxOwned");
      if ((getConfig().getInt("general.MaxOwned.default") >= 1) && (owned >= getConfig().getInt("general.MaxOwned.default"))) {
        notame = true;
      }
    }
    if (notame)
    {
      local(player, "TOO_MANY_HORSES");
      return false;
    }
    return true;
  }
  
  public void ConvertDatabase(CommandSender s)
  {
    File f = new File(getDataFolder(), "horses.yml");
    if (!f.exists())
    {
      msg(s, "Nothing to convert ... horses.yml does not exist.");
      return;
    }
    boolean complete = false;
    FileConfiguration hc = YamlConfiguration.loadConfiguration(f);
    
    msg(s, "Beginning database conversion to SQL ... conversion will take time!");
    String owner;
    for (String horse : hc.getConfigurationSection("horses").getKeys(false))
    {
      String uuid = horse;
      owner = hc.getString("horses." + horse + ".owner");
      String named = hc.getString("horses." + horse + ".named").replaceAll("'", "`").replaceAll(";", "_");
      Long tamed = Long.valueOf(hc.getLong("horses." + horse + ".tamed"));
      int x = 0;
      int y = 0;
      int z = 0;
      String world = "";
      try
      {
        x = hc.getInt("horses." + horse + ".x");
      }
      catch (Exception localException) {}
      try
      {
        y = hc.getInt("horses." + horse + ".y");
      }
      catch (Exception localException1) {}
      try
      {
        z = hc.getInt("horses." + horse + ".z");
      }
      catch (Exception localException2) {}
      try
      {
        world = hc.getString("horses." + horse + ".world");
      }
      catch (Exception localException3) {}
      writeDB("INSERT INTO " + getConfig().getString("MySQL.prefix") + "horses (uid, owner, tamed, named, world, x, y, z, owneruuid) VALUES( '" + uuid + "', '" + owner + "', " + tamed + ", '" + named + "', '" + world + "'," + x + ", " + y + ", " + z + ", null)");
    }
    for (??? = hc.getConfigurationSection("riders").getKeys(false).iterator(); ???.hasNext(); owner.hasNext())
    {
      String horse = (String)???.next();
      owner = hc.getConfigurationSection("riders." + horse).getKeys(false).iterator(); continue;String rider = (String)owner.next();
      String owner = hc.getString("owners." + horse);
      String query = "INSERT INTO " + getConfig().getString("MySQL.prefix") + "riders (uid, name, owner, horse_id, owneruuid, rideruuid) VALUES( '" + horse + "', '" + rider + "', '" + owner + "', null, null, null);";
      debug(query);
      writeDB(query);
    }
    f.renameTo(new File(getDataFolder(), "OLD-horses.yml"));
    msg(s, "Database conversion complete! Please verify your data - if all looks well, you can remove the file 'OLD-horses.yml'");
    msg(s, "Please note: Conversion to SQL did *NOT* include UUID conversion! You must run the UUID conversion seperately!");
  }
  
  Location getHorseLocation(String uid)
  {
    Double x = Double.valueOf(0.0D);Double y = Double.valueOf(0.0D);Double z = Double.valueOf(0.0D);
    String world = null;
    String query = "SELECT x, y, z, world FROM " + getConfig().getString("MySQL.prefix") + "horses WHERE uid='" + uid + "'";
    debug(query);
    this.rs = queryDB(query);
    try
    {
      this.rs.next();
      x = Double.valueOf(this.rs.getDouble("x"));
      y = Double.valueOf(this.rs.getDouble("y"));
      z = Double.valueOf(this.rs.getDouble("z"));
      world = this.rs.getString("world");
    }
    catch (SQLException e)
    {
      debug("Unable to find location.");
      return null;
    }
    if (world == null) {
      return null;
    }
    Location loc = new Location(getServer().getWorld(world), x.doubleValue(), y.doubleValue(), z.doubleValue());
    return loc;
  }
  
  String findHorse(String[] args, int Start, String owneruuid)
  {
    String name = "";
    while (Start < args.length)
    {
      name = name + " " + args[Start];
      Start++;
    }
    name = name.trim();
    debug("SQL Searching ...");
    name = name.replace("'", "`");
    name = name.replace(";", "");
    String query = "SELECT * from " + getConfig().getString("MySQL.prefix") + "horses WHERE owneruuid='" + owneruuid + "' AND named LIKE '" + name + "%'";
    debug(query);
    this.rs = queryDB(query);
    if (this.rs == null) {
      return null;
    }
    try
    {
      debug("Found query ..");
      this.rs.next();
      return this.rs.getString("uid");
    }
    catch (SQLException e)
    {
      debug(query);
      error("findHorse");
    }
    return null;
  }
  
  public String getUUID(String name)
  {
    if (getServer().getPlayer(name) != null) {
      return getServer().getPlayer(name).getUniqueId().toString();
    }
    return null;
  }
  
  public void OpenDatabase()
  {
    String url = "";
    if (getConfig().getBoolean("MySQL.useMySQL"))
    {
      String user = getConfig().getString("MySQL.user");
      String pass = getConfig().getString("MySQL.password");
      String host = getConfig().getString("MySQL.host");
      String db = getConfig().getString("MySQL.database");
      String port = getConfig().getString("MySQL.port");
      if (port.equals("0")) {
        url = "jdbc:mysql://" + host + "/" + db;
      } else {
        url = "jdbc:mysql://" + host + ":" + port + "/" + db;
      }
      try
      {
        this.conn = DriverManager.getConnection(url, user, pass);
        getServer().getLogger().info("Stables loading with MySQL.");
      }
      catch (SQLException e)
      {
        error("Unable to open database with MySQL - Check your database information.");
        debug(e.getStackTrace());
      }
    }
    else if (getConfig().getBoolean("MySQL.useSQLite"))
    {
      String sDriverName = "org.sqlite.JDBC";
      try
      {
        Class.forName(sDriverName);
      }
      catch (ClassNotFoundException e1)
      {
        error("Unable to load SqlDrivers - Please check your setup and reload Stables.");
        debug(e1.getStackTrace());
        getServer().getPluginManager().disablePlugin(this);
        return;
      }
      url = "jdbc:sqlite:" + new File(getDataFolder(), "stables.db");
      try
      {
        this.conn = DriverManager.getConnection(url);
        getServer().getLogger().info("Stables loading with SQLite.");
      }
      catch (SQLException e)
      {
        error("Unable to open database with SQLite");
        debug(e.getStackTrace());
      }
    }
    else
    {
      getServer().getLogger().info("You must use mySQL or SQLite. Check your configuration and load Stables again.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }
    if (setup) {
      return;
    }
    setup = true;
    
    writeDB("CREATE TABLE IF NOT EXISTS " + getConfig().getString("MySQL.prefix") + "horses ( id double PRIMARY KEY, uid text, owner text, tamed long, named text, x double, y double, z double, world text ) ");
    writeDB("CREATE TABLE IF NOT EXISTS " + getConfig().getString("MySQL.prefix") + "riders ( id double PRIMARY KEY, uid text, name text, owner text, horse_id integer ) ");
    writeDB("CREATE TABLE IF NOT EXISTS " + getConfig().getString("MySQL.prefix") + "stable ( id double PRIMARY KEY, uid text, name text, owner text, health integer, type integer, chested boolean, bred boolean, variant integer, temper integer, tamed boolean, saddled boolean, armoritem integer)");
    
    writeDB("CREATE TABLE IF NOT EXISTS " + getConfig().getString("MySQL.prefix") + "auction ( id double PRIMARY KEY, bid double, bidder string, end long, type integer, var intenger, sold_to string)");
    writeDB("CREATE TABLE IF NOT EXISTS " + getConfig().getString("MySQL.prefix") + "return_bids ( id double PRIMARY KEY, bid double, bidder string)");
    
    AddCol(getConfig().getString("MySQL.prefix") + "riders", "horse_id", "integer");
    AddCol(getConfig().getString("MySQL.prefix") + "stable", "name", "text");
    AddCol(getConfig().getString("MySQL.prefix") + "stable", "owner", "text");
    AddCol(getConfig().getString("MySQL.prefix") + "stable", "str", "double");
    AddCol(getConfig().getString("MySQL.prefix") + "horses", "world", "text");
    

    AddCol(getConfig().getString("MySQL.prefix") + "horses", "owneruuid", "text");
    AddCol(getConfig().getString("MySQL.prefix") + "stable", "owneruuid", "text");
    AddCol(getConfig().getString("MySQL.prefix") + "riders", "owneruuid", "text");
    AddCol(getConfig().getString("MySQL.prefix") + "riders", "rideruuid", "text");
    

    writeDB("DROP TABLE IF EXISTS " + getConfig().getString("MySQL.prefix") + "owners;");
    if (getConfig().getBoolean("MySQL.useMySQL"))
    {
      writeDB("ALTER TABLE " + getConfig().getString("MySQL.prefix") + "horses  CHANGE  `id`  `id` DOUBLE NOT NULL AUTO_INCREMENT");
      writeDB("ALTER TABLE " + getConfig().getString("MySQL.prefix") + "riders  CHANGE  `id`  `id` DOUBLE NOT NULL AUTO_INCREMENT");
      writeDB("ALTER TABLE " + getConfig().getString("MySQL.prefix") + "stable  CHANGE  `id`  `id` DOUBLE NOT NULL AUTO_INCREMENT");
      writeDB("ALTER TABLE " + getConfig().getString("MySQL.prefix") + "horses  CHANGE  `tamed`  `tamed` LONG");
    }
  }
  
  public void writeDB(String query)
  {
    try
    {
      if (this.conn == null) {
        OpenDatabase();
      }
      Statement statement = this.conn.createStatement();
      statement.setQueryTimeout(10);
      statement.executeUpdate(query);
    }
    catch (SQLException e)
    {
      if (!setup) {
        error("writeDB error");
      }
    }
  }
  
  public ResultSet queryDB(String query)
  {
    try
    {
      if (this.rs != null) {
        this.rs.close();
      }
      if ((this.conn == null) || (this.conn.isClosed()))
      {
        debug("Opening database ...");
        OpenDatabase();
      }
      debug("DB closed: " + this.conn.isClosed());
      
      Statement statement = this.conn.createStatement();
      statement.setQueryTimeout(30);
      this.rs = statement.executeQuery(query);
      return this.rs;
    }
    catch (SQLException e)
    {
      if (this.rs != null) {
        try
        {
          this.rs.close();
        }
        catch (SQLException e1)
        {
          error("queryDB error, ResultSet");
        }
      }
      if (this.conn != null) {
        try
        {
          this.conn.close();
        }
        catch (SQLException e2)
        {
          error("queryDB error, Connection");
        }
      }
    }
    return null;
  }
  
  public void CloseDatabase()
  {
    try
    {
      if (this.conn != null)
      {
        this.rs.close();
        this.conn.close();
      }
    }
    catch (SQLException e)
    {
      error("closeDatabase() error.");
    }
  }
  
  public void commandStore(final Player p)
  {
    if (p == null) {
      return;
    }
    local(p, "HIT_STORE");
    p.setMetadata("stables.store", new FixedMetadataValue(plugin, Boolean.valueOf(true)));
    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
    {
      public void run()
      {
        if (p.hasMetadata("stables.store"))
        {
          p.removeMetadata("stables.store", Stables.plugin);
          Stables.this.msg(p, Stables.this.getLang("STABLES_TIMEOUT", null));
        }
      }
    }, plugin.getConfig().getInt("stable.timeout") * 20);
  }
  
  public void AddCol(String table, String col, String type)
  {
    try
    {
      if (this.rs != null) {
        this.rs.close();
      }
      if (this.conn == null)
      {
        debug("queryDB: Database not open");
        OpenDatabase();
      }
      Statement statement = this.conn.createStatement();
      statement.setQueryTimeout(30);
      this.rs = statement.executeQuery("SELECT COUNT(" + col + ") FROM " + table);
      return;
    }
    catch (SQLException e)
    {
      writeDB("ALTER TABLE " + table + " ADD COLUMN " + col + " " + type);
      
      debug("Adding colum to table ....");
    }
  }
  
  public String getResultString(int num)
  {
    try
    {
      if (this.rs.next()) {
        return this.rs.getString(num);
      }
    }
    catch (SQLException e)
    {
      debug("SQL Error");
      return null;
    }
    return null;
  }
  
  public void updateCheck() {}
  
  public void lureHorse(String name)
  {
    Player player = getServer().getPlayerExact(name);
    if (player == null)
    {
      debug("Player no longer found");
      return;
    }
    int itemId = ((MetadataValue)player.getMetadata("stables.luring").get(0)).asInt();
    debug("Luring horse with itemId" + itemId);
    player.removeMetadata("stables.luring", plugin);
    Random generator = new Random();
    int randomNum = generator.nextInt(100) + 1;
    if (disabledWorld("horses.lure.useEnabled", "horses.lure.disabled", player.getWorld().getName()))
    {
      local(player, "DISABLED_WORLD");
      return;
    }
    if (!enabledWorld("horses.lure.useEnabled", "horses.lure.enabled", player.getWorld().getName()))
    {
      local(player, "DISABLED_WORLD");
      return;
    }
    debug("Num: " + randomNum + " - Chance = " + getConfig().getInt(new StringBuilder("lure.").append(itemId).append(".chance").toString()));
    if (randomNum > getConfig().getInt("lure." + itemId + ".chance"))
    {
      local(player, "LURE_FAIL");
      return;
    }
    HorseModifier hm = HorseModifier.spawn(player.getLocation());
    int horseType = getConfig().getInt("lure." + itemId + ".type");
    if (getConfig().getInt("lure." + itemId + ".type") == -1) {
      horseType = generator.nextInt(3);
    } else if (getConfig().getInt("lure." + itemId + ".type") == -2) {
      horseType = generator.nextInt(5);
    }
    hm.setType(HorseModifier.HorseType.fromId(horseType));
    if (horseType == 0)
    {
      int var = generator.nextInt(31);
      HorseModifier.HorseVariant[] vars = {
        HorseModifier.HorseVariant.WHITE, HorseModifier.HorseVariant.CREAMY, HorseModifier.HorseVariant.CHESTNUT, HorseModifier.HorseVariant.BROWN, HorseModifier.HorseVariant.BLACK, HorseModifier.HorseVariant.GRAY, HorseModifier.HorseVariant.DARK_BROWN, HorseModifier.HorseVariant.WHITE_WHITE, HorseModifier.HorseVariant.CREAMY_WHITE, HorseModifier.HorseVariant.CHESTNUT_WHITE, 
        HorseModifier.HorseVariant.BROWN_WHITE, HorseModifier.HorseVariant.BLACK_WHITE, HorseModifier.HorseVariant.GRAY_WHITE, HorseModifier.HorseVariant.DARK_BROWN_WHITE, HorseModifier.HorseVariant.WHITE_WHITE_FIELD, HorseModifier.HorseVariant.CREAMY_WHITE_FIELD, HorseModifier.HorseVariant.CHESTNUT_WHITE_FIELD, HorseModifier.HorseVariant.BROWN_WHITE_FIELD, HorseModifier.HorseVariant.BLACK_WHITE_FIELD, HorseModifier.HorseVariant.GRAY_WHITE_FIELD, 
        HorseModifier.HorseVariant.DARK_BROWN_WHITE_FIELD, HorseModifier.HorseVariant.WHITE_WHITE_DOTS, HorseModifier.HorseVariant.CREAMY_WHITE_DOTS, HorseModifier.HorseVariant.CHESTNUT_WHITE_DOTS, HorseModifier.HorseVariant.BROWN_WHITE_DOTS, HorseModifier.HorseVariant.BLACK_WHITE_DOTS, HorseModifier.HorseVariant.GRAY_WHITE_DOTS, HorseModifier.HorseVariant.DARK_BROWN_WHITE_DOTS, HorseModifier.HorseVariant.WHITE_BLACK_DOTS, HorseModifier.HorseVariant.CREAMY_BLACK_DOTS, 
        HorseModifier.HorseVariant.CHESTNUT_BLACK_DOTS, HorseModifier.HorseVariant.BROWN_BLACK_DOTS, HorseModifier.HorseVariant.BLACK_BLACK_DOTS, HorseModifier.HorseVariant.GRAY_BLACK_DOTS, HorseModifier.HorseVariant.DARK_BROWN_BLACK_DOTS };
      
      hm.setVariant(vars[var]);
    }
    hm.setTamed(false);
    int MaxHealth = getConfig().getInt("lure." + itemId + ".health.max");
    int MinHealth = getConfig().getInt("lure." + itemId + ".health.min");
    Double health = Double.valueOf(generator.nextInt(MaxHealth - MinHealth) + MinHealth);
    hm.getHorse().setHealth(health.doubleValue());
    hm.getHorse().setMaxHealth(health.doubleValue());
  }
  
  public void spawnHorse(Location loc, boolean z, boolean s)
  {
    Random generator = new Random();
    int var = generator.nextInt(31);
    int type = generator.nextInt(3);
    if (z) {
      type = 3;
    }
    if (s) {
      type = 4;
    }
    HorseModifier.HorseVariant[] vars = {
      HorseModifier.HorseVariant.WHITE, HorseModifier.HorseVariant.CREAMY, HorseModifier.HorseVariant.CHESTNUT, HorseModifier.HorseVariant.BROWN, HorseModifier.HorseVariant.BLACK, HorseModifier.HorseVariant.GRAY, HorseModifier.HorseVariant.DARK_BROWN, HorseModifier.HorseVariant.WHITE_WHITE, HorseModifier.HorseVariant.CREAMY_WHITE, HorseModifier.HorseVariant.CHESTNUT_WHITE, 
      HorseModifier.HorseVariant.BROWN_WHITE, HorseModifier.HorseVariant.BLACK_WHITE, HorseModifier.HorseVariant.GRAY_WHITE, HorseModifier.HorseVariant.DARK_BROWN_WHITE, HorseModifier.HorseVariant.WHITE_WHITE_FIELD, HorseModifier.HorseVariant.CREAMY_WHITE_FIELD, HorseModifier.HorseVariant.CHESTNUT_WHITE_FIELD, HorseModifier.HorseVariant.BROWN_WHITE_FIELD, HorseModifier.HorseVariant.BLACK_WHITE_FIELD, HorseModifier.HorseVariant.GRAY_WHITE_FIELD, 
      HorseModifier.HorseVariant.DARK_BROWN_WHITE_FIELD, HorseModifier.HorseVariant.WHITE_WHITE_DOTS, HorseModifier.HorseVariant.CREAMY_WHITE_DOTS, HorseModifier.HorseVariant.CHESTNUT_WHITE_DOTS, HorseModifier.HorseVariant.BROWN_WHITE_DOTS, HorseModifier.HorseVariant.BLACK_WHITE_DOTS, HorseModifier.HorseVariant.GRAY_WHITE_DOTS, HorseModifier.HorseVariant.DARK_BROWN_WHITE_DOTS, HorseModifier.HorseVariant.WHITE_BLACK_DOTS, HorseModifier.HorseVariant.CREAMY_BLACK_DOTS, 
      HorseModifier.HorseVariant.CHESTNUT_BLACK_DOTS, HorseModifier.HorseVariant.BROWN_BLACK_DOTS, HorseModifier.HorseVariant.BLACK_BLACK_DOTS, HorseModifier.HorseVariant.GRAY_BLACK_DOTS, HorseModifier.HorseVariant.DARK_BROWN_BLACK_DOTS };
    
    HorseModifier hm = HorseModifier.spawn(loc);
    hm.setType(HorseModifier.HorseType.fromId(type));
    if (type == 0) {
      hm.setVariant(vars[var]);
    }
    hm.setTamed(true);
    hm.setSaddled(true);
    int MaxHealth = getConfig().getInt("horses.spawn.health.max");
    int MinHealth = getConfig().getInt("horses.spawn.health.min");
    Double health = Double.valueOf(generator.nextInt(MaxHealth - MinHealth) + MinHealth);
    hm.getHorse().setMaxHealth(health.doubleValue());
  }
  
  public boolean addHorse(Player p, Entity e, boolean auto)
  {
    if (!isHorse(e))
    {
      local(p, "ADD_ERROR");
      return false;
    }
    if (!canTame(p)) {
      return false;
    }
    String horseName = "";
    if (!auto)
    {
      horseName = p.getItemInHand().getItemMeta().getDisplayName().replace("'", "`");
    }
    else
    {
      LivingEntity l = (LivingEntity)e;
      if (l.getCustomName() != null) {
        horseName = l.getCustomName();
      } else {
        horseName = getRandomName();
      }
    }
    writeDB("DELETE FROM " + getConfig().getString("MySQL.prefix") + "riders WHERE uid='" + e.getUniqueId().toString() + "'");
    writeDB("INSERT INTO " + getConfig().getString("MySQL.prefix") + "horses (uid, owneruuid, owner, tamed, named, x, y, z) VALUES( '" + e.getUniqueId().toString() + "', '" + p.getUniqueId().toString() + "', '" + p.getName() + "', " + System.currentTimeMillis() + ", '" + horseName + "', 0, 0, 0 )");
    

    saveLocation((Horse)e);
    if (auto)
    {
      LivingEntity l = (LivingEntity)e;
      l.setCustomName(horseName);
      local(p, "ADD_AUTO");
    }
    else
    {
      local(p, "NEW_STEED");
    }
    if (getConfig().getBoolean("horses.AutoSaddle"))
    {
      debug("Adding a saddle!");
      HorseModifier hm = new HorseModifier((LivingEntity)e);
      hm.setSaddled(true);
    }
    return true;
  }
  
  public String getRandomName()
  {
    Random generator = new Random();
    int num = generator.nextInt(randomNames.size());
    String name = "Biscuit";
    try
    {
      name = (String)randomNames.get(num);
    }
    catch (NullPointerException e)
    {
      name = "Biscuit";
    }
    return name;
  }
  
  public boolean isRider(String name, String horse)
  {
    String query = "SELECT id FROM " + getConfig().getString("MySQL.prefix") + "riders WHERE uid='" + horse + "' AND rideruuid='" + name + "'";
    debug(query);
    try
    {
      this.rs = queryDB(query);
      this.rs.next();
      if (this.rs.getInt("id") >= 0) {
        return true;
      }
      return false;
    }
    catch (SQLException e) {}
    return false;
  }
  
  public boolean isOwner(String uid, String horse)
  {
    try
    {
      this.rs = queryDB("SELECT id FROM " + 
        getConfig().getString("MySQL.prefix") + 
        "horses WHERE uid='" + horse + 
        "' AND owneruuid='" + uid + "'");
      
      this.rs.next();
      if (this.rs.getInt("id") >= 0) {
        return true;
      }
      return false;
    }
    catch (SQLException e) {}
    return false;
  }
  
  void nameHorse(String id, String name)
  {
    plugin.writeDB("UPDATE " + plugin.getConfig().getString("MySQL.prefix") + "horses SET named='" + name + "' WHERE uid='" + id + "'");
  }
  
  void saveLocation(Horse h)
  {
    if (plugin.HorseOwner(h.getUniqueId().toString()) != null)
    {
      debug("Saving horse location : " + h.getUniqueId());
      
      int x = h.getLocation().getBlockX();
      int y = h.getLocation().getBlockY();
      int z = h.getLocation().getBlockZ();
      

      String query = "UPDATE " + 
        getConfig().getString("MySQL.prefix") + 
        "horses SET x=" + x + ", y=" + y + ", z=" + z + ", world='" + h.getLocation().getWorld().getName() + "'" + 
        " WHERE uid='" + h.getUniqueId() + "';";
      plugin.writeDB(query);
      debug(query);
    }
  }
  
  private void setupLanguage()
  {
    setLang("SYNTAX", "&a[&2Stables&a]&f Syntax is: ");
    setLang("ADD_HIT", "&a[&2Stables&a]&f Punch the horse you want to add the rider to.");
    setLang("ADD_NOARG", "&a[&2Stables&a]&f Who do you want to add as a rider?");
    setLang("CONFIG_ERROR", "'&a[&2Stables&a]&f Could not save config to'");
    setLang("CONFIG_RELOAD", "&a[&2Stables&a]&f Stables configuration reloaded.");
    setLang("CONFIG_SAVE", "&a[&2Stables&a]&f Horses saved.");
    setLang("DEL_HIT", "&a[&2Stables&a]&f Punch the horse you want to delete the rider from.");
    setLang("DEL_NOARG", "'&a[&2Stables&a]&f Who do you want to delete as a rider?'");
    setLang("HIT_FREE", "'&a[&2Stables&a]&f You set this beast free.'");
    setLang("HIT_MAX", "&a[&2Stables&a]&f You already own too many horses! You cannot tame this beast.");
    setLang("HIT_NEW", "&a[&2Stables&a]&f Enjoy your new horse!");
    setLang("HIT_REMOVE", "&a[&2Stables&a]&f Punch the horse you want to remove the owner from.");
    setLang("LIST_NOARG", "&a[&2Stables&a]&f Who do you wish to list the horses of?");
    setLang("NO_CONSOLE", "&a[&2Stables&a]&f This command cannot be run from the console.");
    setLang("NO_PERM", "&a[&2Stables&a]&c You do not have permission for that.");
    setLang("NOT_OWNER", "&a[&2Stables&a]&f That is not even your horse!");
    setLang("PERM_NOCLEAR", "&a[&2Stables&a]&f That is not your horse! You cannot set it free!");
    setLang("PERM_NORIDE", "&a[&2Stables&a]&f You have not been given permission to ride that horse!");
    setLang("PERM_NOTHEFT", "&a[&2Stables&a]&f That is not your horse! That belongs to %1");
    setLang("RECIPE_ADDED", "&a[&2Stables&a]&f Recipe added:");
    setLang("REMOVE_NOARG", "&a[&2Stables&a]&f Who do you wish to remove the horses of?");
    setLang("REMOVE_NOHORSE", "&a[&2Stables&a]&f That player owns no horses.");
    setLang("RIDER_ADD", "&a[&2Stables&a]&f Rider added!");
    setLang("RIDER_ADD_FAILED", "&a[&2Stables&a]&f The UUID of that player could not be found. Please try again.");
    setLang("RIDER_DEL", "&a[&2Stables&a]&f Rider removed.");
    setLang("RO_HIT", "&a[&2Stables&a]&f Punch the horse you want to remove the owner of.");
    setLang("UNKNOWN_OWNER", "&a[&2Stables&a]&f That owner is unknown.");
    
    setLang("SUMMON_HORSE", "&a[&2Stables&a]&f You summon your steed to your location.");
    setLang("CHECK_HIT", "&a[&2Stables&a]&f Punch the horse you want to check the info of.");
    setLang("LIST_NOHORSE", "&a[&2Stables&a]&f That player owns no horses.");
    setLang("HORSE_UNKNOWN", "&a[&2Stables&a]&f A horse by that name was not located.");
    setLang("HORSE_NOT_FOUND", "&a[&2Stables&a]&f Your steed could not be located.");
    setLang("TP_FOUND", "&a[&2Stables&a]&f You teleport to your steed's last known location.");
    setLang("COMMAND_DISABLED", "&a[&2Stables&a]&f A mystical force prevents you from doing this.");
    setLang("HORSE_WRONG_WORLD", "&a[&2Stables&a]&f Your steed was not found in this world.");
    setLang("COMPASS_LOCKED", "&a[&2Stables&a]&f Your compass has locked in to your steed's last location.");
    setLang("DISABLED_WORLD", "&a[&2Stables&a]&f You are unable to do that here!");
    setLang("TOO_POOR", "&a[&2Stables&a]&f You are unable to afford the stable master's fee of $%1");
    setLang("FEE_COLLECT", "&a[&2Stables&a]&f The stable master collects his fee of $%1");
    setLang("MASTER_STORE", "&a[&2Stables&a]&f The stable master leads your horse into a stall.");
    setLang("TOO_MANY_HORSES", "&a[&2Stables&a]&f You already own too many horses! You cannot tame this beast.");
    setLang("HIT_STORE", "&a[&2Stables&a]&f Hit the horse you wish to store.");
    setLang("LURE_FAIL", "&a[&2Stables&a]&f You failed to lure any horses out.");
    setLang("EXIT_NOT_TAME", "&a[&2Stables&a]&f This horse has not yet been named, and is not claimed by you. Use a name tag to claim it.");
    setLang("PUNISH_BREED", "&a[&2Stables&a]&f Your ability to breed horses has been revoked.");
    setLang("PUNISH_NAME", "&a[&2Stables&a]&f Your ability to name horses has been revoked.");
    setLang("NOT_RIDER", "&a[&2Stables&a]&f %1 has not given you permission to ride that horse!");
    setLang("SET_FREE", "&a[&2Stables&a]&f You set this beast free.");
    setLang("NEW_STEED", "&a[&2Stables&a]&f Enjoy your new steed!");
    setLang("REMOVE_CHEST", "&a[&2Stables&a]&f You have removed the chest from your steed.");
    setLang("NO_CHESTS", "&a[&2Stables&a]&f The stable master cannot be held responsible for a horse's inventory, and refuses to stable your horse at this time. You may use /stables removechest instead.");
    setLang("ALREADY_LURE", "&a[&2Stables&a]&f Shh! You're already trying to lure out a horse!");
    setLang("START_LURE", "&a[&2Stables&a]&f You begin trying to lure out a horse ...");
    setLang("RECIPE_PERM", "&a[&2Stables&a]&f You do not have the knowledge to craft that item!");
    setLang("HORSE_ABANDON", "&a[&2Stables&a]&f You abandon your steed.");
    setLang("HORSE_ABANDON_NOT_FOUND", "&a[&2Stables&a]&f You abandon your steed. Note: The physical horse was not located. As such, it may remain 'named', but is no longer claimed by you.");
    setLang("CMD_NAME", "&a[&2Stables&a]&f Change the name of (player)'s horse to (new name)");
    setLang("CMD_ADD", "&a[&2Stables&a]&f Add (rider) to your horse");
    setLang("CMD_DEL", "&a[&2Stables&a]&f Remove (rider) from your horse");
    setLang("CMD_LIST", "&a[&2Stables&a]&f List all of your own horses");
    setLang("CMD_ABANDON", "&a[&2Stables&a]&f Free (horse) from your ownership");
    setLang("CMD_VIEW", "&a[&2Stables&a]&f Show all horses in your virtual stables");
    setLang("CMD_STORE", "&a[&2Stables&a]&f Store a horse in your virtual stables");
    setLang("CMD_RECOVER", "&a[&2Stables&a]&f Recover horse # from your virtual stables. Requires #, NOT NAME");
    setLang("CMD_FIND", "&a[&2Stables&a]&f Point a compass to your horse's last location");
    setLang("CMD_SUMMON", "&a[&2Stables&a]&f Summon your horse to your location");
    setLang("CMD_TP", "&a[&2Stables&a]&f Teleport to your horse's last location");
    setLang("CMD_CHECK", "&a[&2Stables&a]&f View a horse's information & owner");
    setLang("CMD_RO", "&a[&2Stables&a]&f Remove a horse's owner");
    setLang("CMD_LISTALL", "&a[&2Stables&a]&f View all of (player)'s horses");
    setLang("CMD_CLEAR", "&a[&2Stables&a]&f Remove ALL horses owned by (player)");
    setLang("CMD_RELOAD", "&a[&2Stables&a]&f Reload the config file - will not change database options");
    setLang("CMD_SAVE", "&a[&2Stables&a]&f Force a save of the horse database");
    setLang("CMD_CONFIG", "&a[&2Stables&a]&f Alter config options");
    setLang("CMD_CONVERT", "&a[&2Stables&a]&f Convert Flatfile YAML config to SQL");
    setLang("CMD_RENAME", "&a[&2Stables&a]&f Rename a horse from a random list of names");
    setLang("ADD_ERROR", "&a[&2Stables&a]&f That is not a horse! You cannot claim it!");
    setLang("ADD_AUTO", "&a[&2Stables&a]&f You have claimed this steed as your own!");
    setLang("NEW_NAME", "&a[&2Stables&a]&f You have given your steed a new name!");
    setLang("RENAME_NOT_FOUND", "&a[&2Stables&a]&f Your horse couldn't be found near by - are you too far away?");
    setLang("RECOVER_WG", "'&a[&2Stables&a]&f This area is protected! The stablemaster will not deliver here!'");
    
    setLang("STABLES_RETURNOK", "&a[&2Stables&a]&f The stable master waders off to the stalls, then returns with %1");
    setLang("STABLES_LISTING", "&a[&2Stables&a]&f You have the following horses in your stables:");
    setLang("STABLES_RECOVERWHO", "&a[&2Stables&a]&f Which horse did you want to recover?");
    setLang("STABLES_RECOVERWHO2", "&a[&2Stables&a]&f Type /stables recover (name)");
    setLang("STABLES_TIMEOUT", "&a[&2Stables&a]&f Stable storage timeout.");
    setLang("STABLES_RECOVERSIGN1", "&a[&2Stables&a]&f Use /recover");
    setLang("STABLES_RECOVERSIGN2", "&a[&2Stables&a]&f to retreive!");
    

    savelocalConfig();
  }
  
  public void addRandomNames()
  {
    if (!getConfig().isConfigurationSection("randomNames"))
    {
      List<String> n = new ArrayList();
      
      n.add("Ace");
      n.add("Agatha");
      n.add("Airheart");
      n.add("Amberlocks");
      n.add("Ambrosia");
      n.add("Amethyst Star");
      n.add("Apple Bloom");
      n.add("Apple Brown Betty");
      n.add("Apple Bumpkin");
      n.add("Apple Cinnamon");
      n.add("Apple Dumpling");
      n.add("Apple Fritter");
      n.add("Apple Pie");
      n.add("Apple Rose");
      n.add("Apple Tart");
      n.add("Applejack");
      n.add("Archer");
      n.add("Arctic Lily");
      n.add("Atlas");
      n.add("Aura");
      n.add("Autumn Gem");
      n.add("Ballad");
      n.add("Baritone");
      n.add("Beauty Brass");
      n.add("Bee Bop");
      n.add("Belle Star");
      n.add("Berry Frost");
      n.add("Berry Splash");
      n.add("Big Shot");
      n.add("Big Wig");
      n.add("Black Marble");
      n.add("Black Stone");
      n.add("Blaze");
      n.add("Blossomforth");
      n.add("Blue Belle");
      n.add("Blue Bonnet");
      n.add("Bluebell");
      n.add("Bonnie");
      n.add("Boo");
      n.add("Bottlecap");
      n.add("Brown Sugar");
      n.add("Buddy");
      n.add("Bumpkin");
      n.add("Caesar");
      n.add("Calamity Mane");
      n.add("Cappuccino");
      n.add("Caramel");
      n.add("Caramel Apple");
      n.add("Castle");
      n.add("Charcoal");
      n.add("Charm");
      n.add("Cheerilee");
      n.add("Cheery");
      n.add("Cherry Berry");
      n.add("Chocolate");
      n.add("Cinnabelle");
      n.add("Cinnamon Swirl");
      n.add("Classy Clover");
      n.add("Clip Clop");
      n.add("Cloudchaser");
      n.add("Cloudy");
      n.add("Clover");
      n.add("Cobalt");
      n.add("Coconut");
      n.add("Concerto");
      n.add("Cornflower");
      n.add("Cosmic");
      n.add("Cotton ");
      n.add("Cream Puff");
      n.add("Creme Brulee");
      n.add("Crescent Moon");
      n.add("Dainty");
      n.add("Daisy");
      n.add("Derpy");
      n.add("Dinky Doo");
      n.add("Dipsy");
      n.add("Dosie Dough");
      n.add("Dr. Hooves");
      n.add("Drizzle");
      n.add("Dry Wheat");
      n.add("Dust Devil");
      n.add("Earl Grey");
      n.add("Electric Blue");
      n.add("Eliza");
      n.add("Emerald Beacon");
      n.add("Esmeralda");
      n.add("Evening Star");
      n.add("Fancy Pants");
      n.add("Felix");
      n.add("Fiddlesticks");
      n.add("Fire Streak");
      n.add("Flank Sinatra");
      n.add("Flash");
      n.add("Fleetfoot");
      n.add("Flitter");
      n.add("Flounder");
      n.add("Flurry");
      n.add("Ginger");
      n.add("Gingerbread");
      n.add("Giselle");
      n.add("Gizmo");
      n.add("Golden Glory");
      n.add("Golden Harvest");
      n.add("Goldilocks");
      n.add("Graceful Falls");
      n.add("Granny Pie");
      n.add("Graphite");
      n.add("Harry Trotter");
      n.add("Hay Fever");
      n.add("Haymish");
      n.add("Hercules");
      n.add("Hoity Toity");
      n.add("Honeycomb");
      n.add("Hope");
      n.add("Hot Wheels");
      n.add("Ivory");
      n.add("Jangles");
      n.add("Junebug");
      n.add("Knit Knot");
      n.add("Lance");
      n.add("Laurette");
      n.add("Lavender Skies");
      n.add("Lavenderhoof");
      n.add("Lemon Chiffon");
      n.add("Liberty Belle");
      n.add("Lickety Split");
      n.add("Lightning Bolt");
      n.add("Lightning Streak");
      n.add("Lily Valley");
      n.add("Lincoln");
      n.add("Long Jump");
      n.add("Lotus Blossom");
      n.add("Lucky Clover");
      n.add("Majesty");
      n.add("Marigold");
      n.add("Masquerade");
      n.add("Meadow Song");
      n.add("Melody");
      n.add("Merry May");
      n.add("Milky Way");
      n.add("Millie");
      n.add("Mochaccino");
      n.add("Moondancer");
      n.add("Nightingale");
      n.add("Nixie");
      n.add("Ocean Breeze");
      n.add("Opal Bloom");
      n.add("Orange Blossom");
      n.add("Orchid Dew");
      n.add("Orion");
      n.add("Paisley Pastel");
      n.add("Pampered Pearl");
      n.add("Paradise");
      n.add("Peachy Cream");
      n.add("Peachy Pie");
      n.add("Peachy Sweet");
      n.add("Peppermint Crunch");
      n.add("Persnickety");
      n.add("Petunia");
      n.add("Pigpen");
      n.add("Pink Lady");
      n.add("Pipsqueak");
      n.add("Pixie");
      n.add("Pound Cake");
      n.add("Primrose");
      n.add("Princess");
      n.add("Pristine");
      n.add("Professor Bill Neigh");
      n.add("Pumpkin");
      n.add("Purple Haze");
      n.add("Quake");
      n.add("Quicksilver");
      n.add("Ragtime");
      n.add("Rain Dance");
      n.add("Rainbow");
      n.add("Rainbowshine");
      n.add("Raindrops");
      n.add("Rapidfire");
      n.add("Raven");
      n.add("Red Rose");
      n.add("Riverdance");
      n.add("Rose");
      n.add("Rose Quartz");
      n.add("Rosewing");
      n.add("Roxie");
      n.add("Rumble");
      n.add("Sandstorm");
      n.add("Sapphire Rose");
      n.add("Seasong");
      n.add("Serenity");
      n.add("Shamrock");
      n.add("Shining Star");
      n.add("Shoeshine");
      n.add("Shortround");
      n.add("Sightseer");
      n.add("Silver Lining");
      n.add("Silverspeed");
      n.add("Silverwing");
      n.add("Sky");
      n.add("Slipstream");
      n.add("Smart Cookie");
      n.add("Smokestack");
      n.add("Snails");
      n.add("Snips");
      n.add("Snowflake");
      n.add("Spitfire");
      n.add("Spring Flower");
      n.add("Spring Skies");
      n.add("Squeaky Clean");
      n.add("Star Bright");
      n.add("Star Gazer");
      n.add("Starburst");
      n.add("Stardancer");
      n.add("Starlight");
      n.add("Steamer");
      n.add("Stella");
      n.add("Storm");
      n.add("Stormfeather");
      n.add("Strawberry Cream");
      n.add("Strike");
      n.add("Sugar Plum");
      n.add("Sugarberry");
      n.add("Sun Streak");
      n.add("Sunburst");
      n.add("Sunlight");
      n.add("Sunny");
      n.add("Sunset ");
      n.add("Sunstone");
      n.add("Surf");
      n.add("Surprise");
      n.add("Sweet Dreams");
      n.add("Sweet Tart");
      n.add("Sweet Tooth");
      n.add("Sweetie Belle");
      n.add("Symphony");
      n.add("Thorn");
      n.add("Thornhoof");
      n.add("Tiger Lily");
      n.add("Toastie");
      n.add("Toffee");
      n.add("Treasure");
      n.add("Twilight Sky");
      n.add("Twilight Sparkle");
      n.add("Twinkleshine");
      n.add("Twist");
      n.add("Unicorn King");
      n.add("Vera");
      n.add("Vigilance");
      n.add("Whiplash");
      n.add("Wild Fire");
      n.add("Wildwood Flower");
      n.add("Wisp");
      n.add("Yo-Yo");
      n.add("Zodiac");
      

      this.randomNameConfig.set("randomNames", n);
      try
      {
        this.randomNameConfig.save(this.randomNameFile);
      }
      catch (IOException e)
      {
        debug("Error saving name file");
      }
      randomNames = (ArrayList)this.randomNameConfig.getStringList("randomNames");
    }
  }
}
