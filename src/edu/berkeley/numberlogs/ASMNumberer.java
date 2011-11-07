package edu.berkeley.numberlogs;

import static org.objectweb.asm.Opcodes.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.Type;
import edu.berkeley.numberlogs.ConstFlowAnalysis.ConstSrc;

public class ASMNumberer extends AbstractNumberer {

  static final boolean TRACE_ON = false;
  static boolean TRY_STRING_FUSE = true;
  static boolean REWRITE_CALLID_ESCAPE = true;
  
  public ASMNumberer(String agentArgs) {
    super(agentArgs);
    System.out.println("Transforming logs with ASM");
  }

  protected void parseOptionKV(String optKey, String optVal) {
    if(optKey.equals("nofuse"))
      TRY_STRING_FUSE = !optVal.toLowerCase().equals("true");
  }


  PrintWriter out;
  
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
      trace("ASM transforming " + cName);
      
            
      ClassReader cr = new ClassReader(classfileBuffer);
      ClassNode classAsTree = new ClassNode();
      cr.accept(classAsTree, 0);
      
      transformClass(classAsTree, classHash, cName);

      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
                                                //Compute frames seems to upset the loader
      
      if(TRACE_ON) {
        out = new PrintWriter(new FileWriter("output-"+cName+".disassembled"));
        classAsTree.accept(new TraceClassVisitor(cw, out));
        out.close();
      } else {
        classAsTree.accept(cw);
      }

      return cw.toByteArray();
      
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  static class MethodNodeComparator implements Comparator<MethodNode> {
    @Override
    public int compare(MethodNode o1, MethodNode o2) {
      return o1.name.compareTo(o2.name);
    }
  }
  
  @SuppressWarnings("unchecked")
  public synchronized void transformClass(ClassNode cn, String classHash, String classNameDotted) { 
    
    List<MethodNode> alphabeticNodes = new ArrayList<MethodNode>(cn.methods.size());
    MethodNode clinit = null;
    for(Object mn_: cn.methods) {
      MethodNode mn = (MethodNode) mn_;
      alphabeticNodes.add(mn);
      if(mn.name.equals("<clinit>"))
        clinit =mn;
    }
    java.util.Collections.sort(alphabeticNodes, new MethodNodeComparator());
        
    for(MethodNode mn: alphabeticNodes) {
      try {

        Map<MethodInsnNode,Integer> logCalls = findLogCalls(mn, classHash, classNameDotted);
        
        Map<AbstractInsnNode, Integer> callID_LDCs = findGetCallIDs(mn, logCalls);
        rewriteLogCalls(mn, logCalls);
        Frame[] frames = runConstFlow(cn, mn); //callIDs are made constant FIRST
        
        if(TRY_STRING_FUSE && !logCalls.isEmpty()) {
            fixStringConsts(frames, mn, logCalls);
        }
        
        if(REWRITE_CALLID_ESCAPE) {
          relocateCallID(frames, mn, clinit, callID_LDCs);
        }
        
      } catch(AnalyzerException e) {
        System.err.println("FAIL. Offending instr was " + e.node.getOpcode());
        e.printStackTrace();
      }

    }
  }

 
  private Object newConst(Integer id, Object cst) {
    return "(" + id + ") " + cst;
  }

  @SuppressWarnings("unchecked")
  private Map<MethodInsnNode,Integer> findLogCalls(MethodNode mn, String classHash, String className) {
    
    trace("ASM transforming " + mn.name);
    mn.maxStack += 10;
    
    Map<MethodInsnNode,Integer> callIDs = new HashMap<MethodInsnNode,Integer>();
    Iterator<AbstractInsnNode> insnIter = mn.instructions.iterator();
    while(insnIter.hasNext()) {
      AbstractInsnNode insn = insnIter.next();
      int opcode = insn.getOpcode();
      if(opcode == INVOKEINTERFACE || opcode == INVOKEVIRTUAL) {
        MethodInsnNode meth = (MethodInsnNode) insn;
        String targ = getRewriteTarget(meth.owner);
        
        if(targ != null) {
          LogCallAction action = LOG_CALLS.get(meth.name);
          if(action == LogCallAction.LOG_CALL) {
            int line = getLineNumber(insn);
            int id = IDMap.localToGlobal(classHash, posInClass++, className, line, meth.name);
            callIDs.put(meth, id);
          } else if(action == LogCallAction.IGNORE) {
          ;
          } else if(action == LogCallAction.RESET) {
  //            mn.instructions.insertBefore(arg0, arg1)
          }
        }
      }
    }
    return callIDs;
  }

  private Map<AbstractInsnNode, Integer> findGetCallIDs(MethodNode mn, Map<MethodInsnNode, Integer> numberedCalls) {
    Map<AbstractInsnNode, Integer> getNextIDs = new HashMap<AbstractInsnNode, Integer>();
    Iterator<AbstractInsnNode> insnIter = mn.instructions.iterator();
    while(insnIter.hasNext()) {
      AbstractInsnNode insn = insnIter.next();
      int opcode = insn.getOpcode();
      if(opcode == INVOKESTATIC) {
        MethodInsnNode meth = (MethodInsnNode) insn;
        if(meth.owner.equals("edu/berkeley/numberlogs/NumberedLogging") && 
            meth.name.equals("getNextCallID")) {
          int callNumber = getNextCallNumber(meth, mn.instructions, numberedCalls);
          AbstractInsnNode callID = getIConstInstr(callNumber);
          getNextIDs.put(callID, callNumber);
          mn.instructions.insertBefore(meth, callID);
          mn.instructions.remove(meth);
        }
      }
    }
    return getNextIDs;
  }
  
  
  private int getNextCallNumber(AbstractInsnNode call, InsnList instructions, 
         Map<MethodInsnNode, Integer> numberedCalls) {
    
    do {
      Integer id = numberedCalls.get(call);
      if(id != null)
        return id;
      call = call.getNext();
    } while(call != null);
    return -1;
  }

  private void rewriteLogCalls(MethodNode mn,
      Map<MethodInsnNode, Integer> numberedCalls) {
    for(Map.Entry<MethodInsnNode,Integer> kv: numberedCalls.entrySet()) {
      MethodInsnNode meth = kv.getKey();
      int id = kv.getValue();
      int nargs = Type.getArgumentTypes(meth.desc).length;
      //Type retType = Type.getReturnType(meth.desc);
      //System.out.println("Call should return " + retType);
      
      //System.out.println("Rewriting log call on line " + line + " to " + meth.name);
      if(nargs > 2)
        raiseInternalError("Logger call with 3 or more args in " + meth.name + ". Panic!");
      else {
        if(nargs == 1)
          mn.instructions.insertBefore(meth, new InsnNode(ACONST_NULL));
      
        mn.instructions.insertBefore(meth, getIConstInstr(id));
        
        mn.instructions.insertBefore(meth, new LdcInsnNode(meth.name));
        
        meth.desc = newDescFromLogger(meth.owner);
        meth.owner = getRewriteTarget(meth.owner);
        meth.name = "logmsg";
        meth.setOpcode(INVOKESTATIC);
      }
    }
  } 
  

  private Frame[] runConstFlow(ClassNode cn, MethodNode mn)
      throws AnalyzerException {
    ConstFlowAnalysis cflow = new ConstFlowAnalysis();
    Analyzer analyzer = new Analyzer(cflow);
    analyzer.analyze(cn.name, mn);
    Frame[] frames = analyzer.getFrames();
    return frames;
  }
  
  private void fixStringConsts(Frame[] frames, MethodNode mn, Map<MethodInsnNode, Integer> logCalls) throws AnalyzerException {
    HashMap<LdcInsnNode, AbstractInsnNode> toInsert = new HashMap<LdcInsnNode, AbstractInsnNode>();
    
    for(MethodInsnNode logCall: logCalls.keySet()) {
      int callPt = mn.instructions.indexOf(logCall);
      Frame f = frames[callPt];
    //stack will have const, ex, id, desc
      ConstSrc strSrc = (ConstSrc) f.getStack(f.getStackSize() - 4); 
//      System.out.println("logging str const in msg " + logCalls.get(logCall) + " from " + strSrc.toString(mn.instructions));
      if(strSrc.sourceCount() == 1) {
        AbstractInsnNode src = strSrc.getOne();
        int opcode = src.getOpcode();
        /*        int pos = mn.instructions.indexOf(src);
        for(int pos = callPt; pos > 0; pos --) {
          f = frames[pos];
          if(  f.getStack(f.getStackSize() -1).equals(strSrc)) {
            int opcode = mn.instructions.get(pos-1).getOpcode();*/
        if(opcode == LDC) { //SUCCESS -- we can renumber
          LdcInsnNode prevPush = (LdcInsnNode) src;
          
          LdcInsnNode newPush = new LdcInsnNode(newConst(logCalls.get(logCall), prevPush.cst));
          toInsert.put(newPush, logCall);
          logCall.desc = logCall.desc.substring(0, logCall.desc.length() -2) + 
             "Ljava/lang/Object;"+ ")V";
          logCall.name = logCall.name + "_noid";
        } else 
          System.out.println("const introduced via opcode " + opcode);
//            break;
//          }
//        }
      }
    }
    for(Map.Entry<LdcInsnNode, AbstractInsnNode> e:  toInsert.entrySet()) {
      mn.instructions.insertBefore(e.getValue(), e.getKey());
    }
  }
  

  private void relocateCallID(Frame[] frames, MethodNode mn, MethodNode clinit,
      Map<AbstractInsnNode, Integer> callIDLDCs) {
    Iterator<AbstractInsnNode> insnIter = mn.instructions.iterator();
    while(insnIter.hasNext()) {
      AbstractInsnNode insn = insnIter.next();
      int opcode = insn.getOpcode();
      if(opcode == PUTSTATIC) {
        FieldInsnNode putInsn = (FieldInsnNode) insn;
        int callPt = mn.instructions.indexOf(putInsn);
        Frame f = frames[callPt];
        ConstSrc sources = (ConstSrc) f.getStack(f.getStackSize() - 1); 
        if(sources.sourceCount() == 1) {
          AbstractInsnNode src = sources.getOne();
          Integer callID = callIDLDCs.get(src);
          if(callID != null) {
            trace("moving put "+ callID + " to " + putInsn.desc + " to clinit");
            
            mn.instructions.insert(putInsn, new InsnNode(POP));
            mn.instructions.remove(putInsn);
            
              //order is reversed, because we are prepending
            clinit.instructions.insert(putInsn);
            clinit.instructions.insert(getIConstInstr(callID));
          }
        }
      }
    }
  }

  
  private String newDescFromLogger(String logger) {
    String targType = logger.replace(".", "/");
    return "(L"+targType+";Ljava/lang/Object;Ljava/lang/Throwable;ILjava/lang/String;)V";
  }

  private AbstractInsnNode getIConstInstr(int id) {
    if(id < Short.MAX_VALUE)
      return new IntInsnNode(SIPUSH, id);
    else
      return new LdcInsnNode(id);
  }

  private int getLineNumber(AbstractInsnNode insn) {
    while(insn != null) {
      if(insn instanceof LineNumberNode) {
        return ((LineNumberNode) insn).line;
      }
      insn = insn.getPrevious();
    }
    return -1;
  }
  
  //ASM exposes the internal slashed class names for meth call targets
  protected String getRewriteTarget(String originalClass) {
    String targ = null;
    if(originalClass.startsWith("org/apache/log4j"))
      targ =  "edu/berkeley/numberlogs/targ/Log4J";
    else if(originalClass.startsWith("org/apache/commons/log"))
      targ = "edu/berkeley/numberlogs/targ/CommonsLog";
    else if(originalClass.startsWith("java/util/logging"))
      targ = "edu/berkeley/numberlogs/targ/JavaLog";
    return targ;
  }
  
  static final void trace(String s) {
//    System.out.println(s);
  }
  
}
