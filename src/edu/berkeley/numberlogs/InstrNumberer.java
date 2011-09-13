/*
 * Copyright (c) 2011 Ariel Rabkin 
 * All rights reserved.
 * 
 * 
 * Portions taken from JChord, by Mayur Naik, which is licensed under the New 
 * BSD License.

 * JChord is Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */

package edu.berkeley.numberlogs;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.util.*;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class InstrNumberer extends ExprEditor implements ClassFileTransformer {


  
  /**
   * This is the entry point invoked by the JVM.
   * Everything else in this class is the tranformer logic
   * @param agentArgs
   * @param instrumentation
   */
  public static void premain(String agentArgs, Instrumentation instrumentation) {
    
    InstrNumberer numberer = new InstrNumberer();
    instrumentation.addTransformer(numberer);
  }
  public final static String PATH_SEPARATOR = File.pathSeparator + "|;";

  static final String[] excludePrefixes = {"java", "sun", "org.apache.log4j", 
      "edu.berkeley.numberlogs", "com"};

  static HashSet<String> LOG_CALLS;
  
  static String[] LOG_CALL_ARR = {"trace", "debug", "info", "warn", "error", "fatal"};
  static {
    LOG_CALLS= new HashSet<String>(LOG_CALL_ARR.length);
    for(String s: LOG_CALL_ARR)
      LOG_CALLS.add(s); 
  }
  
  
  private final ClassPool pool;
  IDMapper IDMap;

  
  public InstrNumberer() {
    System.out.println("Created numberer");
    pool = new ClassPool();
    addClasspathElems(System.getProperty("sun.boot.class.path"));
    addClasspathElems(System.getProperty("java.class.path"));
    File outFile = IDMapper.DEFAULT_MAPPING;
    
    if(outFile.exists()) {
      try { 
        InputStream in = new FileInputStream(outFile);
        IDMap = IDMapper.readMap(in);
        in.close();
      } catch(IOException e) {
        e.printStackTrace();
        IDMap = new IDMapper();
      }
    } else
      IDMap = new IDMapper();
    UDPCommandListener ucl = new UDPCommandListener();
    ucl.start();
    Runtime.getRuntime().addShutdownHook(new IDMapper.WriterThread(IDMap, outFile));
  }


  private void addClasspathElems(String classpath) {
    String[] bootClassPathElems = classpath.split(PATH_SEPARATOR);
    for (String pathElem : bootClassPathElems) {
      try {
        pool.appendClassPath((new File(pathElem)).getAbsolutePath());
      } catch (NotFoundException e) {
      }
    }
  }

  
  
  protected CtClass currentClass; //these are only defined during call to transform
  protected CtBehavior currentMethod;
  int posInClass;
  String classHash;
  public synchronized byte[] transform(ClassLoader loader, String className,
      Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] classfileBuffer) throws IllegalClassFormatException {
    // Note from javadoc:
    //  If this method determines that no transformations are
    //  needed, it should return null. Otherwise, it should create
    //  a new byte[] array, copy the input classfileBuffer into it,
    //  along with all desired transformations, and return the new
    //  array. The input classfileBuffer must not be modified.
    // className is of the form "java/lang/Object"
    String cName = className.replace('/', '.');

    try {
      classHash = getHash(classfileBuffer);
      posInClass = 0;
      for(String prefix: excludePrefixes)
          if(cName.startsWith(prefix))
            return null;
//      System.out.println("trying to transform "+cName);
    
      CtClass inputClass = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
      CtClass transformed = edit(inputClass);

      if (transformed != null) {
        return transformed.toBytecode();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (CannotCompileException e) {
      System.out.println("Failure working on " + cName);
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }

  private String getHash(byte[] classfileBuffer) throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    MessageDigest md = MessageDigest.getInstance("MD5");

    md.update(classfileBuffer, 0, classfileBuffer.length);
    byte[] bytes = md.digest();
    for(int i=0; i < bytes.length; ++i) {
      if( (bytes[i] & 0xF0) == 0)
        sb.append('0');
      sb.append( Integer.toHexString(0xFF & bytes[i]) );
    }  
    return sb.toString();
  }

  
  class CtBehaviorComparator implements Comparator<CtBehavior> {

    @Override
    public int compare(CtBehavior o1, CtBehavior o2) {
      return o1.toString().compareTo(o2.toString());
    }
  }

  public CtClass edit(CtClass clazz) throws CannotCompileException {
    currentClass = clazz;
    CtBehavior clinit = clazz.getClassInitializer();
    if (clinit != null)
      edit(clinit);
    CtBehavior[] inits = clazz.getDeclaredConstructors();
    for (CtBehavior m : inits)
      edit(m);
    CtBehavior[] meths = clazz.getDeclaredMethods();

      //sort list. This matches static ordering.
    java.util.Arrays.sort(meths, new CtBehaviorComparator());
    
    for (CtBehavior m : meths) {
      edit(m);
    }
    return clazz;
  }

  public void edit(CtBehavior method) throws CannotCompileException {
    currentMethod = method;
    method.instrument(this);
  }


  public void edit(MethodCall e) throws CannotCompileException { 
    int line = e.getLineNumber();
    String meth =  e.getMethodName();
    String dest = e.getClassName() + " " + meth;
    if( !(dest.startsWith("org.apache.log4j") || dest.startsWith("org.apache.commons.log")))
        return;
    if(LOG_CALLS.contains(meth)) {
//      System.out.println("editing method call on line "+ line + " to " + dest);
      int id = IDMap.localToGlobal(classHash, posInClass++);
      e.replace("edu.berkeley.numberlogs.NumberedLogging.logmsg("+id +",\""+meth +"\",$0,$args);"); 
    }
    
  }

}//end class
