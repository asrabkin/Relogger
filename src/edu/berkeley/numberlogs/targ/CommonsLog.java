package edu.berkeley.numberlogs.targ;

import org.apache.commons.logging.Log;
import edu.berkeley.numberlogs.NumberedLogging;
import edu.berkeley.numberlogs.RecordStatements;

public class CommonsLog extends NumberedLogging {

  public static void logmsg(org.apache.commons.logging.Log log, Object msg, Throwable ex, int id, String original_methname) {
    if(cachedMask(id))
      return;
    else longer_logmsg(log, msg, ex, id, original_methname, true);
  }

  public static void logmsg_noid(org.apache.commons.logging.Log log, Object msg, Throwable ex,
      int id, String original_methname) {
    if(cachedMask(id))
      return;
    else longer_logmsg(log, msg, ex, id, original_methname, false);
  }
  
  public static void longer_logmsg(org.apache.commons.logging.Log log, Object msg, 
      Throwable ex, int id, String original_methname, boolean printID) {

    LEVS methname = getWarnLevel(id);
    if(methname == null)
      methname = setMeth(id, original_methname);

    int shouldPrint = 0;
    switch(methname) {
    case FATAL:
     shouldPrint = shouldPrint(id, log.isFatalEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_fatal(log, id, msg, ex, printID);
     break;
    case ERROR:
     shouldPrint = shouldPrint(id, log.isErrorEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_error(log, id, msg, ex, printID);
     break;
    case WARN:
     shouldPrint = shouldPrint(id, log.isWarnEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_warn(log, id, msg, ex, printID);
     break;
    case INFO:
     shouldPrint = shouldPrint(id, log.isInfoEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_info(log, id, msg, ex, printID);
     break;
    case DEBUG:
     shouldPrint = shouldPrint(id, log.isDebugEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_debug(log, id, msg, ex, printID);
     break;
    case TRACE:
     shouldPrint = shouldPrint(id, log.isTraceEnabled());    
     if( (shouldPrint & LOG_OUT) !=0)
         commonsLog_trace(log, id, msg, ex, printID);
     break;
    }
    if( (shouldPrint & RECORD_OUT) != 0)
      RecordStatements.record(id, original_methname, msg, ex);

    //    System.out.println(reformatArray(args));
  }

  public static void commonsLog_fatal(Log log, int id, Object msg, Throwable ex, boolean printID) {
    String msg_str;
    if(printID)
        msg_str = taggedID(id) + msg;
    else
        msg_str = msg.toString();

    if(ex == null) 
      log.fatal(msg_str);
    else
      log.fatal(msg_str, ex);
  }
  public static void commonsLog_error(Log log, int id, Object msg, Throwable ex, boolean printID) {
    String msg_str;
    if(printID)
        msg_str = taggedID(id) + msg;
    else
        msg_str = msg.toString();

    if(ex == null) 
      log.error(msg_str);
    else
      log.error(msg_str, ex);
  }
  public static void commonsLog_warn(Log log, int id, Object msg, Throwable ex, boolean printID) {
    String msg_str;
    if(printID)
        msg_str = taggedID(id) + msg;
    else
        msg_str = msg.toString();

    if(ex == null) 
      log.warn(msg_str);
    else
      log.warn(msg_str, ex);
  }
  public static void commonsLog_info(Log log, int id, Object msg, Throwable ex, boolean printID) {
    String msg_str;
    if(printID)
        msg_str = taggedID(id) + msg;
    else
        msg_str = msg.toString();

    if(ex == null) 
      log.info(msg_str);
    else
      log.info(msg_str, ex);
  }
  public static void commonsLog_debug(Log log, int id, Object msg, Throwable ex, boolean printID) {
    String msg_str;
    if(printID)
        msg_str = taggedID(id) + msg;
    else
        msg_str = msg.toString();

    if(ex == null) 
      log.debug(msg_str);
    else
      log.debug(msg_str, ex);
  }
  public static void commonsLog_trace(Log log, int id, Object msg, Throwable ex, boolean printID) {
    String msg_str;
    if(printID)
        msg_str = taggedID(id) + msg;
    else
        msg_str = msg.toString();

    if(ex == null) 
      log.trace(msg_str);
    else
      log.trace(msg_str, ex);
  }


  
}
