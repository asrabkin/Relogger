package edu.berkeley.numberlogs.targ;

import org.apache.commons.logging.Log;
import edu.berkeley.numberlogs.NumberedLogging;
import edu.berkeley.numberlogs.RecordStatements;

public class CommonsLog extends NumberedLogging {

  public static void logmsg(org.apache.commons.logging.Log log, Object msg, Throwable ex, int id, String original_methname) {
    if(cachedMask(id))
      return;
    else longer_logmsg(log, msg, ex, id, original_methname);
  }

  public static void longer_logmsg(org.apache.commons.logging.Log log, Object msg, Throwable ex, int id, String original_methname) {

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
      log.fatal(taggedID(id) +msg);
    else
      log.fatal(taggedID(id)+msg, ex);
  }
  public static void commonsLog_error(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.error(taggedID(id)+msg);
    else
      log.error(taggedID(id)+msg, ex);
  }
  public static void commonsLog_warn(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.warn(taggedID(id)+msg);
    else
      log.warn(taggedID(id)+msg, ex);
  }
  public static void commonsLog_info(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.info(taggedID(id)+msg);
    else
      log.info(taggedID(id)+msg, ex);
  }
  public static void commonsLog_debug(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.debug(taggedID(id)+msg);
    else
      log.debug(taggedID(id)+msg, ex);
  }
  public static void commonsLog_trace(Log log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.trace(taggedID(id)+msg);
    else
      log.trace(taggedID(id)+msg, ex);
  }

  
}
