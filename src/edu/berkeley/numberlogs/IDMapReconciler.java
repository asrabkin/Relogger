package edu.berkeley.numberlogs;

import java.io.*;
import java.nio.channels.FileLock;
import java.util.Random;;


/**
 * A thread that periodically reads the mapping and reconciles against it
 * @author asrabkin
 *
 */
public class IDMapReconciler extends Thread {
  
  public static int POLL_INTERVAL_MS = 5000;
  public volatile boolean running = true;
  final File canonicalMappingFile;
  final IDMapper mapping;
  public IDMapReconciler(File mapFile, IDMapper m) {
    mapping = m;
    canonicalMappingFile = mapFile;
    this.setDaemon(true);
  }
  
  public void run() {
    
    Random rand = new Random();
    long prevModified = canonicalMappingFile.lastModified();
    long prevLen = canonicalMappingFile.length();
    long prevMapSize = mapping.size(); 
    
    try {
      while(running) {
          //pick a sleep time in [0.5 * POLL_INTERVAL_MS, 1.5 * POLL_INTERVAL_MS)
        int rand_adjust = rand.nextInt(POLL_INTERVAL_MS);
        Thread.sleep(POLL_INTERVAL_MS / 2 + rand_adjust);
        
        long latestMod = canonicalMappingFile.lastModified();
        long latestLen = canonicalMappingFile.length();
        long latestMapSize = mapping.size();
        if(latestMod > prevModified || latestLen != prevLen || prevMapSize != latestMapSize) { 
             //we or somebody else did a write
//          System.out.println("Doing a read; now " + latestMapSize + " mappings");
          RandomAccessFile mapF = new RandomAccessFile(canonicalMappingFile, "rw");
          FileLock lock = mapF.getChannel().lock();

          FileInputStream f_in = new FileInputStream(mapF.getFD());
          boolean addedAMapping = mapping.readAndUpdate(f_in);
          if(addedAMapping|| latestMapSize > prevMapSize) { //we added mappings
            mapF.seek(0);
            FileOutputStream out = new FileOutputStream(mapF.getFD());
            latestMapSize = mapping.writeMap(out);
//            System.out.println("just wrote map. File length is " + mapF.length());
//          mapF.setLength(mapF.getFilePointer());
          }

          prevModified = canonicalMappingFile.lastModified();
          prevLen = canonicalMappingFile.length();
          prevMapSize = latestMapSize;

          lock.release();
          mapF.close();
        } 
//        else
//          System.out.println("No change; still " + latestMapSize + " mappings");
      }
    } catch(InterruptedException e) {
        //do nothing, just unwind and exit
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

}
