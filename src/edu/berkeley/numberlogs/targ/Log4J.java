package edu.berkeley.numberlogs.targ;

import edu.berkeley.numberlogs.NumberedLogging;
import edu.berkeley.numberlogs.RecordStatements;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Log4J extends NumberedLogging {
  
  static class FlexiLevel extends Level {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public FlexiLevel(String levelStr, int syslogEquivalent) {
      super(0, levelStr, syslogEquivalent);
    }
    
  }

  
  public static void logmsg(Logger log, Object msg, Throwable ex, int id, String original_methname) {
    if(cachedMask(id))
      return;
    else longer_logmsg_Log4j(log, msg, ex, id, original_methname, null);
  }

  public static void logmsg_noid(Logger log, Object msg, Throwable ex, int id, String original_methname,
      Object newMsg) {
    if(cachedMask(id))
      return;
    else longer_logmsg_Log4j(log, msg, ex, id, original_methname, newMsg);
  }
  
  //Split from above because JVMs don't always inline long methods.
  private static void longer_logmsg_Log4j(Logger log, Object msg, Throwable ex, int id, 
      String original_methname, Object msg_modified) {

    Level level = Level.toLevel(original_methname);
    boolean legacyEnabled = log.isEnabledFor(level);
    int printResult = shouldPrint(id, legacyEnabled);
    if( (printResult & LOG_OUT)  != 0) {
      /*LEVS methname = getWarnLevel(id);
      if(methname == null)
        methname = setMeth(id, original_methname);
      Level level = Level.toLevel(methname.toString());*/
      if(msg_modified == null)
        msg_modified = taggedID(id) + msg;
        
      String logname = log.getName();

      log.callAppenders(
           new org.apache.log4j.spi.LoggingEvent(logname, log, level, msg_modified, ex));
    }
    if( (printResult & RECORD_OUT) != 0)
      RecordStatements.record(id, original_methname, msg, ex);
  }

}
