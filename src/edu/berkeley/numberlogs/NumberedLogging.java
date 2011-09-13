package edu.berkeley.numberlogs;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.*;

public class NumberedLogging {
  
  static volatile BitSet globalMaskTable = new BitSet();
  static ConcurrentHashMap<Integer, String> warnLevels = new ConcurrentHashMap<Integer, String>();
  
  static boolean isDisabled(int globalID) {
    return globalMaskTable.get(globalID);
  }
  
  static synchronized void update(int i, boolean isDisabled) {
    int newLen = globalMaskTable.length();
    if (i > newLen)
        newLen = i;
    BitSet newTable = new BitSet(newLen);
    newTable.set(i, isDisabled);
    globalMaskTable = newTable;
  }
  

  public static void setMeth(int line, String methname) {
    warnLevels.put(line, methname);
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
  
  public static void logmsg(int id, String original_methname, Logger log, Object[] args) {
    
    if(isDisabled(id))
      return;
    
    String methname = warnLevels.get(id);
    if(methname == null)
      methname = original_methname;
    
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
   } else {
     log.info(methname + "(" + id  + ") " +args[0]);
   }
    
//    System.out.println(reformatArray(args));
  }

  public static void logmsg(int id, String original_methname, org.apache.commons.logging.Log log, Object[] args) {
    
    if(isDisabled(id))
      return;
    
    String methname = warnLevels.get(id);
    if(methname == null)
      methname = original_methname;
    
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
   } else {
     log.info(methname + "(" + id  + ") " +args[0]);
   }
    
//    System.out.println(reformatArray(args));
  }
  
}
