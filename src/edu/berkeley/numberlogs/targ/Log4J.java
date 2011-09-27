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

  
  public static void logmsg(int id, String original_methname, Logger log, Object msg, Throwable ex) {
    if(cachedMaskTable.get(id))
      return;
    else longer_logmsg_Log4j(id, original_methname, log, msg, ex);
  }

  //Split from above because JVMs don't always inline long methods.
  private static void longer_logmsg_Log4j(int id, String original_methname, Logger log, Object msg, Throwable ex) {

    Level level = Level.toLevel(original_methname);
    boolean legacyEnabled = log.isEnabledFor(level);
    int printResult = shouldPrint(id, legacyEnabled);
    if( (printResult & LOG_OUT)  != 0) {
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
           new org.apache.log4j.spi.LoggingEvent(logname, log, level, "("+id+") "+msg, ex));
    }
    if( (printResult & RECORD_OUT) != 0)
      RecordStatements.record(id, original_methname, msg, ex);
  }


}
