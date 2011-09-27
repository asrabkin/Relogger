/*
 * Copyright (c) 2011 Ariel Rabkin 
 * All rights reserved.
 */
package edu.berkeley.numberlogs;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.log4j.*;

public class NumberedLogging {

    //concurrently accessible.
  static volatile BitSet cachedMaskTable = new BitSet();
  static ConcurrentHashMap<Integer, LEVS> warnLevels = new ConcurrentHashMap<Integer, LEVS>();

    //should be protected by a lock on the class.
  static BitSet userDisabled = new BitSet();
  static BitSet userEnabled = new BitSet();
  static BitSet printedOnce = new BitSet(); //this is undefined if user-enabled.

  static final boolean ALWAYS_PRINT_ONCE = false;

  static class FlexiLevel extends Level {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public FlexiLevel(String levelStr, int syslogEquivalent) {
      super(0, levelStr, syslogEquivalent);
    }
    
  }


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

  static synchronized void updateUser(int i, boolean isDisabled) {
    //Updates both user and cached map table
    userDisabled.set(i, isDisabled);
    rebuildCachedDisable();
  }
  
  static void rebuildCachedDisable() {
    int newLen = cachedMaskTable.size();

    BitSet newTable = new BitSet(newLen);
    newTable.or(userDisabled);
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
    else longer_logmsg_Log4j(id, original_methname, log, msg, ex);
  }

  //Split from above because JVMs don't always inline long methods.
  private static void longer_logmsg_Log4j(int id, String original_methname, Logger log, Object msg, Throwable ex) {

    Level level = Level.toLevel(original_methname);
    boolean legacyEnabled = log.isEnabledFor(level);
    if(shouldPrint(id, legacyEnabled)) {
      
      /*LEVS methname = getWarnLevel(id);
      if(methname == null)
        methname = setMeth(id, original_methname);
      Level level = Level.toLevel(methname.toString());*/

      String logname = log.getName();
/*      StringBuilder sb = new StringBuilder(logname.length() + 10);
      sb.append(logname);
      sb.append(" (");
      sb.append(id);
      sb.append(")");
      logname = sb.toString();*/
      log.callAppenders(
           new org.apache.log4j.spi.LoggingEvent(logname, log, level, msg, ex));
    }
  }

  private static synchronized boolean shouldPrint(int id, boolean legacyEnabled) {
    if(userEnabled.get(id))
      return true;
    
    if(!legacyEnabled) {
        //cache disable
      int newLen = cachedMaskTable.size();
      if (id > newLen)
        newLen = id;
      BitSet newTable = new BitSet(newLen);
      newTable.or(cachedMaskTable);
      newTable.set(id);
      cachedMaskTable = newTable;
      
      boolean printedBefore = !ALWAYS_PRINT_ONCE | printedOnce.get(id);
      if(!printedBefore) {
        printedOnce.set(id);
      }
       return !printedBefore; //print if this was first time
    }
    return true; //user-disable was merged into the cache-disable mask
  }

  public static void logmsg(int id, String original_methname, org.apache.commons.logging.Log log, Object msg, Throwable ex) {
    if(cachedMaskTable.get(id))
      return;
    else longer_logmsg(id, original_methname, log, msg, ex);
  }


  public static void longer_logmsg(int id, String original_methname, org.apache.commons.logging.Log log, Object msg, Throwable ex) {

    LEVS methname = getWarnLevel(id);
    if(methname == null)
      methname = setMeth(id, original_methname);

    switch(methname) {
    case FATAL:
      if(shouldPrint(id, log.isFatalEnabled()))
          commonsLog_fatal(log, id, msg, ex);
      break;
    case ERROR:
      if(shouldPrint(id, log.isErrorEnabled()))
          commonsLog_error(log, id, msg, ex);
      break;
    case WARN:
      if(shouldPrint(id, log.isWarnEnabled()))
          commonsLog_warn(log, id, msg, ex);
      break;
    case INFO:
      if(shouldPrint(id, log.isInfoEnabled()))
          commonsLog_info(log, id, msg, ex);
      break;
    case DEBUG:
      if(shouldPrint(id, log.isDebugEnabled()))
          commonsLog_debug(log, id, msg, ex);
      break;
    case TRACE:
      if(shouldPrint(id, log.isTraceEnabled()))
          commonsLog_trace(log, id, msg, ex);
      break;
  }
    //    System.out.println(reformatArray(args));
  }
  public static void commonsLog_fatal(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.fatal("(" + id + ") " +msg);
    else
      log.fatal("(" + id + ") " +msg, ex);
  }
  public static void commonsLog_error(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.error("(" + id + ") " +msg);
    else
      log.error("(" + id + ") " +msg, ex);
  }
  public static void commonsLog_warn(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.warn("(" + id + ") " +msg);
    else
      log.warn("(" + id + ") " +msg, ex);
  }
  public static void commonsLog_info(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.info("(" + id + ") " +msg);
    else
      log.info("(" + id + ") " +msg, ex);
  }
  public static void commonsLog_debug(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.debug("(" + id + ") " +msg);
    else
      log.debug("(" + id + ") " +msg, ex);
  }
  public static void commonsLog_trace(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.trace("(" + id + ") " +msg);
    else
      log.trace("(" + id + ") " +msg, ex);
  }


}
