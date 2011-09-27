package edu.berkeley.numberlogs.targ;

import org.apache.commons.logging.Log;
import edu.berkeley.numberlogs.NumberedLogging;
import edu.berkeley.numberlogs.RecordStatements;

public class CommonsLog extends NumberedLogging {

  public static void logmsg(int id, String original_methname, org.apache.commons.logging.Log log, Object msg, Throwable ex) {
    if(cachedMaskTable.get(id))
      return;
    else longer_logmsg(id, original_methname, log, msg, ex);
  }


  public static void longer_logmsg(int id, String original_methname, org.apache.commons.logging.Log log, Object msg, Throwable ex) {

    LEVS methname = getWarnLevel(id);
    if(methname == null)
      methname = setMeth(id, original_methname);

    int shouldPrint = 0;
    switch(methname) {
    case FATAL:
     shouldPrint = shouldPrint(id, log.isFatalEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_fatal(log, id, msg, ex);
     break;
    case ERROR:
     shouldPrint = shouldPrint(id, log.isErrorEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_error(log, id, msg, ex);
     break;
    case WARN:
     shouldPrint = shouldPrint(id, log.isWarnEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_warn(log, id, msg, ex);
     break;
    case INFO:
     shouldPrint = shouldPrint(id, log.isInfoEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_info(log, id, msg, ex);
     break;
    case DEBUG:
     shouldPrint = shouldPrint(id, log.isDebugEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_debug(log, id, msg, ex);
     break;
    case TRACE:
     shouldPrint = shouldPrint(id, log.isTraceEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_trace(log, id, msg, ex);
     break;
    }
    if( (shouldPrint & RECORD_OUT) != 0)
      RecordStatements.record(id, original_methname, msg, ex);

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
