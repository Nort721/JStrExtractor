package com.nort721.extractor.utils;

import com.nort721.extractor.Extractor;
import com.nort721.extractor.JarData;
import com.nort721.extractor.exceptions.JStrExtractorException;
import com.nort721.extractor.utils.asm.ByteArrayOutputStream;
import com.nort721.extractor.utils.asm.IOUtils;
import lombok.experimental.UtilityClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

@UtilityClass
public class AsmUtil {

    /**
     * loops through all the entries in a file and returns all the compiled
     * classes in that file
     * @param file The file that we are scanning
     * @return A list of all the classNodes in that file
     */
    public static JarData loadJar(File file) {
        List<ClassNode> classes = new ArrayList<>();
        List<byte[]> sourceBytes = new LinkedList<>();
        Map<String, byte[]> fileMap = new HashMap<>();

        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();

                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                    byte[] bytes = IOUtils.toByteArray(inputStream);

                    if (jarEntry.getName().endsWith(".class")) {
                        sourceBytes.add(bytes);

                        ClassNode classNode = new ClassNode();
                        ClassReader classReader = new ClassReader(bytes);

                        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
                        classes.add(classNode);
                    } else {
                        fileMap.put(jarEntry.getName(), bytes);
                    }

                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new JarData(classes, sourceBytes, fileMap);
    }

    public static void saveJar(JarOutputStream jarOutputStream, JarData jarData) throws IOException {
        for (ClassNode classNode : jarData.getClasses()) {
            JarEntry jarEntry = new JarEntry(classNode.name + ".class");
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            jarOutputStream.putNextEntry(jarEntry);

            classNode.accept(classWriter);
            jarOutputStream.write(classWriter.toByteArray());
            jarOutputStream.closeEntry();
        }

        for (Map.Entry<String, byte[]> entry : jarData.getFileMap().entrySet()) {
            jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
            jarOutputStream.write(entry.getValue());
            jarOutputStream.closeEntry();
        }

        jarOutputStream.close();
    }

    public static String getSelfPath() {
        String path = null;
        try {
            path = Extractor.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static long getJarSourceChecksum() {
        String path = null;
        try {
            path = Extractor.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        List<byte[]> classes = new ArrayList<>();
        try (JarFile jarFile = new JarFile(path)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();

                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                    byte[] bytes = IOUtils.toByteArray(inputStream);

                    if (jarEntry.getName().endsWith(".class")) {
                        classes.add(bytes);
                    }

                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        int count = 0;
        for (byte[] arr : classes)
            count += arr.length;

        byte[] jarArray = new byte[count];

        for (int i = 0; i < classes.size(); i++) {
            for (byte b : classes.get(i))
                jarArray[i] = b;
        }

        Checksum crc32 = new CRC32();
        crc32.update(jarArray, 0, jarArray.length);

        return crc32.getValue();
    }

    public static long getJarSourceChecksum(JarData jarData) {
        List<byte[]> classes = jarData.getSourceBytes();

        int count = 0;
        for (byte[] arr : classes)
            count += arr.length;

        byte[] jarArray = new byte[count];

        for (int i = 0; i < classes.size(); i++) {
            for (byte b : classes.get(i))
                jarArray[i] = b;
        }

        Checksum crc32 = new CRC32();
        crc32.update(jarArray, 0, jarArray.length);

        return crc32.getValue();
    }

    public static List<byte[]> loadClasses(File file) {
        List<byte[]> classes = new ArrayList<>();
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();

                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                    byte[] bytes = IOUtils.toByteArray(inputStream);

                    if (jarEntry.getName().endsWith(".class")) {
                        classes.add(bytes);
                    }

                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return classes;
    }

    public static boolean isInstruction(AbstractInsnNode insn) {
        return !(insn instanceof FrameNode) && !(insn instanceof LineNumberNode) && !(insn instanceof LabelNode);
    }

    public static int getCodeSize(MethodNode methodNode) {
        CodeSizeEvaluator cse = new CodeSizeEvaluator(null);
        methodNode.accept(cse);
        return cse.getMaxSize();
    }

    public static byte[] toByteArray(InputStream in) throws JStrExtractorException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (in.available() > 0) {
                int data = in.read(buffer);
                out.write(buffer, 0, data);
            }

            in.close();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new JStrExtractorException(e.getMessage());
        }
    }

    public static String getMainFromManifest(JarFile jar) {
        Map<Object, Object> map = null;
        try {
            map = jar.getManifest().getMainAttributes();
        } catch (IOException e) {
            System.out.println("can't extract main-class from manifest");
        }

        String mainClass = null;

        for (Object obj : map.keySet()) {
            if (obj.toString().equalsIgnoreCase("main-class")) {
                mainClass = map.get(obj).toString();
                break;
            }
        }

        return mainClass;
    }

    public static File getJarFile() {
        return new File(AsmUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath().replace("file:", ""));
    }

    public static AbstractInsnNode getOptimisedInt(int value) {
        if (value >= -1 && value <= 5)
            return new InsnNode(value + 3);
        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
            return new IntInsnNode(Opcodes.BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
            return new IntInsnNode(Opcodes.SIPUSH, value);

        return new LdcInsnNode(value);
    }

    public static void visitOptimisedInt(MethodVisitor mv, int i) {
        if (i >= -1 && i <= 5)
            mv.visitInsn(i + 3);
        else if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE)
            mv.visitIntInsn(Opcodes.BIPUSH, i);
        else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE)
            mv.visitIntInsn(Opcodes.SIPUSH, i);
        else
            mv.visitLdcInsn(i);
    }

    public static boolean isReturn(int opcode) {
        return (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN);
    }

    public static boolean hasAnnotations(ClassNode classNode) {
        return (classNode.visibleAnnotations != null && !classNode.visibleAnnotations.isEmpty())
                || (classNode.invisibleAnnotations != null && !classNode.invisibleAnnotations.isEmpty());
    }

    public static boolean hasAnnotations(MethodNode methodNode) {
        return (methodNode.visibleAnnotations != null && !methodNode.visibleAnnotations.isEmpty())
                || (methodNode.invisibleAnnotations != null && !methodNode.invisibleAnnotations.isEmpty());
    }

    public static boolean hasAnnotations(FieldNode fieldNode) {
        return (fieldNode.visibleAnnotations != null && !fieldNode.visibleAnnotations.isEmpty())
                || (fieldNode.invisibleAnnotations != null && !fieldNode.invisibleAnnotations.isEmpty());
    }

    public static boolean isIntInsn(AbstractInsnNode insn) {
        if (insn == null) {
            return false;
        }
        int opcode = insn.getOpcode();
        return ((opcode >= org.objectweb.asm.Opcodes.ICONST_M1 && opcode <= org.objectweb.asm.Opcodes.ICONST_5)
                || opcode == org.objectweb.asm.Opcodes.BIPUSH
                || opcode == org.objectweb.asm.Opcodes.SIPUSH
                || (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer));
    }

    public static boolean isLongInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode == org.objectweb.asm.Opcodes.LCONST_0
                || opcode == org.objectweb.asm.Opcodes.LCONST_1
                || (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Long));
    }

    public static AbstractInsnNode getNumberInsn(int number) {
        if (number >= -1 && number <= 5) {
            return new InsnNode(number + 3);
        } else if (number >= -128 && number <= 127) {
            return new IntInsnNode(Opcodes.BIPUSH, number);
        } else if (number >= -32768 && number <= 32767) {
            return new IntInsnNode(Opcodes.SIPUSH, number);
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static AbstractInsnNode getNumberInsn(long number) {
        if (number >= 0 && number <= 1) {
            return new InsnNode((int) (number + 9));
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static AbstractInsnNode getNumberInsn(float number) {
        if (number >= 0 && number <= 2) {
            return new InsnNode((int) (number + 11));
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static AbstractInsnNode getNumberInsn(double number) {
        if (number >= 0 && number <= 1) {
            return new InsnNode((int) (number + 14));
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static int getIntegerFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= org.objectweb.asm.Opcodes.ICONST_M1 && opcode <= org.objectweb.asm.Opcodes.ICONST_5) {
            return opcode - 3;
        } else if (insn instanceof IntInsnNode
                && insn.getOpcode() != org.objectweb.asm.Opcodes.NEWARRAY) {
            return ((IntInsnNode) insn).operand;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer) {
            return (Integer) ((LdcInsnNode) insn).cst;
        }

        throw new IllegalArgumentException("Unexpected instruction");
    }

    public static long getLongFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= org.objectweb.asm.Opcodes.LCONST_0 && opcode <= org.objectweb.asm.Opcodes.LCONST_1) {
            return opcode - 9;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Long) {
            return (Long) ((LdcInsnNode) insn).cst;
        }

        throw new IllegalArgumentException("Unexpected instruction");
    }
}
