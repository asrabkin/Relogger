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
import java.security.ProtectionDomain;
import java.util.HashSet;
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
  
  
  private final ClassPool pool;
  protected CtClass currentClass;
  protected CtBehavior currentMethod;

  public final static String PATH_SEPARATOR = File.pathSeparator + "|;";

  static HashSet<String> LOG_CALLS;
  
  static String[] LOG_CALL_ARR = {"trace", "debug", "info", "warn", "error", "fatal"};
  static {
    LOG_CALLS= new HashSet<String>(LOG_CALL_ARR.length);
    for(String s: LOG_CALL_ARR)
      LOG_CALLS.add(s); 
  }
  
  
  public InstrNumberer() {
    System.out.println("Created numberer");
    pool = new ClassPool();
    addClasspathElems(System.getProperty("sun.boot.class.path"));
    addClasspathElems(System.getProperty("java.class.path"));
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

  
  
  static final String[] excludePrefixes = {"java", "sun", "org.apache.log4j", 
      "edu.berkeley.numberlogs", "com"};
  
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

    for(String prefix: excludePrefixes)
        if(cName.startsWith(prefix))
          return null;
    System.out.println("trying to transform "+cName);
    
    Exception ex = null;
    try {
      CtClass inputClass = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
      CtClass transformed = edit(inputClass);

      if (transformed != null) {
        return transformed.toBytecode();
      }
    } catch (IOException e) {
      ex = e;
    } catch (CannotCompileException e) {
      ex = e; 
    }

    if (ex != null) {
      ex.printStackTrace();
    }
    return null;
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
    for (CtBehavior m : meths) {
      edit(m);
    }
    return clazz;
  }

  public void edit(CtBehavior method) throws CannotCompileException {
    currentMethod = method;
    method.instrument(this);
  }


  int nextID = 1;
  public void edit(MethodCall e) throws CannotCompileException { 
    int line = e.getLineNumber();
    String meth =  e.getMethodName();
    String dest = e.getClassName() + " " + meth;
    System.out.println("editing method call on line "+ line + " to " + dest);
    //e.getMethodName()
    if(LOG_CALLS.contains(meth)) {
      int id = nextID ++;
      e.replace("edu.berkeley.numberlogs.NumberedLogging.logmsg("+id +",\""+meth +"\",$0,$args);"); 
    }
    
  }

}//end class

