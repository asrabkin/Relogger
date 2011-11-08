package edu.berkeley.numberlogs.test;

import org.apache.log4j.Logger;

public class LoopPrint {
  
  static Logger LOG = Logger.getLogger(LoopPrint.class);

  public static void main(String[] args) {
    
    try {
    for(int i = 0; i < 10; ++i) {
      Thread.sleep(500);
      LOG.debug("I am message one");
      LOG.debug("I am message two");
      
    }
    } catch(InterruptedException e) {}
    
  }

}
