package edu.berkeley.numberlogs;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.*;

public class NumberedLogging {

  static volatile BitSet cachedMaskTable = new BitSet();
  static BitSet userMaskTable = new BitSet();

  static ConcurrentHashMap<Integer, LEVS> warnLevels = new ConcurrentHashMap<Integer, LEVS>();


  public enum LEVS {FATAL, ERROR, WARN, INFO, DEBUG,TRACE, UNKNOWN};

  public static LEVS levNameToLevel(String methname) {
    if(methname.equals("fatal"))
      return LEVS.FATAL;
    else if(methname.equals("error"))
      return LEVS.ERROR;
    else if(methname.equals("warn"))
      return LEVS.WARN;
    else if(methname.equals("info"))
      return LEVS.INFO;
    else if(methname.equals("debug"))
      return LEVS.DEBUG;
    else if(methname.equals("trace"))
      return LEVS.TRACE;
    else
      return LEVS.UNKNOWN;
  }


  static boolean isDisabled(int globalID) {
    return cachedMaskTable.get(globalID);
  }

  static synchronized void update(int i, boolean isDisabled) {
    //Updates both user and cached map table

    userMaskTable.set(i, isDisabled);

    int newLen = cachedMaskTable.size();
    if (i > newLen)
      newLen = i;

    BitSet newTable = new BitSet(newLen);
    newTable.or(cachedMaskTable);
    newTable.set(i, isDisabled);
    cachedMaskTable = newTable;
  }


  public static synchronized void cached_disable(int i) {
    //    System.out.println("cache-disable " + i);
    int newLen = cachedMaskTable.size();
    if (i > newLen)
      newLen = i;
    BitSet newTable = new BitSet(newLen);
    newTable.or(cachedMaskTable);
    newTable.set(i);
    cachedMaskTable = newTable;
  }

  public static LEVS setMeth(int line, String methname) {
    LEVS lev = levNameToLevel(methname);
    warnLevels.put(line, lev);
    return lev;
  }


  private static LEVS getWarnLevel(int id) {
    LEVS l = warnLevels.get(id);
    /*
   if(l == null)
      return LEVS.UNKNOWN;
    else*/
    return l;
    //    return LEVS.TRACE;
  }

  public static String reformatArray(Object[] arr) {

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    for(Object a : arr) {
      if(a != null)
        sb.append(a.toString());
      else 
        sb.append("null");
      sb.append(",");
    }
    if(sb.length() > 0)
      sb.deleteCharAt(sb.length() -1);
    sb.append("}");
    return sb.toString();
  }

  public static void logmsg(int id, String original_methname, Logger log, Object msg, Throwable ex) {
    if(cachedMaskTable.get(id))
      return;
    else longer_logmsg(id, original_methname, log, msg, ex);
  }

  //Split from above because JVMs don't always inline long methods.
  private static void longer_logmsg(int id, String original_methname, Logger log, Object msg, Throwable ex) {
    

    LEVS methname = getWarnLevel(id);
    if(methname == null)
      methname = setMeth(id, original_methname);
    
    Level level = Level.toLevel(methname.toString());
    if(log.isEnabledFor(level))
      if(ex == null)
        log.log(level, "(" + id + ") " +msg);
      else
        log.log(level, "(" + id + ") " +msg, ex);
    else
      cached_disable(id);
    //   System.out.println("cache-miss, doing long resolve");

  }



  public static void logmsg(int id, String original_methname, org.apache.commons.logging.Log log, Object[] args) {

    if(isDisabled(id))
      return;

    LEVS methname = warnLevels.get(id);
    if(methname == null)
      methname = setMeth(id, original_methname);

    if(methname.equals("fatal")) {
      if(args.length == 1) 
        log.fatal("(" + id + ") " +args[0]);
      else
        log.fatal("(" + id + ") " +args[0], (Throwable) args[1]);
    } else if(methname.equals("error")) {
      if(args.length == 1) 
        log.error("(" + id + ") " +args[0]);
      else
        log.error("(" + id + ") " +args[0], (Throwable) args[1]);
    } else if(methname.equals("warn")) {
      if(args.length == 1) 
        log.warn("(" + id + ") " +args[0]);
      else
        log.warn("(" + id + ") " +args[0], (Throwable) args[1]);
    } else if(methname.equals("info")) {
      if(args.length == 1) 
        log.info("(" + id + ") " +args[0]);
      else
        log.info("(" + id + ") " +args[0], (Throwable) args[1]);
    } else if(methname.equals("debug")) {
      if(args.length == 1) 
        log.debug("(" + id + ") " +args[0]);
      else
        log.debug("(" + id + ") " +args[0], (Throwable) args[1]);
    } else if(methname.equals("trace")) {
      if(log.isTraceEnabled())
        if(args.length == 1) 
          log.trace("(" + id + ") " +args[0]);
        else
          log.trace("(" + id + ") " +args[0], (Throwable) args[1]);
    } else {
      log.info(methname + "(" + id  + ") " +args[0]);
    }

    //    System.out.println(reformatArray(args));
  }

}
