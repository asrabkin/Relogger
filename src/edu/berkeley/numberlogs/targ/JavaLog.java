package edu.berkeley.numberlogs.targ;

import edu.berkeley.numberlogs.NumberedLogging;
import edu.berkeley.numberlogs.RecordStatements;
import java.util.logging.*;

public class JavaLog extends NumberedLogging {
  
  public static void logmsg(Logger log, Object msg, Throwable ex, int id, String original_methname) {
    if(cachedMask(id))
      return;
    else
      longer_logmsg(log, msg, ex, id, original_methname);
  }
  
  public static void longer_logmsg(Logger log, Object msg, Throwable ex, int id, String original_methname) {
    Level lev = Level.parse(original_methname.toUpperCase());
    boolean legacyEnabled =  log.isLoggable(lev);
    int printResult = shouldPrint(id, legacyEnabled);
    if( (printResult & LOG_OUT)  != 0) {
      if(ex == null)
        log.log(lev, taggedID(id) + msg);
      else
        log.log(lev, taggedID(id) + msg, ex);
    }
    if( (printResult & RECORD_OUT) != 0)
      RecordStatements.record(id, original_methname, msg, ex);  
  }
}
