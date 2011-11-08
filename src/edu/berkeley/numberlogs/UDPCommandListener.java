package edu.berkeley.numberlogs;

import java.io.IOException;
import java.net.*;

public class UDPCommandListener extends CommandInterface {
  
  static final int DEFAULT_PORT = 2345;
  
  int portno;
  public UDPCommandListener(int portno, IDMapper mapping) {
    super(mapping);
    this.portno = portno;
    this.setDaemon(true);
  }
  
  public UDPCommandListener(IDMapper mapping) {
    this(DEFAULT_PORT, mapping);
  }

  public void run() {
    
    try {
      DatagramSocket ds = new DatagramSocket(portno);
      byte[] buf = new byte[1000];
      DatagramPacket p = new DatagramPacket(buf, buf.length);
      
      while(true) {
        ds.receive(p);
        String contents = new String(buf, p.getOffset(), p.getLength());
        for(String s: contents.split("\n"))
          doCommand(s);
      }
    
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  

}
