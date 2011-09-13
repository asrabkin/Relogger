package edu.berkeley;

import org.apache.commons.logging.Log;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class LogPerfTest {

  public static final long RUNS = 10* 1000*1000;
  static Logger LOG = Logger.getLogger(LogPerfTest.class);
  static org.apache.commons.logging.Log ApacheLOG =
    org.apache.commons.logging.LogFactory.getLog(LogPerfTest.class);
  /**
   * @param args
   */
  
  public static void main(String[] args) {
    LOG.setLevel(Level.INFO);

    for(int i=0; i < RUNS; ++i)
      LOG.trace("I am a warmup log statement");

    long startT = System.currentTimeMillis();
    for(int i=0; i < RUNS; ++i)
      LOG.trace("I am a simple log statement");
    long duration = System.currentTimeMillis() - startT;
    long ns_per_log = duration * 1000 * 1000 / RUNS;
    System.out.println(ns_per_log + "ns per un-used log stmt");
    
  }

}
