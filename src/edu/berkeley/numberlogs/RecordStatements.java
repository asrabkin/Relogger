package edu.berkeley.numberlogs;

import java.io.*;

public class RecordStatements {

  private static IDMapper mapper;
  static PrintStream out;
  
  public static void init(File containingDir, IDMapper mapper_) {
    mapper = mapper_;
    
    int filesuffix = 1;
    File recordFile = new File(containingDir, "statement_map");
    try {
      while(!recordFile.createNewFile()) {
        recordFile = new File(containingDir, "statement_map."+filesuffix);
        filesuffix ++;
      }
      out = new PrintStream(new FileOutputStream(recordFile));
    } catch(IOException e) {
      e.printStackTrace();
      out = System.out;
    }
  }
  
  public static void record(int stmtID, String originalMethname, Object msg,
      Throwable ex) {
    
    IDMapper.StmtInfo info = mapper.getInfo(stmtID);
    
    synchronized(out) {
      out.println(stmtID+ "\t" +info.canonicalID+ "\t"+info.classname+":"+info.lineno+"\t"+originalMethname+
          "\t" + msg);
      if(ex != null)
        out.println(ex);
      out.flush();
    }
    
  }

}
