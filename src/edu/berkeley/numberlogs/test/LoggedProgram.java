package edu.berkeley.numberlogs.test;

import org.apache.log4j.*;

public class LoggedProgram {

  static Logger LOG = Logger.getLogger(LoggedProgram.class);

  public static void staticMeth() {
    LOG.info("log statement in static method");
    
  }
  
  /**
   * @param args
   * 
   * Try with -javaagent:
   */
  public static void main(String[] args) {
    LOG.info("I am a log statement");
    LOG.warn("I am a second one");
    staticMeth();
    LoggedProgram p = new LoggedProgram();
    p.withExcept(null);
    p.dynMeth();

    
    try {
    int loopsToGo = 300;
    while(args.length > 0 && args[0].equals("loop") && loopsToGo-- > 0) {
        LOG.info("I am looping forever");
        Thread.sleep(1000);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    System.out.println("Done");
  }
  
  public void dynMeth() {
    LOG.info("I am in a dynamic method");
  }
  
  
  public void withExcept(String nullable) {
    
    try {
      nullable.length();
    } catch(Exception e) {
      LOG.info("caught exception", e);
    }
  }
}
