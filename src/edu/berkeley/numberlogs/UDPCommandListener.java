package edu.berkeley.numberlogs;

import java.io.IOException;
import java.net.*;

public class UDPCommandListener extends Thread {
  
  static final int DEFAULT_PORT = 2345;
  
  int portno;
  public UDPCommandListener(int portno) {
    this.portno = portno;
    this.setDaemon(true);
  }
  
  public UDPCommandListener() {
    this(DEFAULT_PORT);
  }

  public void run() {
    
    try {
      DatagramSocket ds = new DatagramSocket(portno);
      byte[] buf = new byte[1000];
      DatagramPacket p = new DatagramPacket(buf, buf.length);
      
      while(true) {
        ds.receive(p);
        String contents = new String(buf, p.getOffset(), p.getLength());
        doCommand(contents);
      }
    
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void doCommand(String contents) {
    try {
      String[] words = contents.split("\\s+");
      String cmd = words[0].toLowerCase();
      
      if(cmd.equals("resetonce")) {
        NumberedLogging.clearAllPrintedOnce();
      } else { //per statement command
        int stmtID = Integer.parseInt(words[1]);
        if(cmd.equals("up") || cmd.equals("on")) {
          NumberedLogging.updateUser(stmtID, false);
        } else if(cmd.equals("down") || cmd.equals("off")) {
          NumberedLogging.updateUser(stmtID, true);
        } else if (cmd.equals("setmeth") && words.length > 2) {
          NumberedLogging.setMeth(stmtID, words[2]);
        } else if(cmd.equals("once")) {
          NumberedLogging.clearPrintedOnce(stmtID);
        } else {
          System.err.println("Unknown log-rewrite command " + cmd);
        }
      }
    } catch(Exception e) {
      System.out.println("got " + contents);
      e.printStackTrace();
    }
    
  }
  
  

}
