package com.oscar0812.obfuscation;

import com.oscar0812.obfuscation.smali.SmaliFile;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;

public class APKInfo {
    private static String apkName;
    private final File projectApkDir;
    private File apkFile;
    private File apkDecompileDir;
    private File smaliDir;
    private String manifestPackage;

    private SmaliFile RSmaliFile;

    private final ArrayList<File> mainManifestFiles = new ArrayList<>();

    private static APKInfo instance = null;

    public static APKInfo getInstance() {
        if(instance == null) {
            instance = new APKInfo();
        }
        return instance;
    }

    public static void setApkName(String inName) {
        apkName = inName;
        instance = new APKInfo();
    }

    public APKInfo() {
        // apk has to be in apks/ folder
        projectApkDir = new File(System.getProperty("user.dir") + File.separator + "apks");
        if (!projectApkDir.exists()) {
            System.out.println("No APKS directory");
        } else {
            // exists
            apkFile = new File(projectApkDir, apkName);

            if (!apkFile.exists()) {
                System.out.println("APK file doesn't exist");
            } else {
                // remove the .apk and make it a directory for output (apktool write)
                apkDecompileDir = new File(apkFile.getAbsolutePath().substring(0, apkFile.getAbsolutePath().lastIndexOf('.')));
                smaliDir = new File(apkDecompileDir, "smali");

                manifestFileInfo();
                fetchRSmali();
            }
        }
    }

    public File getProjectApkDir() {
        return projectApkDir;
    }

    public File getApkFile() {
        return apkFile;
    }

    public File getApkDecompileDir() {
        return apkDecompileDir;
    }

    public File getSmaliDir() {
        return smaliDir;
    }

    public String getManifestPackage() {
        return manifestPackage;
    }

    public SmaliFile getRSmaliFile() {
        return RSmaliFile;
    }

    public ArrayList<File> getMainManifestFiles() {
        return mainManifestFiles;
    }

    // get android info from android manifest
    private void manifestFileInfo(){
        File manifestFile = new File(apkDecompileDir, "AndroidManifest.xml");
        Document document;
        try {
            document = new SAXReader().read(manifestFile);
        } catch (DocumentException e) {
            e.printStackTrace();
            return;
        }

        assert document != null;
        Element manifestTag = document.getRootElement();
        manifestPackage = (String)manifestTag.attribute("package").getData();

        Set<String> visitedFiles = new HashSet<>();

        for (Element manifestChildren: manifestTag.elements()) {
            if(manifestChildren.getName().equals("application")) {
                for(Element applicationChildren: manifestChildren.elements()){
                    // application children (activity, receiver, service, ...)
                    String attrValue = applicationChildren.attributeValue("name"); // com.oscar0812.sample_navigation.MainActivity
                    // don't check library files
                    if(attrValue.contains(manifestPackage)) {
                        String sf = attrValue.replace(".", File.separator) + ".smali"; // com\oscar0812\sample_navigation\MainActivity.smali

                        File file = new File(smaliDir, sf);
                        if (!visitedFiles.contains(file.getAbsolutePath())) {
                            if (file.exists()) {
                                mainManifestFiles.add(file);
                            }
                            visitedFiles.add(file.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    private void fetchRSmali() {
        // ok got the main files, now search for R.smali, that should tell us what the root directory of the apk is
        Queue<File> q = new LinkedList<>(mainManifestFiles);
        Set<String> visitedFiles = new HashSet<>();

        while (!q.isEmpty()) {
            File f = q.poll();
            File parent = f.getParentFile();

            // only visit a folder once. Don't waste time
            if(!visitedFiles.contains(parent.getAbsolutePath())) {
                System.out.println("CHECKING: "+f.getAbsolutePath());
                File r = new File(parent, "R.smali");

                if (r.exists()) {
                    File rID = new File(parent, "R$id.smali");
                    // every R.smali comes with a R$id.smali by design
                    if(rID.exists()) {
                        RSmaliFile = new SmaliFile("R.smali");
                        break;
                    }
                } else if(!parent.getAbsolutePath().equals(smaliDir.getAbsolutePath())) {
                    // can still go back
                    q.add(parent.getParentFile());
                }

                visitedFiles.add(parent.getAbsolutePath());
            }
        }
    }

    public String getName() {
        return apkName;
    }
}
