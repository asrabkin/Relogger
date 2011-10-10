package edu.berkeley.numberlogs.targ;

import edu.berkeley.numberlogs.NumberedLogging;
import edu.berkeley.numberlogs.RecordStatements;
import java.util.logging.*;

public class JavaLog extends NumberedLogging {
  
  public static void logmsg(int id, String original_methname, Logger log, Object msg, Throwable ex) {
    if(cachedMaskTable.get(id))
      return;
    else
      longer_logmsg(id, original_methname, log, msg, ex);
  }
  
  public static void longer_logmsg(int id, String original_methname, Logger log, Object msg, Throwable ex) {
    Level lev = Level.parse(original_methname);
    boolean legacyEnabled =  log.isLoggable(lev);
    int printResult = shouldPrint(id, legacyEnabled);
    if( (printResult & LOG_OUT)  != 0) {
      if(ex == null)
        log.log(lev, "("+id+") "+ msg);
      else
        log.log(lev, "("+id+") "+ msg, ex);
    }
    if( (printResult & RECORD_OUT) != 0)
      RecordStatements.record(id, original_methname, msg, ex);  
  }
}
