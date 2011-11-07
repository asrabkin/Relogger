package edu.berkeley.numberlogs.test;

import java.io.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import edu.berkeley.numberlogs.ASMNumberer;

public class ASMAnalysisTest {
  
  public static void main(String[] args) throws IOException {
    
    String classname = args[0];
    String resource = classname.replace('.', '/') + ".class";
    InputStream is = ClassLoader.getSystemResourceAsStream(resource);

    
    ClassReader cr = new ClassReader(is);
    ClassNode classAsTree = new ClassNode();
    cr.accept(classAsTree, 0);
    ASMNumberer numbering = new ASMNumberer("");
    numbering.transformClass(classAsTree, "xyzhash", classAsTree.name);
    
  }

}
