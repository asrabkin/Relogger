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
    this.setName("IDMapReconciler");
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
        
        synchronized(this) {
          wait(POLL_INTERVAL_MS / 2 + rand_adjust);
        }
        long latestMod = canonicalMappingFile.lastModified();
        long latestLen = canonicalMappingFile.length();
        long latestMapSize = mapping.size();
//        System.out.println("considering a read; size = " + latestMapSize);
        if(latestMod > prevModified || latestLen != prevLen || prevMapSize != latestMapSize) { 
             //we or somebody else did a write
//          System.out.println("Doing a read; now " + latestMapSize + " mappings");
          RandomAccessFile mapF = new RandomAccessFile(canonicalMappingFile, "rw");
          FileLock lock = mapF.getChannel().tryLock();
          if(lock == null) {
//            relogger_log("FAILED TO GET LOCK");
            continue;
          }
//          else 
//            relogger_log("GOT LOCK");
          FileInputStream f_in = new FileInputStream(mapF.getFD());
          boolean addedAMapping = mapping.readAndUpdate(f_in);
          if(addedAMapping|| latestMapSize > prevMapSize) { //we added mappings
            mapF.seek(0);
            FileOutputStream out = new FileOutputStream(mapF.getFD());
            latestMapSize = mapping.writeMap(out);
//            relogger_log("Did write");
//          mapF.setLength(mapF.getFilePointer());
          }

          prevModified = canonicalMappingFile.lastModified();
          prevLen = canonicalMappingFile.length();
          prevMapSize = latestMapSize;

          lock.release();
//          relogger_log("RELEASED LOCK");
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

  public void writeAndStop() throws InterruptedException {
    running = false;
    relogger_log("triggering write on exit");
    synchronized(this) {
      this.notify();
    }
//    System.out.println("in writeAndStop, joining...");
    this.join();
  }
  
  public static void relogger_log(String s) {
    System.out.println("RELOGGER " + s);
  }

  
  static class WriterThread extends Thread{
    IDMapReconciler toWrite;
    public WriterThread(IDMapReconciler toWrite) {
      this.toWrite = toWrite;
    }
    
    public void run() {
      try { 
        toWrite.writeAndStop();
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
    
  }


  /**
   * The relogger instrumentation code has to get locks on relogger data structures
   * at class load time.
   * That means that if we ever trigger a class load while holding locks, disaster will strike.
   * Fix is to force class loads ahead of time by calling the function below.
   * @param outFile
   */
  public static void doDummyWrite(File outFile) {
    try {
      RandomAccessFile justForLoading = new RandomAccessFile("/dev/null", "rw");
      justForLoading.close();
      FileOutputStream fos = new FileOutputStream("/dev/null", true);
      fos.getChannel(); //trigger a class load
      fos.getFD(); //trigger a class load
      IDMapper dummyMapper = new IDMapper();
      dummyMapper.localToGlobal(" ", 1);
      dummyMapper.writeMap(fos);
      fos.close();
    } catch (IOException e) {
    }
  }
  
}
