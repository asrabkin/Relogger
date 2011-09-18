package edu.berkeley;

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
  }

}
