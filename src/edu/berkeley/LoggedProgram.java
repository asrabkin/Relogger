package edu.berkeley;

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
    p.dynMeth();
    p.withExcept(null);
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
