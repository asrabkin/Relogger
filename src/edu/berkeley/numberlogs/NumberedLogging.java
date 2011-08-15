package edu.berkeley.numberlogs;

import java.util.BitSet;
import org.apache.log4j.*;

public class NumberedLogging {
  
  static volatile BitSet globalMaskTable = new BitSet();
  
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
  
  public static void logmsg(int id, String methname, Logger log, Object[] args) {
    
    if(isDisabled(id))
      return;
    
    if(args.length == 1) 
      log.info(methname +"(" + id + " " +args[0]+ ")");
    else
      log.info(methname +"(" + id + " " +args[0]+ ")", (Throwable) args[1]);
    System.out.println(reformatArray(args));
  }
  
}
