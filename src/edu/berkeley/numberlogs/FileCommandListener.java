package edu.berkeley.numberlogs;

import java.io.*;

public class FileCommandListener extends CommandInterface {
  
  static final String DEFAULT_FNAME = "commands";

  private static final long POLL_FREQ = 1000;
  
  File cmdFile;
  public FileCommandListener(String file, IDMapper mapping) {
    super(mapping);
    this.cmdFile = new File(file);
    this.setDaemon(true);
  }

  public FileCommandListener(File file, IDMapper mapping) {
    super(mapping);
    this.cmdFile = file;
    this.setDaemon(true);
  }
  
  public FileCommandListener(IDMapper mapping) {
    this(DEFAULT_FNAME, mapping);
  }

  public void run() {
    
    try { 
     
      long lastModTime = 0;
      long lastLength = 0;
      while(true) {
        long new_lastModTime = cmdFile.lastModified();
        long new_lastLength = cmdFile.length();
        if(new_lastLength != lastLength || new_lastModTime != lastModTime) {
          lastLength = new_lastLength;
          lastModTime = new_lastModTime;
          if(cmdFile.exists())
            readFile(cmdFile);
        }
        Thread.sleep(POLL_FREQ);
      }
    
    } catch(InterruptedException e) {}
  }

  private void readFile(File cmdFile) {
    try {
      FileInputStream fis = new FileInputStream(cmdFile);
      BufferedReader br = new BufferedReader(new InputStreamReader(fis));
      String s; 
      while( (s = br.readLine()) != null ) {
        super.doCommand(s);
      }
      br.close();
    } catch(IOException ex) {
      ex.printStackTrace();
    }
  }
  
}
