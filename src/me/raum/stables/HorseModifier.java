package me.raum.stables;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HorseModifier
{
  private Object entityHorse;
  private Object nbtTagCompound;
  
  public HorseModifier(LivingEntity horse)
  {
    if (!isHorse(horse)) {
      throw new IllegalArgumentException("Entity has to be a horse!");
    }
    try
    {
      this.entityHorse = ReflectionUtil.getMethod("getHandle", horse.getClass(), 0).invoke(horse, new Object[0]);
      this.nbtTagCompound = NBTUtil.getNBTTagCompound(this.entityHorse);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  private HorseModifier(Object entityHorse)
  {
    this.entityHorse = entityHorse;
    try
    {
      this.nbtTagCompound = NBTUtil.getNBTTagCompound(entityHorse);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public static HorseModifier spawn(Location loc)
  {
    World w = loc.getWorld();
    try
    {
      Object worldServer = ReflectionUtil.getMethod("getHandle", w.getClass(), 0).invoke(w, new Object[0]);
      Object entityHorse = ReflectionUtil.getClass("EntityHorse", new Object[] { worldServer });
      ReflectionUtil.getMethod("setPosition", entityHorse.getClass(), 3).invoke(entityHorse, new Object[] { Double.valueOf(loc.getX()), Double.valueOf(loc.getY()), Double.valueOf(loc.getZ()) });
      ReflectionUtil.getMethod("addEntity", worldServer.getClass(), 1).invoke(worldServer, new Object[] { entityHorse });
      return new HorseModifier(entityHorse);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  public static boolean isHorse(LivingEntity le)
  {
    try
    {
      Object entityLiving = ReflectionUtil.getMethod("getHandle", le.getClass(), 0).invoke(le, new Object[0]);
      Object nbtTagCompound = NBTUtil.getNBTTagCompound(entityLiving);
      return NBTUtil.hasKeys(nbtTagCompound, new String[] { "EatingHaystack", "ChestedHorse", "HasReproduced", "Bred", "Type", "Variant", "Temper", "Tame" });
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return false;
  }
  
  public void setType(HorseType type)
  {
    setHorseValue("Type", Integer.valueOf(type.getId()));
  }
  
  public void setChested(boolean chested)
  {
    setHorseValue("ChestedHorse", Boolean.valueOf(chested));
  }
  
  public void setEating(boolean eating)
  {
    setHorseValue("EatingHaystack", Boolean.valueOf(eating));
  }
  
  public void setBred(boolean bred)
  {
    setHorseValue("Bred", Boolean.valueOf(bred));
  }
  
  public void setVariant(HorseVariant variant)
  {
    setHorseValue("Variant", Integer.valueOf(variant.getId()));
  }
  
  public void setTemper(int temper)
  {
    setHorseValue("Temper", Integer.valueOf(temper));
  }
  
  public void setTamed(boolean tamed)
  {
    setHorseValue("Tame", Boolean.valueOf(tamed));
  }
  
  public void setSaddled(boolean saddled)
  {
    setHorseValue("Saddle", Boolean.valueOf(saddled));
  }
  
  public void setArmorItem(ItemStack i)
  {
    if (i != null) {
      try
      {
        Object itemTag = ReflectionUtil.getClass("NBTTagCompound", new Object[] { "ArmorItem" });
        Object itemStack = ReflectionUtil.getMethod("asNMSCopy", Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack"), 1).invoke(this, new Object[] { i });
        ReflectionUtil.getMethod("save", itemStack.getClass(), 1).invoke(itemStack, new Object[] { itemTag });
        setHorseValue("ArmorItem", itemTag);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    } else {
      setHorseValue("ArmorItem", null);
    }
  }
  
  public HorseType getType()
  {
    return HorseType.fromId(((Integer)NBTUtil.getValue(this.nbtTagCompound, Integer.class, "Type")).intValue());
  }
  
  public boolean isChested()
  {
    return ((Boolean)NBTUtil.getValue(this.nbtTagCompound, Boolean.class, "ChestedHorse")).booleanValue();
  }
  
  public boolean isEating()
  {
    return ((Boolean)NBTUtil.getValue(this.nbtTagCompound, Boolean.class, "EatingHaystack")).booleanValue();
  }
  
  public boolean isBred()
  {
    return ((Boolean)NBTUtil.getValue(this.nbtTagCompound, Boolean.class, "Bred")).booleanValue();
  }
  
  public HorseVariant getVariant()
  {
    return HorseVariant.fromId(((Integer)NBTUtil.getValue(this.nbtTagCompound, Integer.class, "Variant")).intValue());
  }
  
  public int getTemper()
  {
    return ((Integer)NBTUtil.getValue(this.nbtTagCompound, Integer.class, "Temper")).intValue();
  }
  
  public boolean isTamed()
  {
    return ((Boolean)NBTUtil.getValue(this.nbtTagCompound, Boolean.class, "Tame")).booleanValue();
  }
  
  public boolean isSaddled()
  {
    return ((Boolean)NBTUtil.getValue(this.nbtTagCompound, Boolean.class, "Saddle")).booleanValue();
  }
  
  public ItemStack getArmorItem()
  {
    try
    {
      Object itemTag = NBTUtil.getValue(this.nbtTagCompound, this.nbtTagCompound.getClass(), "ArmorItem");
      Object itemStack = ReflectionUtil.getMethod("createStack", Class.forName(ReflectionUtil.getPackageName() + ".ItemStack"), 1).invoke(this, new Object[] { itemTag });
      return (ItemStack)ReflectionUtil.getMethod("asCraftMirror", Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack"), 1).invoke(this, new Object[] { itemStack });
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  public void openInventory(Player p)
  {
    try
    {
      Object entityPlayer = ReflectionUtil.getMethod("getHandle", p.getClass(), 0).invoke(p, new Object[0]);
      ReflectionUtil.getMethod("f", this.entityHorse.getClass(), 1).invoke(this.entityHorse, new Object[] { entityPlayer });
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public LivingEntity getHorse()
  {
    try
    {
      return (LivingEntity)ReflectionUtil.getMethod("getBukkitEntity", this.entityHorse.getClass(), 0).invoke(this.entityHorse, new Object[0]);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  private void setHorseValue(String key, Object value)
  {
    NBTUtil.setValue(this.nbtTagCompound, key, value);
    NBTUtil.updateNBTTagCompound(this.entityHorse, this.nbtTagCompound);
  }
  
  public static enum HorseType
  {
    NORMAL("normal", 0),  DONKEY("donkey", 1),  MULE("mule", 2),  UNDEAD("undead", 3),  SKELETAL("skeletal", 4);
    
    private String name;
    private int id;
    private static final Map<String, HorseType> NAME_MAP;
    private static final Map<Integer, HorseType> ID_MAP;
    
    private HorseType(String name, int id)
    {
      this.name = name;
      this.id = id;
    }
    
    public String getName()
    {
      return this.name;
    }
    
    public int getId()
    {
      return this.id;
    }
    
    static
    {
      NAME_MAP = new HashMap();
      ID_MAP = new HashMap();
      for (HorseType effect : values())
      {
        NAME_MAP.put(effect.name, effect);
        ID_MAP.put(Integer.valueOf(effect.id), effect);
      }
    }
    
    public static HorseType fromName(String name)
    {
      if (name == null) {
        return null;
      }
      for (Map.Entry<String, HorseType> e : NAME_MAP.entrySet()) {
        if (((String)e.getKey()).equalsIgnoreCase(name)) {
          return (HorseType)e.getValue();
        }
      }
      return null;
    }
    
    public static HorseType fromId(int id)
    {
      return (HorseType)ID_MAP.get(Integer.valueOf(id));
    }
  }
  
  public static enum HorseVariant
  {
    WHITE("white", 0),  CREAMY("creamy", 1),  CHESTNUT("chestnut", 2),  BROWN("brown", 3),  BLACK("black", 4),  GRAY("gray", 5),  DARK_BROWN("dark brown", 6),  INVISIBLE("invisible", 7),  WHITE_WHITE(
      "white-white", 256),  CREAMY_WHITE("creamy-white", 257),  CHESTNUT_WHITE("chestnut-white", 258),  BROWN_WHITE("brown-white", 259),  BLACK_WHITE("black-white", 260),  GRAY_WHITE("gray-white", 261),  DARK_BROWN_WHITE(
      "dark brown-white", 262),  WHITE_WHITE_FIELD("white-white field", 512),  CREAMY_WHITE_FIELD("creamy-white field", 513),  CHESTNUT_WHITE_FIELD("chestnut-white field", 514),  BROWN_WHITE_FIELD(
      "brown-white field", 515),  BLACK_WHITE_FIELD("black-white field", 516),  GRAY_WHITE_FIELD("gray-white field", 517),  DARK_BROWN_WHITE_FIELD("dark brown-white field", 518),  WHITE_WHITE_DOTS(
      "white-white dots", 768),  CREAMY_WHITE_DOTS("creamy-white dots", 769),  CHESTNUT_WHITE_DOTS("chestnut-white dots", 770),  BROWN_WHITE_DOTS("brown-white dots", 771),  BLACK_WHITE_DOTS(
      "black-white dots", 772),  GRAY_WHITE_DOTS("gray-white dots", 773),  DARK_BROWN_WHITE_DOTS("dark brown-white dots", 774),  WHITE_BLACK_DOTS("white-black dots", 1024),  CREAMY_BLACK_DOTS(
      "creamy-black dots", 1025),  CHESTNUT_BLACK_DOTS("chestnut-black dots", 1026),  BROWN_BLACK_DOTS("brown-black dots", 1027),  BLACK_BLACK_DOTS("black-black dots", 1028),  GRAY_BLACK_DOTS(
      "gray-black dots", 1029),  DARK_BROWN_BLACK_DOTS("dark brown-black dots", 1030);
    
    private String name;
    private int id;
    private static final Map<String, HorseVariant> NAME_MAP;
    private static final Map<Integer, HorseVariant> ID_MAP;
    
    private HorseVariant(String name, int id)
    {
      this.name = name;
      this.id = id;
    }
    
    public String getName()
    {
      return this.name;
    }
    
    public int getId()
    {
      return this.id;
    }
    
    static
    {
      NAME_MAP = new HashMap();
      ID_MAP = new HashMap();
      for (HorseVariant effect : values())
      {
        NAME_MAP.put(effect.name, effect);
        ID_MAP.put(Integer.valueOf(effect.id), effect);
      }
    }
    
    public static HorseVariant fromName(String name)
    {
      if (name == null) {
        return null;
      }
      for (Map.Entry<String, HorseVariant> e : NAME_MAP.entrySet()) {
        if (((String)e.getKey()).equalsIgnoreCase(name)) {
          return (HorseVariant)e.getValue();
        }
      }
      return null;
    }
    
    public static HorseVariant fromId(int id)
    {
      return (HorseVariant)ID_MAP.get(Integer.valueOf(id));
    }
  }
  
  private static class NBTUtil
  {
    public static Object getNBTTagCompound(Object entity)
    {
      try
      {
        Object nbtTagCompound = HorseModifier.ReflectionUtil.getClass("NBTTagCompound", new Object[0]);
        for (Method m : entity.getClass().getMethods())
        {
          Class[] pt = m.getParameterTypes();
          if ((m.getName().equals("b")) && (pt.length == 1) && (pt[0].getName().contains("NBTTagCompound"))) {
            m.invoke(entity, new Object[] { nbtTagCompound });
          }
        }
        return nbtTagCompound;
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      return null;
    }
    
    public static void updateNBTTagCompound(Object entity, Object nbtTagCompound)
    {
      try
      {
        for (Method m : entity.getClass().getMethods())
        {
          Class[] pt = m.getParameterTypes();
          if ((m.getName().equals("a")) && (pt.length == 1) && (pt[0].getName().contains("NBTTagCompound"))) {
            m.invoke(entity, new Object[] { nbtTagCompound });
          }
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    
    public static void setValue(Object nbtTagCompound, String key, Object value)
    {
      try
      {
        if ((value instanceof Integer))
        {
          HorseModifier.ReflectionUtil.getMethod("setInt", nbtTagCompound.getClass(), 2).invoke(nbtTagCompound, new Object[] { key, (Integer)value });
          return;
        }
        if ((value instanceof Boolean))
        {
          HorseModifier.ReflectionUtil.getMethod("setBoolean", nbtTagCompound.getClass(), 2).invoke(nbtTagCompound, new Object[] { key, (Boolean)value });
          return;
        }
        HorseModifier.ReflectionUtil.getMethod("set", nbtTagCompound.getClass(), 2).invoke(nbtTagCompound, new Object[] { key, value });
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    
    public static Object getValue(Object nbtTagCompound, Class<?> c, String key)
    {
      try
      {
        if (c == Integer.class) {
          return HorseModifier.ReflectionUtil.getMethod("getInt", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, new Object[] { key });
        }
        if (c == Boolean.class) {
          return HorseModifier.ReflectionUtil.getMethod("getBoolean", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, new Object[] { key });
        }
        return HorseModifier.ReflectionUtil.getMethod("getCompound", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, new Object[] { key });
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      return null;
    }
    
    public static boolean hasKey(Object nbtTagCompound, String key)
    {
      try
      {
        return ((Boolean)HorseModifier.ReflectionUtil.getMethod("hasKey", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, new Object[] { key })).booleanValue();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      return false;
    }
    
    public static boolean hasKeys(Object nbtTagCompound, String[] keys)
    {
      for (String key : keys) {
        if (!hasKey(nbtTagCompound, key)) {
          return false;
        }
      }
      return true;
    }
  }
  
  private static class ReflectionUtil
  {
    public static Object getClass(String name, Object... args)
      throws Exception
    {
      Class<?> c = Class.forName(getPackageName() + "." + name);
      int params = 0;
      if (args != null) {
        params = args.length;
      }
      for (Constructor<?> co : c.getConstructors()) {
        if (co.getParameterTypes().length == params) {
          return co.newInstance(args);
        }
      }
      return null;
    }
    
    public static Method getMethod(String name, Class<?> c, int params)
    {
      for (Method m : c.getMethods()) {
        if ((m.getName().equals(name)) && (m.getParameterTypes().length == params)) {
          return m;
        }
      }
      return null;
    }
    
    public static String getPackageName()
    {
      return "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }
  }
}
