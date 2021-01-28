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

    private SmaliFile RSmaliFile;
    private final HashMap<String, SmaliFile> smaliFileMap = new HashMap<>();
    private final ArrayList<SmaliFile> smaliFileList = new ArrayList<>();

    private final ArrayList<File> manifestAppFiles = new ArrayList<>();

    private static APKInfo instance = null;

    public static APKInfo getInstance() {
        if (instance == null) {
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

                // TODO: what about apks with smali/ AND smali_classes2/
                smaliDir = new File(apkDecompileDir, "smali");
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

    public HashMap<String, SmaliFile> getSmaliFileMap() {
        return smaliFileMap;
    }

    public ArrayList<SmaliFile> getSmaliFileList() {
        return smaliFileList;
    }

    public void addSmaliFile(SmaliFile smaliFile) {
        // quick access through path
        smaliFileMap.put(smaliFile.getAbsolutePath(), smaliFile);
        smaliFileList.add(smaliFile);
    }

    public void fetchDecompiledInfo() {
        manifestFileInfo();
        fetchSmaliFiles();
    }

    // get android info from android manifest
    private void manifestFileInfo() {
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

        Set<String> visitedFiles = new HashSet<>();

        for (Element manifestChildren : manifestTag.elements()) {
            if (manifestChildren.getName().equals("application")) {
                for (Element applicationChildren : manifestChildren.elements()) {
                    // application children (activity, receiver, service, ...)
                    String attrValue = applicationChildren.attributeValue("name"); // com.oscar0812.sample_navigation.MainActivity

                    String sf = attrValue.replace(".", File.separator) + ".smali"; // com\oscar0812\sample_navigation\MainActivity.smali

                    File file = new File(smaliDir, sf);
                    if (!visitedFiles.contains(file.getAbsolutePath())) {
                        if (file.exists()) {
                            manifestAppFiles.add(file);
                        }
                        visitedFiles.add(file.getAbsolutePath());
                    }

                }
            }
        }
    }

    private void fetchSmaliFiles() {
        // ok got the main files, now search for R.smali, that should tell us what the root directory of the apk is
        Queue<File> q = new LinkedList<>(manifestAppFiles);
        Set<String> visitedFiles = new HashSet<>();

        while (!q.isEmpty()) {
            File f = q.poll();
            File parent = f.getParentFile();

            // only visit a folder once. Don't waste time
            if (!visitedFiles.contains(parent.getAbsolutePath())) {
                // System.out.println("CHECKING: "+f.getAbsolutePath());
                File r = new File(parent, "R.smali");

                if (r.exists()) {
                    File rID = new File(parent, "R$id.smali");
                    // every R.smali comes with a R$id.smali by design
                    if (rID.exists()) {
                        RSmaliFile = new SmaliFile(parent, "R.smali");
                        break;
                    }
                } else if (!parent.getAbsolutePath().equals(smaliDir.getAbsolutePath())) {
                    // can still go back
                    q.add(parent.getParentFile());
                }

                visitedFiles.add(parent.getAbsolutePath());
            }
        }

        assert RSmaliFile != null;

        // got R.smali, now get all files in smali/main_package directory
        // meh recursion, use queue
        q.clear();
        q.add(RSmaliFile.getParentFile());

        // to set the smali packages
        HashMap<String, String> pathToPackage = new HashMap<>();
        Stack<File> packageStack = new Stack<>();
        pathToPackage.put(smaliDir.getAbsolutePath(), ""); // base package

        while (!q.isEmpty()) {
            File parent = q.poll(); // retrieve and remove the first element
            File[] files = parent.listFiles();

            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".smali")) {
                        // append this smali file
                        SmaliFile sf = new SmaliFile(file.getAbsolutePath());

                        // get package. Bubble up to known parent
                        File bubbler = file.getParentFile();
                        while (!pathToPackage.containsKey(bubbler.getAbsolutePath())) {
                            packageStack.add(bubbler);
                            bubbler = bubbler.getParentFile();
                        }

                        // Bubble down to current file and set the trail of paths
                        StringBuilder builder = new StringBuilder(pathToPackage.get(bubbler.getAbsolutePath()));
                        while (!packageStack.empty()) {
                            bubbler = packageStack.pop();
                            builder.append(bubbler.getName()).append("/");
                            pathToPackage.put(bubbler.getAbsolutePath(), builder.toString());
                        }

                        // set package
                        sf.setSmaliPackage("L" + builder.toString() + file.getName() + ";");
                        addSmaliFile(sf);
                    }
                } else if (file.isDirectory()) {
                    // found directory
                    q.add(file);
                }
            }
        }
    }

    public String getName() {
        return apkName;
    }
}
