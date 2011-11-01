package edu.berkeley.numberlogs;


import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public abstract class AbstractNumberer implements ClassFileTransformer {

  static final boolean JAVASSIST = false;
  /**
   * This is the entry point invoked by the JVM.
   * Everything else in this class is the transformer logic
   * @param agentArgs
   * @param instrumentation
   */
  public static void premain(String agentArgs, Instrumentation instrumentation) {
    AbstractNumberer numberer;
    if(JAVASSIST)
      numberer = new JavassistNumberer(agentArgs);//this will trigger a whole bunch
        //of initialization and class loads -- before the transformer hooks are in.
    else
       numberer = new ASMNumberer(agentArgs);
    instrumentation.addTransformer(numberer);
  }
  
  protected enum LogCallAction {
      LOG_CALL,
      IGNORE,
      RESET
    }

  protected static HashMap<String, LogCallAction> LOG_CALLS;
  protected static String[] LOG_CALL_ARR = {"trace", "debug", "info", "warn", "error", "fatal", 
      "fine", "finer", "finest"};
  protected static String[] INNOCUOUS_LOGGER_CALL_ARR = {"getLogger", "getLog", "isEnabledFor", 
        "getLevel", "addHandler", "setUseParentHandlers", "isDebugEnabled", "isInfoEnabled", 
        "isTraceEnabled", "close", "shutdown", "flush", "getAllAppenders", "parse",
        "getCurrentLoggers", "append", "removeAllAppenders", "addAppender"};
  protected static String[] RESET_CALLS_ARR = {"setLevel", "setAdditivity", "setPriority"};
  public static final String PATH_SEPARATOR = File.pathSeparator + "|;";
  protected static final String[] excludePrefixes = {"java", "sun", "org.apache.log4j", 
        "edu.berkeley.numberlogs", "com", "javassist", "org.apache.commons.logging"};
  
  static {
    LOG_CALLS= new HashMap<String, LogCallAction>(LOG_CALL_ARR.length);
    for(String s: LOG_CALL_ARR)
      LOG_CALLS.put(s, LogCallAction.LOG_CALL); 
    for(String s: INNOCUOUS_LOGGER_CALL_ARR) 
      LOG_CALLS.put(s, LogCallAction.IGNORE); 
    for(String s: RESET_CALLS_ARR) 
      LOG_CALLS.put(s, LogCallAction.RESET); 
      
  }
  
  protected IDMapper IDMap;
  protected int posInClass;
  protected String classHash;

  public AbstractNumberer() {
    super();
  }
  
  public AbstractNumberer(String agentArgs) {
    
    int portno = UDPCommandListener.DEFAULT_PORT;
    File outFile = IDMapper.DEFAULT_MAPPING;

    if(agentArgs != null && agentArgs.length() > 0) {
      for(String s: agentArgs.split(",")) {
        String[] k_v = s.split("=");
        if(k_v.length != 2) {
          System.err.println("syntax error. Relogger config is a comma-separated series of settings.");
          System.err.println("Options are portno,file,alwaysonce");
        }
        String optKey = k_v[0];
        String optVal = k_v[1];
        if(optKey.equals("port") || optKey.equals("portno"))
          portno = Integer.parseInt(optVal);
        else if(optKey.equals("alwaysonce"))
          NumberedLogging.ALWAYS_PRINT_ONCE = !optVal.equals("false"); //default to true
        else if(optKey.equals("file"))
          outFile = new File(optVal);
        else
          parseOptionKV(optKey,optVal);
      }
    }
    
    IDMapReconciler.doDummyWrite(outFile);

    IDMap = new IDMapper();
    NumberedLogging.mapper = IDMap;
    IDMapReconciler rec = new IDMapReconciler(outFile, IDMap);
    try {
      if(outFile.exists())
        rec.readMap();
    } catch(IOException e) {
      e.printStackTrace();
    }
    RecordStatements.init(outFile.getParentFile(), IDMap);
    rec.start(); //TODO: is there a race condition if the thread hasn't started before stop?
    UDPCommandListener ucl = new UDPCommandListener(portno, IDMap);
    ucl.start();
    Runtime.getRuntime().addShutdownHook(new IDMapReconciler.WriterThread(rec));
    System.out.println("UDP listener alive on port " + ucl.portno);
  }


  protected void parseOptionKV(String optKey, String optVal) {
  }

  protected String getHash(byte[] classfileBuffer) throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    MessageDigest md = MessageDigest.getInstance("MD5");
  
    md.update(classfileBuffer, 0, classfileBuffer.length);
  
    byte[] bytes = md.digest();
    for(int i=0; i < bytes.length; ++i) {
      if( (bytes[i] & 0xF0) == 0)
        sb.append('0');
      sb.append( Integer.toHexString(0xFF & bytes[i]) );
    }  
    return sb.toString();
  }
  
  protected String getRewriteTarget(String originalClass) {
    String targ = null;
    if(originalClass.startsWith("org.apache.log4j"))
      targ =  "edu.berkeley.numberlogs.targ.Log4J";
    else if(originalClass.startsWith("org.apache.commons.log"))
      targ = "edu.berkeley.numberlogs.targ.CommonsLog";
    else if(originalClass.startsWith("java.util.logging"))
      targ = "edu.berkeley.numberlogs.targ.JavaLog";
    return targ;
  }
  
  protected void raiseInternalError(String msg) {
    System.err.println(msg);
  }

  protected void raiseInternalError(String msg, Throwable ex) {
    System.err.println(msg);
    ex.printStackTrace(System.err);
  }


}