package edu.berkeley.numberlogs;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;
import java.io.*;

/**
 * A map between global and local IDs
 * @author asrabkin
 *
 */
public class IDMapper {
  
  int nextID = 1;
  public static final File DEFAULT_MAPPING = new File("relogger/mapping.out");


  Map<String,Integer> mapping;
  
  protected IDMapper() {
    this.mapping = new HashMap<String,Integer>();
  }

  
  /**
   * Construct an IDMapper around an existing Map instance
   * @param mapping
   */
  protected IDMapper(Map<String,Integer> mapping, int maxID) {
    this.mapping = mapping;
    nextID = maxID + 1;
  }
  
  synchronized int localToGlobal(String classHash, int posInClass) {
    String key = classHash + "_" + posInClass; //clunky hack
    Integer v = mapping.get(key);
    if (v == null)  {
      v = nextID++;
      mapping.put(key, v);
      
    }
    return v;
  }
  
  
  public static IDMapper readMap(InputStream in) throws IOException {
    Map<String,Integer> mapping = new HashMap<String,Integer>();
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    
    String ln;
    int maxID = 0;
    while( (ln = br.readLine()) != null) {
      String[] p = ln.split(" ");
      int globalID = Integer.parseInt(p[1]);
      mapping.put(p[0], globalID);
      if( globalID > maxID)
        maxID = globalID;
    }
    
    br.close();
    return new IDMapper(mapping, maxID);
  }
  
  public synchronized void writeMap(OutputStream rawOut) {
    PrintStream ps = new PrintStream(rawOut);
    for( Map.Entry<String, Integer> e: mapping.entrySet()) {
      ps.print(e.getKey());
      ps.print(' ');
      ps.print(e.getValue());
      ps.println();
    }
  }

  static class WriterThread extends Thread{
    IDMapper toWrite;
    File f;
    public WriterThread(IDMapper toWrite, File f) {
      this.toWrite = toWrite;
      this.f = f;
    }
    
    public void run() {
      try { 
        OutputStream out = new FileOutputStream(f);
        toWrite.writeMap(out);
        out.close();
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
    
  }

}
