package me.raum.stables;

import com.avaje.ebean.EbeanServer;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.scheduler.BukkitScheduler;

public class EventListener
  implements Listener, Plugin
{
  Stables plugin = Stables.plugin;
  
  public void debug(String msg)
  {
    if (this.plugin.getConfig().getBoolean("general.Debug")) {
      Bukkit.getServer().getLogger().info("Stables DEBUG: " + msg);
    }
  }
  
  @EventHandler
  public void onPlayerLogin(PlayerLoginEvent event)
  {
    Player p = event.getPlayer();
    String query = "";
    if (this.plugin.getConfig().getBoolean("MySQL.useSQLite")) {
      query = 
      
        "INSERT OR IGNORE INTO " + Stables.dbprefix + "uuid ( who, uuid ) VALUES('" + p.getName().toLowerCase() + "','" + p.getUniqueId().toString() + "');" + "UPDATE " + Stables.dbprefix + "uuid SET who='" + p.getName().toLowerCase() + "' WHERE uuid LIKE '" + p.getUniqueId().toString() + "';";
    } else {
      query = 
        "INSERT INTO " + Stables.dbprefix + "uuid ( who, uuid ) VALUES('" + p.getName().toLowerCase() + "','" + p.getUniqueId().toString() + "') ON DUPLICATE KEY UPDATE who='" + p.getName().toLowerCase() + "';";
    }
    this.plugin.writeDB(query);
  }
  
  @EventHandler
  public void onVehicleExit(VehicleExitEvent event)
  {
    if (!this.plugin.isHorse(event.getVehicle())) {
      return;
    }
    Horse h = (Horse)event.getVehicle();
    if (this.plugin.HorseOwner(h.getUniqueId().toString()) != null)
    {
      this.plugin.saveLocation(h);
    }
    else if ((h.isTamed()) && ((event.getExited() instanceof Player)))
    {
      Player p = (Player)event.getExited();
      String name = this.plugin.HorseName(h.getUniqueId().toString(), null);
      if (this.plugin.getConfig().getBoolean("horses.AutoOwn"))
      {
        this.plugin.addHorse(p, h, true);
        return;
      }
      if ((name.equalsIgnoreCase("Unknown")) && (this.plugin.getConfig().getBoolean("general.showUnownedWarning")))
      {
        this.plugin.local(p, "EXIT_NOT_TAME");
        return;
      }
    }
  }
  
  @EventHandler
  public void onUnleash(EntityUnleashEvent event)
  {
    debug("Unleash: " + event.getEntity().toString() + " - " + event.getReason());
  }
  
  @EventHandler
  public void onEntityDeath(EntityDeathEvent event)
  {
    if (!this.plugin.isHorse(event.getEntity())) {
      return;
    }
    LivingEntity e = event.getEntity();
    this.plugin.removeHorse(e.getUniqueId().toString());
  }
  
  @EventHandler
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
  {
    if (!this.plugin.isHorse(event.getRightClicked()))
    {
      debug("Not a horse: " + event.getRightClicked().getClass());
      if ((event.getRightClicked() instanceof Hanging))
      {
        debug("hanging");
        Hanging h = (Hanging)event.getRightClicked();
        if ((h.getType() == EntityType.PAINTING) || (h.getType() == EntityType.ITEM_FRAME)) {
          return;
        }
       // debug(h.getUniqueId());
        return;
      }
      return;
    }
    String owner = "";
    boolean owned = false;
    LivingEntity e = (LivingEntity)event.getRightClicked();
    debug("Horse being right clicked: " + this.plugin.HorseName(null, e));
    if ((!event.getPlayer().hasPermission("stables.admin")) && (event.getPlayer().hasPermission("stables.punish.breed")) && ((event.getPlayer().getItemInHand().getTypeId() == 322) || (event.getPlayer().getItemInHand().getTypeId() == 396)))
    {
      this.plugin.local(event.getPlayer(), "PUNISH_BREED");
      event.setCancelled(true);
      return;
    }
    owner = this.plugin.HorseOwner(e.getUniqueId().toString());
    if (owner != null) {
      owned = true;
    }
    String ownername = this.plugin.HorseOwnerName(e.getUniqueId().toString());
    if ((owned) && (event.getPlayer().getItemInHand().getType() != Material.NAME_TAG) && (!owner.equals(event.getPlayer().getUniqueId().toString())) && (!this.plugin.canRide(e, event.getPlayer())))
    {
      this.plugin.msg(event.getPlayer(), this.plugin.getLang("NOT_RIDER", ownername));
      event.setCancelled(true);
      return;
    }
    if ((owned) && (event.getPlayer().getItemInHand().getType() == Material.NAME_TAG) && (!event.getPlayer().getItemInHand().getItemMeta().hasDisplayName()))
    {
      if (!owner.equals(event.getPlayer().getUniqueId().toString()))
      {
        this.plugin.local(event.getPlayer(), "NOT_OWNER");
        event.setCancelled(true);
        return;
      }
      this.plugin.local(event.getPlayer(), "SET_FREE");
      this.plugin.removeHorse(e.getUniqueId().toString());
      e.setCustomName(null);
      event.setCancelled(true);
      return;
    }
    if ((owned) && (event.getPlayer().getItemInHand().getType() == Material.NAME_TAG) && (event.getPlayer().getItemInHand().getItemMeta().hasDisplayName()) && (!owner.equals(event.getPlayer().getUniqueId().toString())))
    {
      this.plugin.local(event.getPlayer(), "NOT_OWNER");
      event.setCancelled(true);
      return;
    }
    if ((owned) && (event.getPlayer().getItemInHand().getType() == Material.NAME_TAG) && (event.getPlayer().getItemInHand().getItemMeta().hasDisplayName()) && (owner.equals(event.getPlayer().getUniqueId().toString())))
    {
      String horseName = event.getPlayer().getItemInHand().getItemMeta().getDisplayName().replace("'", "`");
      
      this.plugin.nameHorse(event.getRightClicked().getUniqueId().toString(), horseName);
      return;
    }
    if ((!owned) && (event.getPlayer().getItemInHand().getType() == Material.NAME_TAG) && (event.getPlayer().getItemInHand().getItemMeta().hasDisplayName()))
    {
      String name = event.getPlayer().getItemInHand().getItemMeta().getDisplayName();
      if ((name.contains(";")) || (name.contains("\\")))
      {
        event.getPlayer().sendMessage("That is an invalid name tag. Please rename the tag before trying to claim this horse.");
        event.setCancelled(true);
        return;
      }
      if ((event.getPlayer().hasPermission("stables.punish.name")) && (!event.getPlayer().hasPermission("stables.admin")))
      {
        this.plugin.local(event.getPlayer(), "PUNISH_NAME");
        event.setCancelled(true);
        return;
      }
      if (event.isCancelled())
      {
        debug("Event cancelled outside of Stables .. Cancelling here.");
        return;
      }
      if (!this.plugin.addHorse(event.getPlayer(), event.getRightClicked(), false)) {
        event.setCancelled(true);
      }
      return;
    }
  }
  
  @EventHandler
  public void onSplash(PotionSplashEvent event)
  {
    if ((event.getPotion() == null) || (event.getPotion().getShooter() == null)) {
      return;
    }
    Player p = null;
    boolean found = false;
    if (event.getPotion().getShooter() == null) {
      if (((event.getPotion().getShooter() instanceof Player)) && (!this.plugin.getConfig().getBoolean("general.PVPDamage"))) {
        return;
      }
    }
    if ((!(event.getPotion().getShooter() instanceof Player)) && (!this.plugin.getConfig().getBoolean("general.MobDamage"))) {
      return;
    }
    if ((event.getPotion().getShooter() instanceof Player)) {
      p = (Player)event.getPotion().getShooter();
    }
    for (LivingEntity a : event.getAffectedEntities())
    {
      found = false;
      String owner = null;
      if ((a instanceof Horse))
      {
        owner = this.plugin.HorseOwner(a.getUniqueId().toString());
        if ((this.plugin.getConfig().getBoolean("general.ProtectUnclaimed")) && (owner == null)) {
          owner = "unclaimed";
        }
        if (owner != null)
        {
          debug("Named horses ... checking splash info ...");
          if ((p == null) && (this.plugin.getConfig().getBoolean("general.MobDamage")))
          {
            debug("Blocking from mob damage");
            found = true;
          }
          if ((p != null) && (this.plugin.getConfig().getBoolean("general.PVPDamage"))) {
            if (!owner.equals(p.getUniqueId().toString()))
            {
              debug("Blocking from pvp damage");
              found = true;
            }
          }
          if ((p != null) && (owner.equals(p.getUniqueId().toString())) && (this.plugin.getConfig().getBoolean("general.OwnerDamage")))
          {
            debug("Blocking from owner damage");
            found = true;
          }
          if (found) {
            event.getAffectedEntities().remove(a);
          }
        }
      }
    }
  }
   //general.PVPMountedDamage
  @EventHandler(priority = EventPriority.HIGHEST)
	public void onDamage(EntityDamageEvent event)
	{
		if(event.getEntityType().equals(EntityType.HORSE))
		{
			Horse horse = (Horse) event.getEntity();
			if (this.plugin.getConfig().getBoolean("general.AllowPVPMountedDamage"))
					{
			if(horse.getPassenger() != null)
			{
					//if(horse.isTamed())
						event.setCancelled(false);
				}
				
				return;
	}
			}else
			{
				if(event.getEntityType().equals(EntityType.HORSE))
				{
				Horse horse = (Horse) event.getEntity();
				if(horse.getPassenger() != null)
				{
						//if(horse.isTamed())
							event.setCancelled(true);
					}
					
					return;
			}
		}
	}
  @EventHandler
  public void onEntityDamage(EntityDamageEvent event)
  {
    if (!(event.getEntity() instanceof Horse)) {
      return;
    }
    boolean owned = false;
    String owner = "";
    LivingEntity e = (LivingEntity)event.getEntity();
    owner = this.plugin.HorseOwner(e.getUniqueId().toString());
    if ((this.plugin.getConfig().getBoolean("general.ProtectUnclaimed")) && (owner == null)) {
      owner = "unclaimed";
    }
    if (owner != null) {
      owned = true;
    }
    if (!owned) {
      return;
    }
    debug("Horse beign damaged:" + this.plugin.HorseName(null, e));
    EntityDamageEvent.DamageCause cause = event.getCause();
    Entity damager = null;
    if ((cause.equals(EntityDamageEvent.DamageCause.DROWNING)) || (cause.equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) || (cause.equals(EntityDamageEvent.DamageCause.CONTACT)) || (cause.equals(EntityDamageEvent.DamageCause.FALL)) || (cause.equals(EntityDamageEvent.DamageCause.FALLING_BLOCK)) || (cause.equals(EntityDamageEvent.DamageCause.FIRE)) || (cause.equals(EntityDamageEvent.DamageCause.FIRE_TICK)) || (cause.equals(EntityDamageEvent.DamageCause.LAVA)) || (cause.equals(EntityDamageEvent.DamageCause.SUFFOCATION)))
    {
      if (this.plugin.getConfig().getBoolean("general.EnvironmentDamage"))
      {
        debug("Ennviroment onEntityDamage: Cancelled");
        event.setCancelled(true);
      }
    }
    else if (cause.equals(EntityDamageEvent.DamageCause.CUSTOM))
    {
      if ((damager instanceof Player))
      {
        Player p = (Player)damager;
        debug("Player damage from " + p.getName());
        debug("Owner is " + owner);
        if ((this.plugin.getConfig().getBoolean("general.PVPDamage")) && (!owner.equals(p.getName())))
        {
          debug("Fireball onEntityDamage: Cancelled PVP");
          event.setCancelled(true);
          return;
        }
        if ((this.plugin.getConfig().getBoolean("general.OwnerDamage")) && (owner.equals(p.getName())))
        {
          debug("Fireball onEntityDamage: Cancelled Owner");
          event.setCancelled(true);
        }
      }
      else
      {
        if (this.plugin.getConfig().getBoolean("general.MobDamage")) {
          debug("Fireball onEntityDamage: Cancelled Mob");
        }
        event.setCancelled(true);
      }
    }
    else if (cause.equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK))
    {
      debug("Entity Damage");
      return;
    }
  }
  
  @EventHandler
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
  {
    if (!(event.getEntity() instanceof Horse)) {
      return;
    }
    EntityDamageEvent.DamageCause cause = event.getCause();
    Horse t = (Horse)event.getEntity();
    String owner = "";
    boolean owned = false;
    LivingEntity e = (LivingEntity)event.getEntity();
    owner = this.plugin.HorseOwnerName(e.getUniqueId().toString());
    if ((this.plugin.getConfig().getBoolean("general.ProtectUnclaimed")) && (owner == null)) {
      owner = "unclaimed";
    }
    if (owner != null) {
      owned = true;
    }
    if (((event.getDamager() instanceof Player)) && (event.getDamager().hasMetadata("stables.checkinfo")))
    {
      Player p = (Player)event.getDamager();
      event.setCancelled(true);
      p.removeMetadata("stables.checkinfo", this.plugin);
      p.sendMessage("===============" + ChatColor.GREEN + " [" + ChatColor.DARK_GREEN + "Horse Information" + ChatColor.GREEN + "] " + ChatColor.WHITE + "===============" );
      p.sendMessage(ChatColor.GREEN + "That horse is owned by " + ChatColor.WHITE + owner);
    //  p.sendMessage(ChatColor.GREEN + "UID: " + ChatColor.WHITE + e.getUniqueId());
      p.sendMessage(ChatColor.GREEN + "Horse Name: " + ChatColor.WHITE + t.getCustomName());
      p.sendMessage(ChatColor.GREEN + "Jump Strength: " + ChatColor.WHITE + t.getJumpStrength());
      p.sendMessage(ChatColor.GREEN + "Health: " + ChatColor.WHITE + t.getHealth() + ChatColor.GREEN + "/" + ChatColor.WHITE + t.getMaxHealth());

      return;
    }
    if (((event.getDamager() instanceof Player)) && (event.getDamager().hasMetadata("stables.removeowner")))
    {
      Player p = (Player)event.getDamager();
      if (!owned)
      {
        p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "That horse is not yet owned.");
      }
      else
      {
        p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "That horse no longer has an owner.");
        this.plugin.removeHorse(e.getUniqueId().toString());
        e.setCustomName(null);
      }
      p.removeMetadata("stables.removeowner", this.plugin);
      event.setCancelled(true);
      return;
    }
    if (!owned) {
      return;
    }
    if (((event.getDamager() instanceof Player)) && (event.getDamager().hasMetadata("stables.addrider")))
    {
      Player p = (Player)event.getDamager();
      if ((!owner.equals(p.getName())) && (!this.plugin.perm(p, "stables.addrider")))
      {
        this.plugin.local(p, "NOT_OWNER");
        event.setCancelled(true);
        return;
      }
      String name = ((MetadataValue)event.getDamager().getMetadata("stables.addrider").get(0)).asString();
      String riderid = this.plugin.getUUID(name);
      if (riderid == null)
      {
        this.plugin.local(p, "RIDER_ADD_FAILED");
        p.removeMetadata("stables.addrider", this.plugin);
        event.setCancelled(true);
        return;
      }
      String query = "INSERT INTO " + this.plugin.getConfig().getString("MySQL.prefix") + "riders (uid, name, owner, owneruuid,rideruuid) VALUES('" + e.getUniqueId() + "','" + name + "','" + p.getName() + "', '" + p.getUniqueId() + "','" + riderid + "');";
      this.plugin.info(query);
      this.plugin.writeDB(query);
      this.plugin.local(p, "RIDER_ADD");
      p.removeMetadata("stables.addrider", this.plugin);
      event.setCancelled(true);
      return;
    }
    if (((event.getDamager() instanceof Player)) && (event.getDamager().hasMetadata("stables.removechest")))
    {
      Player p = (Player)event.getDamager();
      if (!owner.equals(p.getName()))
      {
        this.plugin.local(p, "NOT_OWNER");
        event.setCancelled(true);
        return;
      }
      this.plugin.local(p, "REMOVE_CHEST");
      p.removeMetadata("stables.removechest", this.plugin);
      HorseModifier hm = new HorseModifier(t);
      hm.setChested(false);
      event.setCancelled(true);
      return;
    }
    if (((event.getDamager() instanceof Player)) && (event.getDamager().hasMetadata("stables.store")))
    {
      Player p = (Player)event.getDamager();
      if ((!owner.equals(p.getName())) && (!this.plugin.perm(p, "stables.store.others")))
      {
        this.plugin.local(p, "NOT_OWNER");
        event.setCancelled(true);
        return;
      }
      HorseModifier hm = new HorseModifier(e);
      if (hm.isChested())
      {
        this.plugin.local(p, "NO_CHESTS");
        event.setCancelled(true);
        return;
      }
      this.plugin.stableHorse(e, p.getName());
      p.removeMetadata("stables.store", this.plugin);
      event.setCancelled(true);
      return;
    }
    if (((event.getDamager() instanceof Player)) && (event.getDamager().hasMetadata("stables.delrider")))
    {
      Player p = (Player)event.getDamager();
      if (!owner.equals(p.getName()))
      {
        this.plugin.local(p, "NOT_OWNER");
        event.setCancelled(true);
        return;
      }
      String name = ((MetadataValue)event.getDamager().getMetadata("stables.delrider").get(0)).asString();
      this.plugin.writeDB("DELETE FROM " + this.plugin.getConfig().getString("MySQL.prefix") + "riders WHERE uid='" + t.getUniqueId() + "' AND name='" + name + "'");
      this.plugin.local(p, "RIDER_DEL");
      
      p.removeMetadata("stables.delrider", this.plugin);
      event.setCancelled(true);
      return;
    }
    debug("Horse being damaged:" + this.plugin.HorseName(null, e));
    if (this.plugin.getConfig().getBoolean("general.BlockAll"))
    {
      debug("Cancelling damage - Blocking All Damage");
      event.setCancelled(true);
      return;
    }
    if (cause.equals(EntityDamageEvent.DamageCause.PROJECTILE))
    {
      debug("Projectile Damage");
      Entity damager = event.getDamager();
      debug("Damager: " + damager);
      if ((damager instanceof Fireball))
      {
        Fireball arrow = (Fireball)damager;
        if ((arrow.getShooter() instanceof Player))
        {
          Player p = (Player)arrow.getShooter();
          if ((this.plugin.getConfig().getBoolean("general.PVPDamage")) && (!owner.equals(p.getName())))
          {
            debug("Fireball onEntityDamage: Cancelled PVP");
            event.setCancelled(true);
            return;
          }
          if ((this.plugin.getConfig().getBoolean("general.OwnerDamage")) && (owner.equals(p.getName())))
          {
            debug("Fireball onEntityDamage: Cancelled Owner");
            event.setCancelled(true);
          }
        }
        else
        {
          if (this.plugin.getConfig().getBoolean("general.MobDamage")) {
            debug("Fireball onEntityDamage: Cancelled Mob");
          }
          event.setCancelled(true);
        }
      }
      if ((damager instanceof Arrow))
      {
        debug("Arrow info!");
        Arrow arrow = (Arrow)damager;
        if ((arrow.getShooter() instanceof Player))
        {
          debug("Player shot arrow");
          Player p = (Player)arrow.getShooter();
          if ((this.plugin.getConfig().getBoolean("general.PVPDamage")) && (!owner.equals(p.getName())))
          {
            debug("Fireball onEntityDamage: Cancelled PVP");
            event.setCancelled(true);
            return;
          }
          if ((this.plugin.getConfig().getBoolean("general.OwnerDamage")) && (owner.equals(p.getName())))
          {
            debug("Fireball onEntityDamage: Cancelled Owner");
            event.setCancelled(true);
          }
        }
        else
        {
          if (this.plugin.getConfig().getBoolean("general.MobDamage")) {
            debug("Fireball onEntityDamage: Cancelled Mob");
          }
          event.setCancelled(true);
        }
      }
    }
    else
    {
      if ((this.plugin.getConfig().getBoolean("general.MobDamage")) && (!(event.getDamager() instanceof Player)))
      {
        debug("Cancelling damage - Mob");
        event.setCancelled(true);
        return;
      }
      if ((event.getDamager() instanceof Player))
      {
        Player p = (Player)event.getDamager();
        if ((this.plugin.getConfig().getBoolean("general.PVPDamage")) && (!owner.equals(p.getName())))
        {
          debug("Cancelling damage - PVP");
          event.setCancelled(true);
          return;
        }
        if ((this.plugin.getConfig().getBoolean("general.OwnerDamage")) && (owner.equals(p.getName())))
        {
          debug("Cancelling damage - Owner Damage");
          event.setCancelled(true);
          return;
        }
      }
    }
    debug("No damage issues - letting damage go through. " + event.getDamage());
    event.setCancelled(false);
  }
  
  @EventHandler
  public void onPlayerInteractBlock(PlayerInteractEvent event)
  {
    if (!this.plugin.getConfig().getBoolean("horses.lure.allow")) {
      return;
    }
    if (event.getPlayer().hasMetadata("stables.luring"))
    {
      this.plugin.local(event.getPlayer(), "ALREADY_LURE");
      return;
    }
    if (!event.getPlayer().isSneaking()) {
      return;
    }
    int itemId = event.getPlayer().getItemInHand().getTypeId();
    if (!this.plugin.getConfig().contains("lure." + itemId + ".chance")) {
      return;
    }
    int amt = event.getPlayer().getItemInHand().getAmount() - 1;
    event.getPlayer().getItemInHand().setAmount(amt);
    debug("Amt remainnig: " + amt);
    if (amt == 0)
    {
      ItemStack itemstack = new ItemStack(this.plugin.getConfig().getInt("horses.lure.item"));
      event.getPlayer().getInventory().removeItem(new ItemStack[] {
        itemstack });
      
      debug("Removing item stack...");
    }
    this.plugin.local(event.getPlayer(), "START_LURE");
    int stime = this.plugin.getConfig().getInt("horses.lure.delay") * 20;
    event.getPlayer().setMetadata("stables.luring", new FixedMetadataValue(this.plugin, Integer.valueOf(itemId)));
    final String name = event.getPlayer().getName();
    
    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable()
    {
      public void run()
      {
        EventListener.this.plugin.lureHorse(name);
      }
    }, stime);
  }
  
  @EventHandler
  public void onCraftItem(CraftItemEvent event)
  {
    if (!this.plugin.getConfig().getBoolean("recipe.usePerms")) {
      return;
    }
    int id = event.getRecipe().getResult().getType().getId();
    String perm = "";
    Player p = (Player)event.getView().getPlayer();
    switch (id)
    {
    default: 
      return;
    case 421: 
      perm = "stables.recipe.nametag";
      break;
    case 417: 
      perm = "stables.recipe.armor.iron";
      break;
    case 418: 
      perm = "stables.recipe.armor.gold";
      break;
    case 419: 
      perm = "stables.recipe.armor.diamond";
      break;
    case 329: 
      perm = "stables.recipe.saddle";
    }
    debug("Checking perm " + perm);
    if (!this.plugin.perm(p, perm))
    {
      this.plugin.local(p, "RECIPE_PERM");
      event.setCancelled(true);
      return;
    }
    debug("Pass! Crafting");
  }
  
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event)
  {
    if (event.getClickedBlock() == null) {
      return;
    }
    Player p = event.getPlayer();
    if ((event.getClickedBlock().getType() == Material.WALL_SIGN) || (event.getClickedBlock().getType() == Material.SIGN_POST))
    {
      debug("Checking signs!");
      Sign sign = (Sign)event.getClickedBlock().getState();
      String stablesign = ChatColor.stripColor(sign.getLine(0));
      if (stablesign.equals("[Stables]")) {
        this.plugin.commandStore(p);
      }
      return;
    }
  }
  
  @EventHandler
  public void onSignChange(SignChangeEvent event)
  {
    String stableSign = event.getLine(0);
    if (!stableSign.equalsIgnoreCase("[stables]")) {
      return;
    }
    Block block = event.getBlock();
    Player p = event.getPlayer();
    boolean typeWallSign = block.getType() == Material.WALL_SIGN;
    boolean typeSignPost = block.getType() == Material.SIGN_POST;
    if ((typeWallSign) || (typeSignPost))
    {
      Sign sign = (Sign)block.getState();
      if (!this.plugin.perm(p, "stables.sign"))
      {
        p.sendMessage(this.plugin.getLang("NO_PERM", null));
        event.setLine(0, p.getName());
        event.setLine(1, "is");
        event.setLine(2, "naughty.");
        event.setLine(3, "");
        sign.update(true);
        return;
      }
      p.sendMessage(ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "Stables" + ChatColor.GREEN + "] " + ChatColor.WHITE + "Stables created!");
      event.setLine(0, "&9[&0Stables&9]");
      event.setLine(0, event.getLine(0).replaceAll("&([0-9A-Fa-f])", "§$1"));
      event.setLine(1, "");
      event.setLine(2, this.plugin.getLang("STABLES_RECOVERSIGN1", null));
      event.setLine(3, this.plugin.getLang("STABLES_RECOVERSIGN2", null));
      sign.update(true);
      return;
    }
  }
  
  @EventHandler(ignoreCancelled=true)
  public void onInteractBlock(PlayerInteractEvent event)
  {
    if ((event.getPlayer() == null) || (event.getClickedBlock() == null) || (event.getItem() == null)) {
      return;
    }
    if (!this.plugin.getConfig().getBoolean("items.deconstruct.allow")) {
      return;
    }
    Player p = event.getPlayer();
    Block b = event.getClickedBlock();
    ItemStack item = event.getItem();
    int itemid = item.getTypeId();
    boolean found = false;
    Material anvil = Material.getMaterial(this.plugin.getConfig().getString("items.deconstruct.item"));
    if (b.getType() != anvil) {
      return;
    }
    switch (itemid)
    {
    default: 
      return;
    }
  }
  
  @EventHandler
  public void onInventoryClick(InventoryClickEvent e) {}
  
  public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3)
  {
    return null;
  }
  
  public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3)
  {
    return false;
  }
  
  public FileConfiguration getConfig()
  {
    return null;
  }
  
  public File getDataFolder()
  {
    return null;
  }
  
  public EbeanServer getDatabase()
  {
    return null;
  }
  
  public ChunkGenerator getDefaultWorldGenerator(String arg0, String arg1)
  {
    return null;
  }
  
  public PluginDescriptionFile getDescription()
  {
    return null;
  }
  
  public Logger getLogger()
  {
    return null;
  }
  
  public String getName()
  {
    return null;
  }
  
  public PluginLoader getPluginLoader()
  {
    return null;
  }
  
  public InputStream getResource(String arg0)
  {
    return null;
  }
  
  public Server getServer()
  {
    return null;
  }
  
  public boolean isEnabled()
  {
    return false;
  }
  
  public boolean isNaggable()
  {
    return false;
  }
  
  public void onDisable() {}
  
  public void onEnable() {}
  
  public void onLoad() {}
  
  public void reloadConfig() {}
  
  public void saveConfig() {}
  
  public void saveDefaultConfig() {}
  
  public void saveResource(String arg0, boolean arg1) {}
  
  public void setNaggable(boolean arg0) {}
}