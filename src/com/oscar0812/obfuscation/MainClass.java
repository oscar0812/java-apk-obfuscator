package com.oscar0812.obfuscation;

import brut.common.BrutException;
import com.oscar0812.obfuscation.smali.SmaliFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
 *       C:\Users\oscar\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat localhost:63543
 *          63543 is the port in adb settings (gear icon -> preferences -> scroll down)
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

    // TODO: read the files in parallel to finish faster (might be alot of files)
    private void processFiles(ArrayList<SmaliFile> smaliFiles) {
        for(SmaliFile s: smaliFiles) {
            //service.execute(s::processLines);
            s.processLines();
        }
    }

    // start the obfuscation process
    public void obfuscate() {
        ArrayList<SmaliFile> smaliFiles = APKInfo.getInstance().getSmaliFileList();
        processFiles(smaliFiles); // read files line by line and extract SmaliLine's (class)
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

        // obfuscate();
        int a = 1;
        /*
        buildWithAPKTool(outputDir);
        signAPKWithUber(apkFile, apkDir, outputDir);
         */
    }

    // TODO: what about reflective methods?
    // obfuscate xml?
    // obfuscate assets?

    public static void main(String[] args) {
        MainClass m = new MainClass();
        m.start();

        // SmaliStringObfuscator s = new SmaliStringObfuscator()
    }
}
