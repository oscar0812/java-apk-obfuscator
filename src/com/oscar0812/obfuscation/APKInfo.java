package com.oscar0812.obfuscation;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.File;

public class APKInfo {
    private final String apkName = "sample_navigation.apk";
    private File outputDir;
    private String manifestPackage;

    private static APKInfo instance = null;

    public static APKInfo getInstance() {
        if(instance == null) {
            instance = new APKInfo();
        }
        return instance;
    }

    public String getManifestPackage() {
        return manifestPackage;
    }

    public void setOutputDir(File file) {
        outputDir = file;
        manifestPackage = getAPKPackage();
    }

    public File getOutputDir() {
        return outputDir;
    }

    // get android package name from android manifest
    private String getAPKPackage(){
        if(outputDir == null) {
            return null;
        }
        File file = new File(outputDir, "AndroidManifest.xml");
        Document document;
        try {
            document = new SAXReader().read(file);
        } catch (DocumentException e) {
            e.printStackTrace();
            return null;
        }

        assert document != null;
        Attribute p = document.getRootElement().attribute("package");
        return (String) p.getData();
    }

    public String getName() {
        return apkName;
    }
}
