package com.oscar0812.obfuscation;

import brut.common.BrutException;
import com.oscar0812.obfuscation.res.ResourceInfo;
import com.oscar0812.obfuscation.res.XMLFile;
import com.oscar0812.obfuscation.smali.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/*
 * STEPS:
 *   1: decompile app with apktool
 *       1.a: brut.apktool.Main.main(args)
 *   2: obfuscate smali code
 *   3: compile app with apktool
 *       3.a: brut.apktool.Main.main(args)
 *   4: sign app with uber signer
 *       4.a: at.favre.tools.apksigner.SignTool.main(args)
 *
 *
 *   CONNECT TO BLUESTACKS ADB-LOGCAT:
 *       C:\Users\oscar\AppData\Local\Android\Sdk\platform-tools\adb.exe connect localhost:63543
 *       C:\Users\oscar\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat localhost:63543
 *          63543 is the port in adb settings (gear icon -> preferences -> scroll down)
 *
 *   use jadx.exe to look at apk .dex source code
 *
 *   papers:
 *      https://arxiv.org/pdf/1809.11037.pdf
 * */


public class MainClass {

    private void callAPKTool(String[] params) {
        System.out.println("\n" + Arrays.toString(params));
        try {
            brut.apktool.Main.main(params);
        } catch (IOException | InterruptedException | BrutException e) {
            e.printStackTrace();
        }
    }

    private void decompileWithAPKTool(File apkFile, File outputDir) {
        String[] apk_decompile_params = {"d", "-f", apkFile.getAbsolutePath(), "--output", outputDir.getAbsolutePath()};
        callAPKTool(apk_decompile_params);
        // tell apkinfo that we decompiled the apk
    }

    private void buildWithAPKTool(File outputDir) {
        // --use-aapt2 is needed for new applications
        String[] apk_build_params = {"b", "--use-aapt2", outputDir.getAbsolutePath()};
        callAPKTool(apk_build_params);
    }

    private void signAPKWithUber(File apkFile, File apkDir, File outputDir) {
        // APKTool builds into outputDir/dist/name.apk
        File buildAPK = new File(outputDir, "dist" + File.separator + apkFile.getName());

        if (!buildAPK.exists()) {
            System.out.println("ERROR while signing! Couldn't find " + buildAPK.getAbsolutePath());
            return;
        }

        // call the jar API
        String[] sign_params = {"-a", buildAPK.getAbsolutePath(), "--out", apkDir.getAbsolutePath()};
        at.favre.tools.apksigner.SignTool.main(sign_params);
    }

    // connect file to parent of parent of parent of...
    // and to child of child of ...
    // and to other files that have children together
    private void connectSmaliFileParents(ArrayList<SmaliFile> arr) {
        // bubble up to parents with no children
        ArrayList<SmaliFile> parents = new ArrayList<>();
        for (SmaliFile smaliFile : arr) {
            if (smaliFile.getChildFileMap().size() == 0) {
                break;
            }
            parents.add(smaliFile);
        }

        Queue<SmaliFile> q = new LinkedList<>(parents);
        while (!q.isEmpty()) {
            SmaliFile parentFile = q.poll();
            ArrayList<SmaliFile> allChildren = new ArrayList<>();
            Queue<SmaliFile> bubbler = new LinkedList<>();
            bubbler.add(parentFile);

            Set<String> checked = new HashSet<>();
            while (!bubbler.isEmpty()) {
                SmaliFile b = bubbler.poll();

                if (checked.contains(b.getAbsolutePath())) {
                    continue;
                }
                checked.add(b.getAbsolutePath());

                allChildren.addAll(b.getChildFileMap().values());
                bubbler.addAll(b.getChildFileMap().values());
            }

            // we have the child list (depth), so now assign
            for (SmaliFile childFile : allChildren) {
                childFile.getParentFileMap().put(parentFile.getAbsolutePath(), parentFile);
                parentFile.getChildFileMap().put(childFile.getAbsolutePath(), childFile);
            }
        }


        // connect married files: files that share a child
        for (int x = 0; x < arr.size(); x++) {
            SmaliFile parentFile1 = arr.get(x);
            Set<String> parentFile1Keys = parentFile1.getChildFileMap().keySet();
            if (parentFile1.getChildFileMap().size() == 0) {
                break;
            }
            for (int y = x + 1; y < arr.size(); y++) {
                SmaliFile parentFile2 = arr.get(y);
                Set<String> parentFile2Keys = parentFile2.getChildFileMap().keySet();
                if (!Collections.disjoint(parentFile1Keys, parentFile2Keys)) {
                    // disjoint return true if no elements in common, false o.t.w
                    parentFile1.getMarriedFileMap().put(parentFile2.getAbsolutePath(), parentFile2);
                    parentFile2.getMarriedFileMap().put(parentFile1.getAbsolutePath(), parentFile1);
                }
            }
        }
    }

    private void obfuscateStrings(SmaliFile smaliFile) {
        HashMap<String, ArrayList<SmaliLine>> smaliLineMap = smaliFile.getFirstWordSmaliLineMap();

        if (!smaliLineMap.containsKey("const-string")) {
            return;
        }

        ArrayList<SmaliLine> smaliLines = smaliLineMap.get("const-string");

        // obfuscate strings
        // DOESN'T WORK: line 16 of SongLoader: "is_music=1 AND title != \'\'"
        for (SmaliLine smaliLine : smaliLines) {
            String[] parts = smaliLine.getParts();
            if (parts.length > 2 && parts[parts.length - 1].contains("\\'\\'") && parts[1].equals("v0,")) {
                // if (parts.length > 2 && parts[parts.length - 1].contains("\\")) {
                // weird case with const-string if it contains \'\' and stored at v0
                continue;
            }

            if (smaliLine.getParentMethod() != null && !smaliLine.getParentMethod().isConstructor()) {
                SmaliLineObfuscator.getInstance().stringToStaticCall(smaliLine);
            }
        }
    }

    private void obfuscateInts(SmaliFile smaliFile) {
        HashMap<String, ArrayList<SmaliLine>> smaliLineMap = smaliFile.getFirstWordSmaliLineMap();

        if (!smaliLineMap.containsKey("const")) {
            return;
        }

        ArrayList<SmaliLine> smaliLines = smaliLineMap.get("const");

        // obfuscate const integers/longs
        for (SmaliLine smaliLine : smaliLines) {
            if (smaliLine.getParentMethod() != null && !smaliLine.getParentMethod().isConstructor()) {
                // dont obfuscate constructor const's (for now)
                SmaliLineObfuscator.getInstance().obfuscateConstInt(smaliLine);
            }
        }
    }

    private void obfuscateMethods(SmaliFile smaliFile) {
        ArrayList<SmaliMethod> fileMethods = new ArrayList<>(smaliFile.getMethodList());
        for (SmaliMethod smaliMethod : fileMethods) {
            smaliMethod.rename();
        }
    }

    private void obfuscateFields(SmaliFile smaliFile) {
        ArrayList<SmaliField> fileFields = new ArrayList<>(smaliFile.getFieldList());
        for (SmaliField smaliField : fileFields) {
            smaliField.rename();
        }
    }

    private void deleteDebugLines(SmaliFile smaliFile) {
        HashMap<String, ArrayList<SmaliLine>> smaliLineMap = smaliFile.getFirstWordSmaliLineMap();
        String[] ignoreStart = new String[]{".line", "#"};
        for (String is : ignoreStart) {
            if (smaliLineMap.containsKey(is)) {
                for (SmaliLine sl : smaliLineMap.get(is)) {
                    sl.delete();
                }
            }
        }
    }

    private void obfuscateRSmaliAndXML() {
        ResourceInfo.getInstance().parseValuesDir();
        HashMap<String, String> publicXMLNameMap = ResourceInfo.getInstance().getXMLNameAttrChangeMap();

        // loop through R.smali files
        for (SmaliFile smaliFile : APKInfo.getInstance().getRFileMap().values()) {
            String fn = smaliFile.getName();
            if (fn.equals("R$attr.smali")) {
                // attributes crash stuff, idk why
                continue;
            }

            for (SmaliField smaliField : new ArrayList<>(smaliFile.getFieldList())) {
                // smali: TextAppearance_Compat_Notification
                // XML:   TextAppearance.Compat.Notification
                String smaliToXMLName = smaliField.getIdentifier().replaceAll("[_]+", ".");
                String identifier = null;
                if (publicXMLNameMap.containsKey(smaliField.getIdentifier())) {
                    identifier = smaliField.getIdentifier();
                } else if (publicXMLNameMap.containsKey(smaliToXMLName)) {
                    identifier = smaliToXMLName;
                }

                if (identifier != null) {
                    smaliField.rename(publicXMLNameMap.get(identifier).replaceAll("[.]+", "_"));
                }
            }
        }

        // AndroidManifest uses values??? news to me
        XMLFile androidManifest = new XMLFile(APKInfo.getInstance().getApkDecompileDir(), "AndroidManifest.xml");
        ResourceInfo.getInstance().getAllXMLFiles().put(androidManifest.getAbsolutePath(), androidManifest);
        for (XMLFile xmlFile : ResourceInfo.getInstance().getAllXMLFiles().values()) {
            xmlFile.processLines();
            xmlFile.saveToDisk();
        }
    }

    private void renameDrawables() {
        // rename drawable files
        for (String from : ResourceInfo.getInstance().getRenameFilesMap().keySet()) {
            File fromFile = new File(from);
            File toFile = new File(ResourceInfo.getInstance().getRenameFilesMap().get(from));
            while (!fromFile.renameTo(toFile)) {
                System.out.println("COULDN'T RENAME: " + fromFile.getAbsolutePath() + " -> " + toFile.getAbsolutePath());
            }
            // System.out.println("RENAMED: " + fromFile.getAbsolutePath() + " -> " + toFile.getAbsolutePath());
        }
    }

    // we have to call rename() on all files first to replace class calls on all files, then we can rename the file in disk
    private void renameSmaliClassFiles() {
        HashMap<String, SmaliFile> rFileMap = APKInfo.getInstance().getRFileMap();

        for (SmaliFile smaliFile : APKInfo.getInstance().getProjectSmaliFileMap().values()) {
            if (rFileMap.containsKey(smaliFile.getAbsolutePath())) {
                continue;
            }
            smaliFile.rename();
        }

        HashMap<String, String> renamedMap = APKInfo.getInstance().getNewToOldRenamedFilePathMap();
        HashMap<String, SmaliFile> allSmaliMap = APKInfo.getInstance().getAllSmaliFileMap();
        HashMap<String, SmaliFile> projectSmaliMap = APKInfo.getInstance().getProjectSmaliFileMap();

        for (String newFilePath : renamedMap.keySet()) {
            String oldFilePath = renamedMap.get(newFilePath);
            SmaliFile smaliFile = allSmaliMap.get(oldFilePath);

            allSmaliMap.put(newFilePath, allSmaliMap.remove(smaliFile.getAbsolutePath()));
            projectSmaliMap.put(newFilePath, projectSmaliMap.remove(smaliFile.getAbsolutePath()));


            while (smaliFile.exists() && !smaliFile.delete()) {
                System.out.println("COULDN'T DELETE:: " + smaliFile.getAbsolutePath());
            }
        }
    }

    public void deleteEmptyDirs() {
        // delete empty directories
        Queue<File> q = new LinkedList<>();
        q.add(APKInfo.getInstance().getSmaliDir());

        Stack<File> allDirs = new Stack<>();

        while (!q.isEmpty()) {
            File qDir = q.poll();
            allDirs.push(qDir);

            File[] listFiles = qDir.listFiles();

            if (listFiles == null || listFiles.length == 0) {
                continue;
            }

            for (File f : listFiles) {
                if (f.isDirectory()) {
                    q.add(f);
                }
            }
        }

        // a stack will keep the deepest folders at the top
        while (!allDirs.empty()) {
            File dir = allDirs.pop();
            File[] listFiles = dir.listFiles();

            if (listFiles == null || listFiles.length == 0) {
                dir.delete();
            }
        }
    }

    // start the obfuscation process
    public void obfuscate() {
        APKInfo apkInfo = APKInfo.getInstance();
        // parallel process lines (7 seconds vs 31 seconds!)
        APKInfo.getInstance().getAllSmaliFileMap().values().parallelStream().forEach(SmaliFile::processLines);

        ArrayList<SmaliFile> smaliFiles = new ArrayList<>(APKInfo.getInstance().getProjectSmaliFileMap().values()); // since it changes

        // sort, put parent files first
        smaliFiles.sort((a, b) -> Integer.compare(b.getChildFileMap().size(), a.getChildFileMap().size()));

        connectSmaliFileParents(smaliFiles);

        // all lines are processed, time to obfuscate
        for (SmaliFile smaliFile : smaliFiles) {

            if (APKInfo.getInstance().getRFileMap().containsKey(smaliFile.getAbsolutePath())) {
                // don't obfuscate R files, we need them intact for xml obfuscation
                continue;
            }

            // System.out.println("\"" + smaliFile.getAbsolutePath() + "\",");

            obfuscateStrings(smaliFile);
            obfuscateInts(smaliFile);

            obfuscateMethods(smaliFile);
            obfuscateFields(smaliFile);
            // deleteDebugLines(smaliFile);

            // other...
        }

        // save the obf string classes to their files, since we need to start renaming
        for (String path : APKInfo.getInstance().getCreatedSmaliFileMap().keySet()) {
            SmaliFile smaliFile = APKInfo.getInstance().getCreatedSmaliFileMap().get(path);
            smaliFile.saveToDisk(path);
        }

        renameSmaliClassFiles();
        obfuscateRSmaliAndXML();
        renameDrawables();

        // save everything
        for (String path : APKInfo.getInstance().getAllSmaliFileMap().keySet()) {
            SmaliFile smaliFile = APKInfo.getInstance().getAllSmaliFileMap().get(path);
            smaliFile.saveToDisk(path);
        }

        deleteEmptyDirs();
    }

    private void start() {
        APKInfo.setAPKPath("C:\\Users\\oscar\\IdeaProjects\\APKobfuscation\\apks\\timber.apk");
        APKInfo info = APKInfo.getInstance();
        File apkFile = info.getApkFile();
        File apkParentDir = info.getApkParentDir();
        File outputDir = info.getApkDecompileDir();

        decompileWithAPKTool(apkFile, outputDir);

        APKInfo.getInstance().fetchDecompiledInfo();

        System.out.println("==== DONE DECOMPILING ====");
        obfuscate();

        buildWithAPKTool(outputDir);
        signAPKWithUber(apkFile, apkParentDir, outputDir);
    }

    public static void main(String[] args) {
        MainClass m = new MainClass();
        m.start();
    }
}