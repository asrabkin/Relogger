package edu.berkeley.numberlogs.test;

import org.apache.log4j.Logger;
import edu.berkeley.numberlogs.NumberedLogging;

public class TestStmtNumbers {

  static int stmtID;
  static Logger LOG = Logger.getLogger(TestStmtNumbers.class);

  public static void main(String[] args) {
    System.out.println("Future statement ID is " + stmtID);
    methodWithStatement();
  }
  
  public static void methodWithStatement() {
    stmtID = NumberedLogging.getNextCallID();
    LOG.info("I am log statement number " + stmtID);
  }

}
