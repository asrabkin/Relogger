package edu.berkeley.numberlogs;

import java.util.Arrays;
import java.util.HashMap;

public class TagSets {
  
  public static class TagSet {
    int START_SIZE = 4;
    public int[] contents;
    public int entryCount;
    
    public void add(int stmtID) {
      if(entryCount >= contents.length)
        contents = Arrays.copyOf(contents, contents.length * 2);
      contents[entryCount++] = stmtID;
    }
    
    public TagSet() {
      contents = new int[START_SIZE];
      entryCount = 0;
    }
  }
  
  protected HashMap<String, TagSet> tagsByName = new HashMap<String, TagSet>(10);
  
  public synchronized TagSet get(String s) {
    return tagsByName.get(s);
  }
  
  public synchronized void set(String s, TagSet t) {
    tagsByName.put(s, t);
  }
  
  public synchronized void tag(String tagSetName, int ID) {
    TagSet t = tagsByName.get(tagSetName);
    if(t == null) {
      t = new TagSet();
       tagsByName.put(tagSetName, t);
    }
    t.add(ID);
  }
   

}
