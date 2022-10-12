package com.nort721.extractor;

import com.nort721.extractor.scanners.FieldsScanner;
import com.nort721.extractor.scanners.JarScanner;
import com.nort721.extractor.scanners.LocalVariableScanner;
import com.nort721.extractor.utils.AsmUtil;
import org.apache.commons.io.FilenameUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Extractor {

    private final static List<JarScanner> scanners = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        String targetPath = args[0];

        System.out.println(targetPath);

        File jarTarget = new File(targetPath);
        JarData jarData = AsmUtil.loadJar(jarTarget);

        scanners.add(new FieldsScanner(jarData));
        scanners.add(new LocalVariableScanner(jarData));

        List<String> totalStrings = new LinkedList<>();
        for (JarScanner jarScanner : scanners)
            totalStrings.addAll(jarScanner.scan());

        StringBuilder msg = new StringBuilder("\nStrings extracted from " + FilenameUtils.getName(targetPath) + ":\n");
        for (String str : totalStrings)
            msg.append(str).append('\n');
        System.out.println(msg);
    }

}
