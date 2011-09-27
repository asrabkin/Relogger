/*
 * Copyright (c) 2011 Ariel Rabkin 
 * All rights reserved.
 */
package edu.berkeley.numberlogs;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;

public class NumberedLogging {

    //concurrently accessible.
  protected static volatile BitSet cachedMaskTable = new BitSet();
  static ConcurrentHashMap<Integer, LEVS> warnLevels = new ConcurrentHashMap<Integer, LEVS>();

    //should be protected by a lock on the class.
  static BitSet userDisabled = new BitSet();
  static BitSet userEnabled = new BitSet();
  static BitSet printedOnce = new BitSet(); //this is undefined if user-enabled.

  static boolean ALWAYS_PRINT_ONCE = false;

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


  protected static LEVS getWarnLevel(int id) {
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


  protected static synchronized boolean shouldPrint(int id, boolean legacyEnabled) {
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



}
