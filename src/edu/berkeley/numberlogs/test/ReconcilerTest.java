package edu.berkeley.numberlogs.test;

import java.io.File;
import edu.berkeley.numberlogs.*;

public class ReconcilerTest {

  /**
   * @param args
   */
  public static void main(String[] args) throws InterruptedException {
    
    if(args.length < 1) {
      System.out.println("usage: ReconcilerTest [id]");
      System.exit(0);
    }
    String myID = args[0];
    
    IDMapper mapper = new IDMapper();
    IDMapReconciler imr = new IDMapReconciler(new File("reconcilertest.txt"), mapper);
    IDMapReconciler.POLL_INTERVAL_MS = 100;
    imr.start();
    String myHashStr = "hashme";
    for( int i =0 ; i < 200; ++i) {
      int nextid = (i + myID.hashCode()) % 200;
      mapper.localToGlobal(myHashStr, nextid, "classname", nextid, "meth");
      Thread.sleep(75);
      if(i % 10 ==0)
        System.out.println(myID + " is alive; " + mapper.size() + " mappings");
    }
    System.out.println(myID + " is exiting!!");
  }

}
