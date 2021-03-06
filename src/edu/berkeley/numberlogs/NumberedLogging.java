/*
 * Copyright (c) 2011 Ariel Rabkin 
 * All rights reserved.
 */
package edu.berkeley.numberlogs;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import edu.berkeley.numberlogs.IDMapper.StmtInfo;

public class NumberedLogging {

    //concurrently accessible.
  private static volatile BitSet cachedMaskTable = new BitSet();
//  protected static volatile boolean[] cachedMaskTable = new boolean[10];
  static ConcurrentHashMap<Integer, LEVS> warnLevels = new ConcurrentHashMap<Integer, LEVS>();

    //should be protected by a lock on the class.
  static BitSet userDisabled = new BitSet();
  static BitSet userEnabled = new BitSet();
  static BitSet printedOnce = new BitSet(); //this is undefined if user-enabled.
  static IDMapper mapper;

  static boolean ALWAYS_PRINT_ONCE = false;
  static boolean RECORD_ONCE = true;

  
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
  

  protected static boolean cachedMask(int id) {
    return cachedMaskTable.get(id);
  }

  static synchronized void updateUser(int[] stmtIDs, boolean isDisabled) {
    //Updates both user and cached map table
    for(int i : stmtIDs) {
      userDisabled.set(i, isDisabled);
      userEnabled.set(i, !isDisabled);
    }
    changeCacheDisable(stmtIDs, isDisabled);
  }

  /**
   * Clear both user-enabled and user-disabled flags
   */
  static synchronized void clearUser(int[] stmtIDs) {
    //Updates both user and cached map table
    for(int i: stmtIDs) {
      userDisabled.set(i, false);
      userEnabled.set(i, false);
    }
    changeCacheDisable(stmtIDs, false);
  }

  public static synchronized void clearPrintedOnce(int[] stmtIDs) {
    
    for(int id : stmtIDs) {
      printedOnce.clear(id);
    }
    changeCacheDisable(stmtIDs, false);
  }
  


  public static synchronized void clearAllPrintedOnce() {
    printedOnce.clear();
    changeCacheDisable(0, false);
  }

  /**
   * Change a bit in the cachedMaskTable, recopying table.
   * CALLER LOCKS
   * @param id
   * @param newVal
   */
  private static void changeCacheDisable(int id, boolean newVal) {

    int newLen = Math.max(id, cachedMaskTable.size());
    BitSet newTable = new BitSet(newLen);
    newTable.or(cachedMaskTable);
    newTable.set(id, newVal);
    cachedMaskTable = newTable;
  }
  
  private static void changeCacheDisable(int[] ids, boolean newVal) {

    int newLen = cachedMaskTable.size();
    for(int i: ids)
        if(i >= newLen)
            newLen = i + 1;
    BitSet newTable = new BitSet(newLen);
    newTable.or(cachedMaskTable);
    for(int i: ids)
      newTable.set(i, newVal);
    cachedMaskTable = newTable;
  }

  
    /** resets cached-disable table, throwing out all cached changes.
     *  Call this after a change to underlying logger config.
     */
  public static synchronized void resetCachedDisable() {
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


  protected static LEVS getWarnLevel(int id) {
    LEVS l = warnLevels.get(id);
    /*
   if(l == null)
      return LEVS.UNKNOWN;
    else*/
    return l;
    //    return LEVS.TRACE;
  }

  protected static final int LOG_OUT = 1; //constant indicating message should be printed
  protected static final int RECORD_OUT = 2; //constant indicating caller should format and record message
  
  protected static synchronized int shouldPrint(int id, boolean legacyEnabled) {
    int returnV = 0;
    
    if(!printedOnce.get(id)) {
      printedOnce.set(id);
      if(ALWAYS_PRINT_ONCE)
        returnV |= LOG_OUT;
      if(RECORD_ONCE)
        returnV |= RECORD_OUT;
    }
    
    if(userEnabled.get(id))
      return LOG_OUT | returnV;
    
    if(legacyEnabled) {
      //Any user-disable was merged into the cache-disable mask.
      return LOG_OUT | returnV; 
    } else {
        //should disable message, since user didn't specify either way and legacy logger disabled
      changeCacheDisable(id, true);
      return returnV; //decision to print based on whether was first-time or sampling
    }
  }
  
  static String[] tags = new String[200];
  protected static String taggedID(int id) {
    if(id >= tags.length)
      tags = Arrays.copyOf(tags, id * 2);
    if(tags[id] == null) {
      StringBuffer sb = new StringBuffer(40);
      sb.append('(');
      sb.append(id);
      StmtInfo stmtInfo = mapper.getInfo(id);
      synchronized(stmtInfo) {
        for(String tag: stmtInfo.tags)
          if(Character.isUpperCase(tag.charAt(0))) {
            sb.append(' ');
            sb.append(tag);
          }
      }
      
      sb.append(") ");
      tags[id] = sb.toString();
    }
    return tags[id];
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


  public static int getNextCallID() {
    System.out.println("This call should be inlined away");
    return -1;
  }



}
