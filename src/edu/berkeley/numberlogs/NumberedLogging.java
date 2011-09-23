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
      if(log.isFatalEnabled())
          commonsLog_fatal(log, id, msg, ex);
      else
          cached_disable(id);
      break;
    case ERROR:
      if(log.isErrorEnabled())
          commonsLog_error(log, id, msg, ex);
      else
          cached_disable(id);
      break;
    case WARN:
      if(log.isWarnEnabled())
          commonsLog_warn(log, id, msg, ex);
      else
          cached_disable(id);
      break;
    case INFO:
      if(log.isInfoEnabled())
          commonsLog_info(log, id, msg, ex);
      else
          cached_disable(id);
      break;
    case DEBUG:
      if(log.isDebugEnabled())
          commonsLog_debug(log, id, msg, ex);
      else
          cached_disable(id);
      break;
    case TRACE:
      if(log.isTraceEnabled())
          commonsLog_trace(log, id, msg, ex);
      else
          cached_disable(id);
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
