package edu.berkeley;

import java.util.BitSet;
import org.apache.commons.logging.Log;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class LogPerfTest {

  public static final long RUNS = 100* 1000*1000;
  static Logger LOG = Logger.getLogger(LogPerfTest.class);
  static org.apache.commons.logging.Log ApacheLOG =
    org.apache.commons.logging.LogFactory.getLog(LogPerfTest.class);
  /**
   * @param args
   */
  
  public static void main(String[] args) {
    LOG.setLevel(Level.INFO);
    
    if(args.length > 0) {
      testBitsetCopy();
    }

    for(int i=0; i < RUNS; ++i) {
      LOG.isTraceEnabled();
      printATrace("I am a warmup log statement");
    }
    
    
    long startT = System.currentTimeMillis();
    for(int i=0; i < RUNS; ++i)
      printATrace("I am a simple log statement");
    long duration = System.currentTimeMillis() - startT;
    double ns_per_log = duration * 1000 * 1000.0 / RUNS;
    System.out.printf("%.2f ns per un-used log stmt", ns_per_log);
    
  }
  private static void printATrace(String s) {
    LOG.trace(s);
  }
  private static void testBitsetCopy() {
    int BITSET_RUNS = 1000000;

    BitSet b = new BitSet(6000);
    for(int i=0; i < BITSET_RUNS; ++i)
      doCopy(b);

    long startT = System.currentTimeMillis();
    for(int i=0; i < BITSET_RUNS; ++i)
      doCopy(b);
    long duration = System.currentTimeMillis() - startT;
    long ns_per_log = duration * 1000 * 1000 / BITSET_RUNS;
    System.out.println(ns_per_log + "ns per bitset copy; bitset size is " + b.size());
  
  }
  private static BitSet doCopy(BitSet b) {
    BitSet newTable;
    synchronized(b) {
      int newLen = b.size();
      newTable = new BitSet(newLen);
      newTable.or(b);
    }
    return newTable;
  }

}
