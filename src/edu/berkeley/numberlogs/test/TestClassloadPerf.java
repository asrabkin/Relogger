package edu.berkeley.numberlogs.test;

import java.io.*;
import java.util.ArrayList;

public class TestClassloadPerf {

  /**
   * @param args
   */
  public static void main(String[] args) throws IOException {
    //input is a file with a list of class names
    
    if(args.length < 1) {
      System.err.println("expected filename");
      System.exit(0);
    }
    
    ArrayList<String> classesToLoad = readClassList(args);
    int loadedOK = 0, errs = 0;
    long startT = System.currentTimeMillis();
    for(String s : classesToLoad) {
      try {
        Class.forName(s);
        loadedOK ++;
      } catch (ClassNotFoundException e) {
        if(errs == 0) {
          e.printStackTrace();
        }
        errs ++;
      } catch(NoClassDefFoundError e) {
        if(errs == 0) {
          e.printStackTrace();
        }
        errs ++;
      }
    }
    long duration = System.currentTimeMillis() - startT;
    double avgLoadTime = 1.0 * duration / loadedOK;
    System.out.printf("Loaded %d classes (failed %d) in %d ms. Average of %.3f ms\n", 
          loadedOK, errs, duration, avgLoadTime);
  }

  private static ArrayList<String> readClassList(String[] args)
      throws FileNotFoundException, IOException {
    BufferedReader br = new BufferedReader(new FileReader(args[0]));
    ArrayList<String> classesToLoad = new ArrayList<String>(2000);
    String s = "";
    while( (s = br.readLine()) != null)
      classesToLoad.add(s);
    br.close();
    return classesToLoad;
  }

}
