package edu.berkeley.numberlogs.test;

import java.util.Random;
import org.apache.log4j.Logger;

public class NondeterministicLoad {
  static Logger LOG = Logger.getLogger(NondeterministicLoad.class);


  static class A implements Runnable{
    static {
      LOG.info("loading class A");

    }
    public void run() {
      LOG.info("I am class A");
    }
  }

  static class B implements Runnable{
    static {
      LOG.info("loading class B");

    }
    public void run() {
      LOG.info("I am class B");
    }
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    
    boolean runA = false;
    if(args.length > 0)
        runA = args[0].toLowerCase().equals("a");
    else {
      Random r = new Random();
      runA = r.nextBoolean();
    }
    Runnable aOrB;
    if(runA) {
      aOrB = new A();
    } else
      aOrB = new B();
    aOrB.run();
  }

}
