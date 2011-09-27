package edu.berkeley;

import java.io.ByteArrayOutputStream;
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
  

  public static final long RUNS = 100* 1000*1000;
  static final int PRINTRUNS = 1000 * 1000 * 10;
  static Logger LOG = Logger.getLogger(LogPerfTest.class);
  static org.apache.commons.logging.Log ApacheLOG =
    org.apache.commons.logging.LogFactory.getLog(LogPerfTest.class);
  /**
   * @param args
   */
  
  public static void main(String[] args) {
    LOG.setLevel(Level.INFO);
    
    java.util.logging.Logger JavaLOG = java.util.logging.Logger.getLogger("commons.logger");
    
    if(args.length > 0) {
      testBitsetCopy();
    }

    for(int i=0; i < RUNS; ++i) {
      LOG.isTraceEnabled();
      LOG.trace("I am a warmup log statement");
      ApacheLOG.trace("So am I");
      JavaLOG.fine("me too!");
    }

    testLog4jUnused();
    
    long startT;
    long duration;
    double ns_per_log;    
    
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
    
    startT = System.currentTimeMillis();
    for(int i=0; i < PRINTRUNS; ++i)
      LOG.info("I am a printed log statement");
    duration = System.currentTimeMillis() - startT;
    ns_per_log = duration * 1000 * 1000.0 / PRINTRUNS;
    System.out.printf("%.2f ns per formatted Log4j stmt\n", ns_per_log);
    
    testApacheUnused();
    
  }



  private static void testApacheUnused() {
    long startT;
    long duration;
    double ns_per_log;
    startT = System.currentTimeMillis();
    for(int i=0; i < RUNS; ++i)
      ApacheLOG.trace("I am a simple log statement");
    duration = System.currentTimeMillis() - startT;
    ns_per_log = duration * 1000 * 1000.0 / RUNS;
    System.out.printf("%.2f ns per un-used Commons log stmt\n", ns_per_log);
  }


  private static void testLog4jUnused() {
    long startT = System.currentTimeMillis();
    for(int i=0; i < RUNS; ++i)
      LOG.trace("I am a simple log statement");
    long duration = System.currentTimeMillis() - startT;
    double ns_per_log = duration * 1000 * 1000.0 / RUNS;
    System.out.printf("%.2f ns per un-used Log4j log stmt\n", ns_per_log);
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
