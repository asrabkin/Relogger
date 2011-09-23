package edu.berkeley.numberlogs;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;
import java.io.*;

/**
 * A map between global and local IDs. This is only used by the instrumentor.
 * It is a singleton per program, since there is only one instrumentor.
 * 
 * Mappings may be added either due to a class-load or via the Reconciler thread;
 * 
 * @author asrabkin
 *
 */
public class IDMapper {
  
  int nextID = 1;
  public static final File DEFAULT_MAPPING = new File("relogger/mapping.out");


  Map<String,Integer> mapping;
  
  protected IDMapper() {
    this.mapping = new HashMap<String,Integer>(100);
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
    IDMapper m = new IDMapper();
    m.readAndUpdate(in);
    return m;
  }
  
  /**
   * reads mappings from the supplied input stream.
   * disregards mappings for already-mapped log messages.
   * @param in
   * @return true if there were any new mappings
   */
  public synchronized boolean readAndUpdate(InputStream in) throws IOException {
    boolean madeChange = false;
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    
    String ln;
    while( (ln = br.readLine()) != null) {
      String[] p = ln.split(" ");
      int globalID = Integer.parseInt(p[1]);
      if(!mapping.containsKey(p[0])) {
        mapping.put(p[0], globalID);
        madeChange = true;
      }
      if( globalID > nextID)
        nextID = globalID + 1;
    }
    
    br.close(); //FIXME: won't this close the inner?
    return madeChange;
  }
  
  public synchronized int writeMap(OutputStream rawOut) {
    PrintStream ps = new PrintStream(rawOut);
    
    for( Map.Entry<String, Integer> e: mapping.entrySet()) {
      ps.print(e.getKey());
      ps.print(' ');
      ps.print(e.getValue());
      ps.println();
    }
    return mapping.size();
  }
  
  public synchronized int size() {
    return mapping.size();
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
