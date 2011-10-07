package edu.berkeley.numberlogs;

import java.util.Arrays;
import edu.berkeley.numberlogs.TagSets.TagSet;


public class CommandInterface extends Thread {

  final IDMapper mapping;

  public CommandInterface(IDMapper mapping) {
    this.mapping = mapping;
  }

  protected void doCommand(String contents) {
    try {
      String[] words = contents.split("\\s+");
      String cmd = words[0].toLowerCase();
      
      if(words.length > 1){ //per statement command
        int[] stmtIDs = designatorToIDs(words[1]);
        
        if(cmd.equals("up") || cmd.equals("on")) {
          for(int stmtID : stmtIDs)
            NumberedLogging.updateUser(stmtID, false);
        } else if(cmd.equals("down") || cmd.equals("off")) {
          for(int stmtID : stmtIDs)
            NumberedLogging.updateUser(stmtID, true);
        } else if (cmd.equals("setmeth") && words.length > 2) {
          for(int stmtID : stmtIDs)
            NumberedLogging.setMeth(stmtID, words[2]);
        } else if(cmd.equals("once")) {
          for(int stmtID : stmtIDs)
            NumberedLogging.clearPrintedOnce(stmtID);
        } else {
          System.err.println("Unknown log-rewrite command " + cmd);
        }
      }  //commands that are not per-statement
      if(cmd.equals("resetonce")) {
        NumberedLogging.clearAllPrintedOnce();
      } 
      
    } catch(Exception e) {
      System.out.println("got " + contents);
      e.printStackTrace();
    }
    
  }

  static final int[] EMPTY = new int[0];
  private int[] designatorToIDs(String label) {
    int stmtID = 0;
    try {
      stmtID = Integer.parseInt(label);
      return new int[] {stmtID};
    } catch(NumberFormatException e) {
      
      Integer id = mapping.lookup(label);
      if(id != null)
        return new int[] {id};
      else {
        TagSet ts = mapping.ts.get(label);
        if(ts.entryCount > 0)
          return Arrays.copyOf(ts.contents, ts.entryCount);
        else
          return EMPTY;
      }
    }
  }

}