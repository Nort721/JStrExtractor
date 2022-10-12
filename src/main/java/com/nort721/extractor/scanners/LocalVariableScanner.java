package com.nort721.extractor.scanners;

import com.nort721.extractor.JarData;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedList;
import java.util.List;

public class LocalVariableScanner extends JarScanner {

    public LocalVariableScanner(JarData jarData) {
        super(jarData);
    }

    @Override
    public List<String> scan() {
        List<String> detectedStrings = new LinkedList<>();

        for (ClassNode classNode : jarData.getClasses()) {
            for (MethodNode methodNode : classNode.methods) {
                if (methodNode.localVariables != null) {
                    for (LocalVariableNode localVariableNode : methodNode.localVariables) {
                        if (methodNode.desc.contains("Ljava/lang/String;")) {
                            //System.out.println("name: " + localVariableNode.name + " | desc: " + localVariableNode.desc);
                        }
                    }
                }
            }
        }

        return detectedStrings;
    }
}
