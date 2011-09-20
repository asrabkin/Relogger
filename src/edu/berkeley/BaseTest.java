package edu.berkeley;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class BaseTest {

  static Logger LOG = Logger.getLogger(BaseTest.class);
  static Log COMMONS_LOG = LogFactory.getLog(BaseTest.class);

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    Exception e = new IOException("An exception");

    if(args.length > 0 && args[0].equals("commons")) {
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
    } else {
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
    }    
  }

}
