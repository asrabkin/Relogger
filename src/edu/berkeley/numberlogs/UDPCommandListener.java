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
      int line = Integer.parseInt(words[1]);
      if(cmd.equals("up")) {
        NumberedLogging.update(line, false);
      } else if(cmd.equals("down")) {
        NumberedLogging.update(line, true);
      } else if (cmd.equals("setmeth") && words.length > 2) {
        NumberedLogging.setMeth(line, words[2]);
      } else {
        System.err.println("Unknown log-rewrite command " + cmd);
      }
    } catch(Exception e) {
      System.out.println("got " + contents);
      e.printStackTrace();
    }
    
  }
  
  

}
