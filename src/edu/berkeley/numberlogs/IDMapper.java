package edu.berkeley.numberlogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.io.*;

/**
 * A map between global and local IDs. This is only used by the instrumentor.
 * It is a singleton per program, since there is only one instrumentor.
 * 
 * Mappings may be added either due to a class-load or via the Reconciler thread.
 * 
 * File format is successive lines each of which matches
 *    <canonicalID> <globalID> <classname>( <tag>)*
 * 
 * @author asrabkin
 *
 */
public class IDMapper {
  
  public static class StmtInfo {
    public String canonicalID;
    public String classname;
    public int lineno;
    List <String> tags;
    
    public StmtInfo(String canonicalID, String classname, int lineno) {
      this.canonicalID = canonicalID;
      this.classname = classname;
      this.lineno = lineno;
      tags = new LinkedList<String>();
    }

    public synchronized void addTag(String t) {
      if(!tags.contains(t))
        tags.add(t);
    }

    public synchronized void removeTag(String tag) {
      tags.remove(tag);
    }
    
  }
  
  int nextID = 1;
  public static final File DEFAULT_MAPPING = new File("relogger/mapping.out");

  Map<String,Integer> mapping; //forward mapping canonical to number
  Map<Integer, StmtInfo> info; //back map: info about each canonical-number
  Map<String,String> relocationTable; //canonical[newer] to canonical [older]
  
  public IDMapper() {
    this.mapping = new HashMap<String,Integer>(100);
    this.relocationTable = new HashMap<String, String>(100);
    this.info = new LinkedHashMap<Integer, StmtInfo>(100);
  }

  
  /**
   * Construct an IDMapper around an existing Map instance
   * @param mapping
   */
  protected IDMapper(Map<String,Integer> mapping, int maxID) {
    this.mapping = mapping;
    nextID = maxID + 1;
  }
  
  /**
   * Called once per program run, at load time.
   * @param classHash
   * @param posInClass   ignored
   * @param classname
   * @param lineno
   * @return
   */
  public synchronized int localToGlobal(String classHash, int posInClass, String classname, int lineno,
      String methname) {
    String canonicalKey = classHash + "_" + lineno; 
    String softKey = classname+":"+lineno;
    Integer v = mapping.get(canonicalKey);

  //step backwards through relocation table until we either find a mapping or run
  //out of relocation table.
    String reloc = canonicalKey;
    while(v == null && reloc != null) {
      reloc = relocationTable.get(reloc);
      v = mapping.get(reloc);
    }
    
    if (v == null)  { //no mapping found. Note that Relocation mappings are NOT this case.
      v = nextID++;
      mapping.put(canonicalKey, v);//Note this is the true canonical ID, not relocated.
    }
    mapping.put(softKey, v);
    
    if(!info.containsKey(v)) {//FIXME: can this ever happen if v was non-null?
      StmtInfo stmt = new StmtInfo(canonicalKey, classname, lineno);
      info.put(v, stmt);
      stmt.addTag(methname);
    }
    return v;
  }

  /**
   * Check whether we have a mapping for the specified canonical or soft ID.
   * @param s
   * @return
   */
  public synchronized Integer lookup(String s) {
    return mapping.get(s);
  }

  public synchronized StmtInfo getInfo(int stmtID) {
    return info.get(stmtID);
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

    String ln = "";
    try {
      while( (ln = br.readLine()) != null) {
        if(ln.length() < 2)
          continue;
        
        String[] p = ln.split(" ");
        int globalID = Integer.parseInt(p[1]);
        StmtInfo stmt;
        if(!mapping.containsKey(p[0])) { //new statement
          mapping.put(p[0], globalID);
          stmt = new StmtInfo(p[0], p[2], globalID);
          info.put(globalID, stmt);
          madeChange = true;
        } else {
          int myGID = mapping.get(p[0]);
          stmt = info.get(myGID);
        }
        if( globalID > nextID)
          nextID = globalID + 1;
        for(int tagno=3; tagno < p.length; ++tagno) {
/*          if(p[tagno].equals("on")) {

          } else if(p[tagno].equals("off")) {
            
          } else {*/
            ts.tag(p[tagno], globalID);
            stmt.addTag(p[tagno]);
//          }
        }
      }
    } catch(Exception e) {
      System.err.println("Err; failure on line '" + ln+"'");
      e.printStackTrace();
      Runtime.getRuntime().halt(1);
    }
    
    return madeChange;
  }
  
  public synchronized int writeMap(OutputStream rawOut) {
    PrintStream ps = new PrintStream(rawOut);
    
    for( Map.Entry<Integer, StmtInfo> e: info.entrySet()) {
      StmtInfo stmt = e.getValue();
//      if(stmt.canonicalID.length() < 2)
      ps.print(stmt.canonicalID);
      ps.print(' ');
      ps.print(e.getKey());
      ps.print(' ');
      ps.print(stmt.classname);
      for(String tag: stmt.tags) {
        ps.print(' ');
        ps.print(tag);
      }
      ps.println();
    }
    ps.flush();
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


  public final TagSets ts = new TagSets();

  public void readRelocationTables(File reloggerDir) throws IOException {
    for(File f: reloggerDir.listFiles()) {
      if(f.getName().startsWith("relocation")) {
        FileInputStream fis = new FileInputStream(f);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        readRelocation(br);
        br.close();
      }
    }
  }


  private void readRelocation(BufferedReader in) throws IOException {
    String s = "";
    while( (s= in.readLine()) != null) {
      String[] parts = s.split(" ");
      relocationTable.put(parts[0], parts[1]);
    }
  }


  public synchronized void addTags(String tag, int[] stmtIDs) {
    for(int id: stmtIDs) {
      StmtInfo st = info.get(id);
      if(st != null)
        st.addTag(tag);
      ts.tag(tag, id);
    }
  }
  

  public synchronized void rmTags(String tag, int[] stmtIDs) {
    for(int id: stmtIDs) {
      StmtInfo st = info.get(id);
      if(st != null)
        st.removeTag(tag);
      ts.untag(tag, id);
    }
  }

}
