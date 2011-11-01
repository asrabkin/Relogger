/*
 * Copyright (c) 2011 Ariel Rabkin 
 * All rights reserved.
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
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.util.*;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class JavassistNumberer extends AbstractNumberer {
  JavassistEditor ed = new JavassistEditor();

  public JavassistNumberer(String agentArgs) {
    super(agentArgs);
    System.out.println("Created numberer");
    pool = new ClassPool();
    addClasspathElems(System.getProperty("sun.boot.class.path"));
    addClasspathElems(System.getProperty("java.class.path"));  
  }

  private final ClassPool pool;


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
  protected String currentMethod;
  public byte[] transform(ClassLoader loader, String className,
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
      for(String prefix: excludePrefixes)
          if(cName.startsWith(prefix) && !cName.startsWith("edu.berkeley.numberlogs.test"))
            return null;
      classHash = getHash(classfileBuffer);
//      System.out.println("Working on class " + cName);
      /*
      System.out.println("Class" +cName + " has " + classfileBuffer.length + " bytes");
      System.out.println("First byte is " + classfileBuffer[0]);
      for(int j =classfileBuffer.length - 5; j < classfileBuffer.length; ++j)
        System.out.print(" " + classfileBuffer[j]);      
      System.out.println(); */
      posInClass = 0;

//      System.out.println("trying to transform "+cName);
      synchronized(this) {
        CtClass inputClass = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
        CtClass transformed = ed.edit(inputClass);
        if (transformed != null) {
          return transformed.toBytecode();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (CannotCompileException e) {
      if(currentMethod != null)
        raiseInternalError("Failure working on " + cName + "." + currentMethod, e);
      else
        raiseInternalError("Failure working on " + cName, e);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Alphabetize methods.
   * @author asrabkin
   *
   */
  static class CtBehaviorComparator implements Comparator<CtBehavior> {

    @Override
    public int compare(CtBehavior o1, CtBehavior o2) {
      return -o1.toString().compareTo(o2.toString());
    }
  }

  class JavassistEditor extends ExprEditor  {
    
  public CtClass edit(CtClass clazz) throws CannotCompileException {
    currentClass = clazz;
    CtBehavior clinit = clazz.getClassInitializer();
    if (clinit != null)
      edit(clinit);
    CtBehavior[] inits = clazz.getDeclaredConstructors();
    for (CtBehavior m : inits)
      edit(m);
    CtBehavior[] meths = clazz.getDeclaredMethods();

      //sort list alphabetically. This matches static ordering.
    java.util.Arrays.sort(meths, new CtBehaviorComparator());
    
    for (CtBehavior m : meths) {
      edit(m);
    }
    return clazz;
  }

  public void edit(CtBehavior method) throws CannotCompileException {
    currentMethod = method.getLongName();
    method.instrument(this);
  }
  
  public void edit(MethodCall e) throws CannotCompileException { 
    int line = e.getLineNumber();
    String meth =  e.getMethodName();
    String dest = e.getClassName() + " " + meth;
    
    String targ = getRewriteTarget(dest);
    
    if(targ != null)  {
      LogCallAction action = LOG_CALLS.get(meth);
      if(action == null) {
          System.err.println("NOTE: logger call to " + meth + " from " + currentMethod);
      } else if(action == LogCallAction.LOG_CALL) {
  //      System.out.println("editing method call on line "+ line + " to " + dest);
        int id = IDMap.localToGlobal(classHash, posInClass++, currentClass.getName(), line, meth);
        int nargs = Descriptor.numOfParameters(e.getSignature());
        if(nargs == 1)
          e.replace(targ + ".logmsg($0,$1, null, "+id +",\""+meth+"\");"); 
        else if(nargs == 2)
          e.replace(targ +".logmsg($0,$1,$2,"+id +",\""+meth+"\");"); 
        else
          raiseInternalError("Logger call with 3 or more args in " + currentMethod + ". Panic!");
      } else if(action == LogCallAction.IGNORE){
      } else if(action == LogCallAction.RESET) {
        e.replace("{ $_ = $proceed($$); edu.berkeley.numberlogs.NumberedLogging.resetCachedDisable(); }");
      }
    }
  }
  }
}//end class

