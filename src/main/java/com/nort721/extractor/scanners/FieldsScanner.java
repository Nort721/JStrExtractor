package com.nort721.extractor.scanners;

import com.nort721.extractor.JarData;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FieldsScanner extends JarScanner {

    public FieldsScanner(JarData jarData) {
        super(jarData);
    }

    @Override
    public List<String> scan() {
        List<String> detectedStrings = new LinkedList<>();

        for (ClassNode classNode : jarData.getClasses()) {
            for (FieldNode fieldNode : classNode.fields) {
                if (fieldNode.desc.contains("Ljava/lang/String;") && fieldNode.value != null)
                    detectedStrings.add("name -> "+fieldNode.name+", value -> " + fieldNode.value);
            }
        }

        return detectedStrings;
    }
}
