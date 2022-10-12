package com.nort721.extractor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class JarData {
    private final List<ClassNode> classes;
    private final List<byte[]> sourceBytes;
    private final Map<String, byte[]> fileMap;
    @Setter private String mainClass = "";
}
