package edu.berkeley;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Log4JLogger;

public class BaseTest {

  static Logger LOG = Logger.getLogger(BaseTest.class);
  static Log COMMONS_LOG = new Log4JLogger(LOG);

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    IOException e = new IOException("An exception");

    LOG.fatal("I am fatal");
    LOG.error("I am error");
    LOG.warn("I am warn");
    LOG.info("I am info");
    LOG.debug("I am debug");
    LOG.trace("I am trace");
    
    LOG.fatal("I am fatal", e);
    LOG.error("I am error", e);
    LOG.warn("I am warn", e);
    LOG.info("I am info", e);
    LOG.debug("I am debug", e);
    LOG.trace("I am trace", e);
  
    commonsLogs(e);

  }

  private static void commonsLogs(IOException e) {
    
    
    COMMONS_LOG.fatal("I am fatal");
    COMMONS_LOG.error("I am error");
    COMMONS_LOG.warn("I am warn");
    COMMONS_LOG.info("I am info");
    COMMONS_LOG.debug("I am debug");
    COMMONS_LOG.trace("I am trace");
    
    COMMONS_LOG.fatal("I am fatal", e);
    COMMONS_LOG.error("I am error", e);
    COMMONS_LOG.warn("I am warn", e);
    COMMONS_LOG.info("I am info", e);
    COMMONS_LOG.debug("I am debug", e);
    COMMONS_LOG.trace("I am trace", e);
  }

}
