package edu.berkeley.numberlogs.targ;

import edu.berkeley.numberlogs.NumberedLogging;
import edu.berkeley.numberlogs.RecordStatements;
import java.util.logging.*;

public class JavaLog extends NumberedLogging {
  
  public static void logmsg(Logger log, Object msg, Throwable ex, int id, String original_methname) {
    if(cachedMask(id))
      return;
    else
      longer_logmsg(log, msg, ex, id, original_methname, null);
  }

  public static void logmsg_noid(Logger log, Object msg, Throwable ex, int id, String original_methname,
      Object newmsg) {
    if(cachedMask(id))
      return;
    else
      longer_logmsg(log, msg, ex, id, original_methname, newmsg);
  }
  
  public static void longer_logmsg(Logger log, Object msg, Throwable ex, int id, 
      String original_methname, Object newmsg) {
    Level lev = Level.parse(original_methname.toUpperCase());
    boolean legacyEnabled =  log.isLoggable(lev);
    int printResult = shouldPrint(id, legacyEnabled);
    if( (printResult & LOG_OUT)  != 0) {
      String msg_str;
      if(newmsg == null)
        msg_str = taggedID(id) + msg;
      else
        msg_str = newmsg.toString();
      
      if(ex == null)
        log.log(lev, msg_str);
      else
        log.log(lev, msg_str, ex);
    }
    if( (printResult & RECORD_OUT) != 0)
      RecordStatements.record(id, original_methname, msg, ex);  
  }
}
