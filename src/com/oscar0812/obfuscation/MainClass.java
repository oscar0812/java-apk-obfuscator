package com.oscar0812.obfuscation;

import brut.common.BrutException;
import com.oscar0812.obfuscation.smali.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    private void connectParents(ArrayList<SmaliFile> arr) {
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

    private void obfuscateStrings(ArrayList<SmaliLine> smaliLines, SmaliLineObfuscator slo) {
        // obfuscate strings
        // DOESN'T WORK: line 16 of SongLoader: "is_music=1 AND title != \'\'"
        for (SmaliLine smaliLine : smaliLines) {
            String[] parts = smaliLine.getParts();
            if (parts.length > 2 && parts[parts.length - 1].contains("\\'\\'") && parts[1].equals("v0,")) {
                // weird case with const-string if it contains \'\' and stored at v0
                continue;
            }

            if (smaliLine.getParentMethod() != null && !smaliLine.isGarbage()) {
                SmaliMethod smaliMethod = smaliLine.getParentMethod();
                if (!smaliMethod.isConstructor()) {
                    // dont obfuscate constructor string (for now)
                    SmaliLine obfCall = slo.stringToStaticCall(smaliLine);
                    smaliLine.getPrevSmaliLine().insertAfter(obfCall);
                    smaliLine.delete();
                }
            }
        }
    }

    private void obfuscateMethod(SmaliMethod smaliMethod) {
        if (!smaliMethod.canRename()) {
            // can't rename constructors
            return;
        }

        HashMap<String, ArrayList<SmaliLine>> methodReferenceMap = smaliMethod.getParentFile().getMethodReferences();

        if (methodReferenceMap.containsKey(smaliMethod.getMethodIdentifier())) {
            // this file created this method and lines are calling it
            // what about parent class method overriding? will that be an issue?
            smaliMethod.changeMethodName();
        }
    }

    // start the obfuscation process
    public void obfuscate() {

        // TODO: read the files in parallel to finish faster (might be alot of files)
        for (SmaliFile sf : APKInfo.getInstance().getSmaliFileList()) {
            sf.processLines();
        }

        ArrayList<SmaliFile> copy = new ArrayList<>(APKInfo.getInstance().getSmaliFileList()); // since it changes

        // sort, put parent files first
        copy.sort((a, b) -> Integer.compare(b.getChildFileMap().size(), a.getChildFileMap().size()));

        connectParents(copy);

        // all lines are processed, time to obfuscate
        for (SmaliFile smaliFile : copy) {

            if(APKInfo.getInstance().getRFileMap().containsKey(smaliFile.getAbsolutePath())) {
                // don't obfuscate R files, we need them intact for xml obfuscation
                continue;
            }

            // System.out.println("\"" + smaliFile.getAbsolutePath() + "\",");
            HashMap<String, ArrayList<SmaliLine>> smaliLineMap = smaliFile.getFirstWordSmaliLineMap();

            // Obfuscate strings
            if (smaliLineMap.containsKey("const-string")) {
                obfuscateStrings(smaliLineMap.get("const-string"), SmaliLineObfuscator.getInstance());
            }

            // Obfuscate methods (method name change)
            ArrayList<SmaliMethod> fileMethods = new ArrayList<>(smaliFile.getMethodList());
            for (SmaliMethod smaliMethod : fileMethods) {
                obfuscateMethod(smaliMethod);
            }

            // obfuscate fields (field name change)
            ArrayList<SmaliField> fileFields = new ArrayList<>(smaliFile.getFieldList());
            for (SmaliField smaliField : fileFields) {
                smaliField.changeFieldName();
            }

            // TODO: Obfuscate classes (class name change)

            // remove debugging lines
            // String[] ignoreStart = new String[]{".line", ".local", ".param", "#"};
            String[] ignoreStart = new String[]{".line", "#"};
            for (String is : ignoreStart) {
                if (smaliLineMap.containsKey(is)) {
                    for (SmaliLine sl : smaliLineMap.get(is)) {
                        // sl.delete();
                    }
                }
            }

            // other...
        }

        // save
        for (SmaliFile smaliFile : APKInfo.getInstance().getSmaliFileList()) {
            smaliFile.saveToDisk();
        }

    }


    private void start() {
        APKInfo.setApkName("timber.apk");
        // APKInfo.setApkName("sample_navigation.apk");
        APKInfo info = APKInfo.getInstance();
        File apkFile = info.getApkFile();
        File apkDir = info.getProjectApkDir();
        File outputDir = info.getApkDecompileDir();

        decompileWithAPKTool(apkFile, outputDir);
        APKInfo.getInstance().fetchDecompiledInfo();

        System.out.println("==== DONE DECOMPILING ====");
        // TODO: work on obfuscate

        obfuscate();

        buildWithAPKTool(outputDir);
        signAPKWithUber(apkFile, apkDir, outputDir);

    }

    // TODO: what about reflective methods?
    // obfuscate xml?
    // obfuscate assets?

    // TODO: timber.apk crashes after const-string obfuscation, but doesn't if some lines are left out.
    // TODO: track down the problem by NOT obfuscating some files and check if it still works, recursively

    public static void main(String[] args) {
        MainClass m = new MainClass();
        m.start();

        // SmaliStringObfuscator s = new SmaliStringObfuscator()
    }
}
