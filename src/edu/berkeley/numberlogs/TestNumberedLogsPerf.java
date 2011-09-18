package edu.berkeley.numberlogs;

import org.apache.log4j.Logger;

public class TestNumberedLogsPerf {

  /**
   * @param args
   */
  static final int RUNS = 10 * 1000 * 1000;
  public static void main(String[] args) {
    
    Logger LOG = Logger.getLogger(TestNumberedLogsPerf.class);
    
    for(int i=0; i < RUNS; ++i) {
      LOG.isTraceEnabled();
      NumberedLogging.logmsg(0, "trace", LOG, "I am a warmup log statement", null);
    }
    System.out.println("Done warming");
    
    long startT = System.currentTimeMillis();
    for(int i=0; i < RUNS; ++i)
      NumberedLogging.logmsg(0, "trace", LOG, "I am a simple log statement", null);
    long duration = System.currentTimeMillis() - startT;
    long ns_per_log = duration * 1000 * 1000 / RUNS;
    System.out.println(ns_per_log + "ns per un-used log stmt");
  }
}
