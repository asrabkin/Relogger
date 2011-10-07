package edu.berkeley.numberlogs.test;

import java.io.IOException;
import java.util.BitSet;
import org.apache.commons.logging.Log;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.WriterAppender;


public class LogPerfTest {
  
  
  static class DummyOutputStream extends java.io.OutputStream {

    @Override
    public void write(int b) throws IOException {}
    
    @Override
    public void write(byte[] b, int off, int len) {}

    @Override
    public void write(byte[] b) {}
    
  }
  
  interface TestCase {
    double test();
    String name();
    void warmup();
  }
  
  static class Log4JUnprinted implements TestCase {
    
    public void warmup() {}
    
    public double test() {
      long startT = System.currentTimeMillis();
      for(int i=0; i < RUNS; ++i)
        LOG.trace("I am a simple log statement");
      long duration = System.currentTimeMillis() - startT;
      return duration * 1000 * 1000.0 / RUNS;
    }
    
    public String name() {
      return "unused Log4J";
    }    
  }

  static class JavaUnprinted implements TestCase {
    
    public void warmup() {}
    
    public double test() {
      long startT = System.currentTimeMillis();
      for(int i=0; i < RUNS; ++i)
        JavaLOG.finer("I am a simple log statement");
      long duration = System.currentTimeMillis() - startT;
      return duration * 1000 * 1000.0 / RUNS;
    }
    
    public String name() {
      return "unused Java.util.log";
    }    
  }  
  
  static class ApacheUnprinted implements TestCase {
    
    public void warmup() {}
    
    public double test() {
      long startT = System.currentTimeMillis();
      for(int i=0; i < RUNS; ++i)
        ApacheLOG.trace("I am a simple log statement");
      long duration = System.currentTimeMillis() - startT;
      return duration * 1000 * 1000.0 / RUNS;
    }
    
    public String name() {
      return "unused Apache Commons";
    }    
  }
  
 static class Log4JPrinted implements TestCase {
    
    public void warmup() {
      WriterAppender bufferAppender = new WriterAppender(new SimpleLayout(), new DummyOutputStream());
      LOG.removeAllAppenders();
      LOG.setAdditivity(false);
      LOG.addAppender(bufferAppender);

      for(int i=0; i < PRINTRUNS; ++i)
        LOG.info("I am a warmup printing log statement");

      bufferAppender = new WriterAppender(new SimpleLayout(), new DummyOutputStream());
      LOG.removeAllAppenders();
      LOG.setAdditivity(false);
      LOG.addAppender(bufferAppender);
      System.gc();
    }
    
    public double test() {
      long startT;
      long duration;
      
      startT = System.currentTimeMillis();
      for(int i=0; i < PRINTRUNS; ++i)
        LOG.info("I am a printed log statement");
      duration = System.currentTimeMillis() - startT;
      return duration * 1000 * 1000.0 / PRINTRUNS;
    }
    
    public String name() {
      return "formatted Log4J";
    }    
  }
  

  public static final long RUNS = 100* 1000*1000;
  static final int PRINTRUNS = 1000 * 1000 * 10;
  static Logger LOG = Logger.getLogger(LogPerfTest.class);
  static org.apache.commons.logging.Log ApacheLOG =
    org.apache.commons.logging.LogFactory.getLog(LogPerfTest.class);
  static java.util.logging.Logger JavaLOG;
  static int numTests = 1;
  /**
   * @param args
   */
  
  public static void main(String[] args) {
    LOG.setLevel(Level.INFO);
    
    JavaLOG = java.util.logging.Logger.getLogger("commons.logger");
    
    if(args.length > 0)  {
      if(args[0].equals("bitset")){
        testBitsetCopy();
      } else if(args[0].contains("runs=")) {
        numTests = Integer.parseInt(args[0].split("=")[1]);
      }
    }

    for(int i=0; i < RUNS; ++i) {
      LOG.isTraceEnabled();
      LOG.trace("I am a warmup log statement");
      ApacheLOG.trace("So am I");
      JavaLOG.fine("me too!");
    }
    
    TestCase[] tests = new TestCase[] {new Log4JUnprinted(), new ApacheUnprinted(), 
      new JavaUnprinted()};
    for(TestCase t: tests)
      doTest(t);
    
    TestCase t = new Log4JPrinted();
    t.warmup();
    doTest(t);

  }



  private static void doTest(TestCase test) {
    double[] runs = new double[numTests];
    double ns_per_log = 0;
    for(int i=0; i< numTests; ++i) {
      runs[i] = test.test();
      ns_per_log += runs[i] / numTests;
    }
        
    System.out.printf("-- %s avg = %.2f ns\t", test.name(), ns_per_log);
    if(numTests > 1) {
      double stdDev = getStdDev(runs, ns_per_log);
      System.out.printf("stddev = %.2f ns\n", stdDev);
    } else
      System.out.println();
  }



  private static double getStdDev(double[] runs, double mean) {
    double variance = 0;
    for(double d:runs) {
      double diff = d-mean;
      variance += diff * diff / runs.length;
    }
    return Math.sqrt(variance);
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
