package edu.berkeley.numberlogs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Value;
import static org.objectweb.asm.Opcodes.*;


/**
 * An interpreter to find where constants go.
 * A constant is a source instruction and a value.
 *
 */
public class ConstFlowAnalysis implements org.objectweb.asm.tree.analysis.Interpreter {
  
  static class ConstSrc implements Value {
    Set<AbstractInsnNode> srcSet;
    
    Type type;

    public ConstSrc(AbstractInsnNode src, Type type) {
      if(src != null) {
        this.srcSet = new HashSet<AbstractInsnNode>();
        srcSet.add(src);
      }
      this.type = type;
    }

    private ConstSrc(Type type) {
      this.srcSet = null;
      this.type = type;
    }
    
    public String toString() {
      return type.toString();
    }
    
    public String toString(InsnList il) 
    {
      if(srcSet == null)
        return type.toString();
      else {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        for(AbstractInsnNode src: srcSet) {
          sb.append("#");
          sb.append(il.indexOf(src));
          sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("} ");
        sb.append(type.toString());
        return sb.toString();
      }
    }
    
    @Override
    public int getSize() {
      return (type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE) ? 2 : 1;
    }
    
    public boolean equals(Object t) {
      if(this == t)
        return true;
      else if(t==null || !(t instanceof ConstSrc))
        return false;
      else {
        ConstSrc d = (ConstSrc) t;
        if(type == null)
          return d.type == null;
        else if(srcSet == null)
            return d.srcSet == null;
        else
          return type.equals(d.type) && srcSet.equals(d.srcSet);
      }
    }

    public static ConstSrc merge(ConstSrc a, ConstSrc b) {
      
      if(a.type == null || b.type == null)
        return UNINITIALIZED;
      assert a != null: "trying to merge null with " + b;
      assert b != null: "trying to merge " + a + " and null";
      if(a.type.getSort() != b.type.getSort())
        return UNINITIALIZED;
      if(a.equals(b))
        return a;
      else {
        Type supertype;
        if(a.type.getSort() != Type.OBJECT || a.type.equals(b.type))
          supertype = a.type; //no lattice for primitives, so if sorts are equal, types are.
        else
          supertype = Type.getType("Ljava/lang/Object;");
        

        if(a.srcSet == null || b.srcSet == null)
          return new ConstSrc(null, supertype);
        else {
          HashSet<AbstractInsnNode> sources = new HashSet<AbstractInsnNode>();
          sources.addAll(a.srcSet);
          sources.addAll(b.srcSet);
          ConstSrc r = new ConstSrc(supertype);
          r.srcSet = sources;
          return r;
        }
        
      }
    }

    public int sourceCount() {
      if(srcSet == null)
        return 0;
      else
        return srcSet.size();
    }

    public AbstractInsnNode getOne() {
//      if(srcSet == null)
//        return null;
      return srcSet.iterator().next();
    }
  }

  public static final ConstSrc UNINITIALIZED =  new ConstSrc(null, Type.VOID_TYPE);
  
  public static final ConstSrc RETURN =  new ConstSrc(null, null);

  
  @Override
  public Value binaryOperation(AbstractInsnNode insn, Value arg1, Value arg2)
      throws AnalyzerException {
    switch (insn.getOpcode()) {
    case IALOAD:
    case BALOAD:
    case CALOAD:
    case SALOAD:
    case IADD:
    case ISUB:
    case IMUL:
    case IDIV:
    case IREM:
    case ISHL:
    case ISHR:
    case IUSHR:
    case IAND:
    case IOR:
    case IXOR:
        return new ConstSrc(null, Type.INT_TYPE);
    case FALOAD:
    case FADD:
    case FSUB:
    case FMUL:
    case FDIV:
    case FREM:        
      return new ConstSrc(null, Type.FLOAT_TYPE);
    case LALOAD:
    case LADD:
    case LSUB:
    case LMUL:
    case LDIV:
    case LREM:
    case LSHL:
    case LSHR:
    case LUSHR:
    case LAND:
    case LOR:
    case LXOR:
      return new ConstSrc(null, Type.LONG_TYPE);
    case DALOAD:
    case DADD:
    case DSUB:
    case DMUL:
    case DDIV:
    case DREM:
      return new ConstSrc(null, Type.DOUBLE_TYPE);
    case AALOAD: {
      return new ConstSrc(null, Type.getObjectType("java/lang/Object"));
    }
    case LCMP:
    case FCMPL:
    case FCMPG:
    case DCMPL:
    case DCMPG:
      return new ConstSrc(null, Type.INT_TYPE);
    case IF_ICMPEQ:
    case IF_ICMPNE:
    case IF_ICMPLT:
    case IF_ICMPGE:
    case IF_ICMPGT:
    case IF_ICMPLE:
    case IF_ACMPEQ:
    case IF_ACMPNE:
    case PUTFIELD:
      return null;
    default:
      throw new AnalyzerException(insn, "unexpected opcode" + insn.getOpcode());
    }
  }

  @Override
  public Value copyOperation(AbstractInsnNode arg0, Value arg1)
      throws AnalyzerException {
    if(arg1 instanceof ConstSrc) {
      return arg1;
    } else
      throw new AnalyzerException(arg0, "getting an " + arg1.getClass().getCanonicalName() + " surprises me");
  }


  int mergeCount = 0;
  @Override
  public Value merge(Value v, Value w) {
    assert mergeCount++ < 1E7 + 40;
//    if(mergeCount > 1E7)
//      System.out.println("merging " + v + " and " + w);
    if(v == null || w == null)
      return null;
    if(v instanceof ConstSrc && w instanceof ConstSrc) 
      return ConstSrc.merge((ConstSrc)v, (ConstSrc)w);
    else
      return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Value naryOperation(AbstractInsnNode insn, List args)
      throws AnalyzerException {
    if (insn.getOpcode() == MULTIANEWARRAY) {
      return new ConstSrc(null, Type.getType(((MultiANewArrayInsnNode) insn).desc));
    } else { //some kind of method invocation
     Type returnType = Type.getReturnType(((MethodInsnNode) insn).desc);
     if(returnType == Type.VOID_TYPE) 
       return null;            
     return new ConstSrc(null, returnType);
   }
  }

  @Override
  public ConstSrc newOperation(AbstractInsnNode insn) throws AnalyzerException {
      
    switch (insn.getOpcode()) {
      case ACONST_NULL:
          return UNINITIALIZED;
      case ICONST_M1:
      case ICONST_0:
      case ICONST_1:
      case ICONST_2:
      case ICONST_3:
      case ICONST_4:
      case ICONST_5:
      case BIPUSH:
      case SIPUSH:
        return new ConstSrc(insn, Type.INT_TYPE);
      case LCONST_0:
      case LCONST_1:
        return new ConstSrc(insn, Type.LONG_TYPE);
      case FCONST_0:
      case FCONST_1:
      case FCONST_2:
        return new ConstSrc(insn, Type.FLOAT_TYPE);
      case DCONST_0:
      case DCONST_1:
        return new ConstSrc(insn, Type.DOUBLE_TYPE);
      case LDC:
        Object cst = ((LdcInsnNode) insn).cst;
        if (cst instanceof Integer) {
            return new ConstSrc(insn, Type.INT_TYPE);
        } else if (cst instanceof Float) {
          return new ConstSrc(insn,Type.FLOAT_TYPE);
        } else if (cst instanceof Long) {
          return new ConstSrc(insn, Type.LONG_TYPE);
        } else if (cst instanceof Double) {
          return new ConstSrc(insn, Type.DOUBLE_TYPE);
        } else if (cst instanceof Type) {
            return new ConstSrc(insn, Type.getObjectType("java/lang/Class"));
        } else {
            return new ConstSrc(insn, Type.getType(cst.getClass()));
        }
    case JSR:
        return RETURN;
    case GETSTATIC:
        return new ConstSrc(null, Type.getType(((FieldInsnNode) insn).desc));
    case NEW:
        Type t = Type.getObjectType(((TypeInsnNode) insn).desc);
        return new ConstSrc(null, t);
    default:
        throw new AnalyzerException(insn, "Unexpected opcode.");
    }
  }

  @Override
  public Value newValue(Type type) {
    if(type == Type.VOID_TYPE)
      return null;
    else
      return new ConstSrc(null, type);
  }

  //all the ternary operations are array stores -- which don't return values
  public ConstSrc ternaryOperation(
      final AbstractInsnNode insn,
      final Value value1,
      final Value value2,
      final Value value3) throws AnalyzerException {
      return null;
  }
  
  public void returnOperation(
      final AbstractInsnNode insn,
      final Value value,
      final Value expected) throws AnalyzerException  {
//    System.out.println("returning " + value + " expected " + expected);
  }


  @Override
  public Value unaryOperation(AbstractInsnNode insn, Value v)
      throws AnalyzerException {
    switch (insn.getOpcode()) {
    case INEG:
    case IINC:
    case L2I:
    case F2I:
    case D2I:
    case I2B:
    case I2C:
    case I2S:
      return new ConstSrc(null, Type.INT_TYPE);
    case FNEG:
    case I2F:
    case L2F:
    case D2F:
      return new ConstSrc(null, Type.FLOAT_TYPE);
    case LNEG:
    case I2L:
    case F2L:
    case D2L:
      return new ConstSrc(null, Type.LONG_TYPE);
    case DNEG:
    case I2D:
    case L2D:
    case F2D:
      return new ConstSrc(null, Type.DOUBLE_TYPE);
    case IFEQ:
    case IFNE:
    case IFLT:
    case IFGE:
    case IFGT:
    case IFLE:
    case TABLESWITCH:
    case LOOKUPSWITCH:
    case IRETURN:
    case LRETURN:
    case FRETURN:
    case DRETURN:
    case ARETURN:
    case PUTSTATIC:
        return null;
    case GETFIELD:
      FieldInsnNode fin = ((FieldInsnNode) insn);
        return new ConstSrc(null, Type.getType(fin.desc));
    case NEWARRAY:
        switch (((IntInsnNode) insn).operand) {
            case T_BOOLEAN:
                return new ConstSrc(null, Type.getType("[Z"));
            case T_CHAR:
                return new ConstSrc(null, Type.getType("[C"));
            case T_BYTE:
                return new ConstSrc(null, Type.getType("[B"));
            case T_SHORT:
                return new ConstSrc(null, Type.getType("[S"));
            case T_INT:
                return new ConstSrc(null, Type.getType("[I"));
            case T_FLOAT:
                return new ConstSrc(null, Type.getType("[F"));
            case T_DOUBLE:
                return new ConstSrc(null, Type.getType("[D"));
            case T_LONG:
                return new ConstSrc(null, Type.getType("[J"));
            default:
                throw new AnalyzerException(insn, "Invalid array type");
        }
    case ANEWARRAY:
        String desc = ((TypeInsnNode) insn).desc;
        return new ConstSrc(null, Type.getType("[" + Type.getObjectType(desc)));
    case ARRAYLENGTH:
        return new ConstSrc(null, Type.INT_TYPE);
    case ATHROW:
        return null;
    case CHECKCAST:
        desc = ((TypeInsnNode) insn).desc;
        return new ConstSrc(null, Type.getObjectType(desc));
    case INSTANCEOF:
      return new ConstSrc(null, Type.INT_TYPE);
    case MONITORENTER:
    case MONITOREXIT:
    case IFNULL:
    case IFNONNULL:
        return null;
    default:
        throw new AnalyzerException(insn, "unexpected opcode" + insn.getOpcode());
    }
  }
}
