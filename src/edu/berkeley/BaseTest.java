package edu.berkeley;

import java.io.IOException;
import org.apache.log4j.Logger;

public class BaseTest {

  static Logger LOG = Logger.getLogger(BaseTest.class);

  /**
   * @param args
   */
  public static void main(String[] args) {
    LOG.fatal("I am fatal");
    LOG.error("I am error");
    LOG.warn("I am warn");
    LOG.info("I am info");
    LOG.debug("I am debug");
    LOG.trace("I am trace");
    
    Exception e = new IOException("An exception");
    LOG.fatal("I am fatal", e);
    LOG.error("I am error", e);
    LOG.warn("I am warn", e);
    LOG.info("I am info", e);
    LOG.debug("I am debug", e);
    LOG.trace("I am trace", e);

  }

}
