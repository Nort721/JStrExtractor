package com.nort721.extractor.scanners;

import com.nort721.extractor.JarData;

import java.util.List;

public abstract class JarScanner {
    protected JarData jarData;

    public JarScanner(JarData jarData) {
        this.jarData = jarData;
    }
    public abstract List<String> scan();
}
