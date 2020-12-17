package com.oscar0812.obfuscation;

import brut.common.BrutException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/*
 * STEPS:
 *   1: decompile app with apktool
 *       1.a: brut.apktool.Main.main(args)
 *   2: obfuscate smali code
 *   3: compile app with apktool
 *       3.a: brut.apktool.Main.main(args)
 *   4: sign app with uber signer
 *       4.a: at.favre.tools.apksigner.SignTool.main(args)
 * */



public class MainClass {
    public static final boolean REMOVE_DOT_LINE = false;
    public static final boolean ONLY_OBFUSCATE_MAIN_PACKAGE = true; // for debugging, dont want to wait

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


    private void start() {
        // apk has to be in apks/ folder
        File apkDir = new File(System.getProperty("user.dir") + File.separator + "apks");
        if (!apkDir.exists()) {
            System.out.println("No APKS directory");
            return;
        }

        File apkFile = new File(apkDir, APKInfo.getInstance().getName());

        if (!apkFile.exists()) {
            System.out.println("APK file doesn't exist");
            return;
        }

        System.out.println("APK file: " + apkFile);

        // remove the .apk and make it a directory for output (apktool write)
        File outputDir = new File(apkFile.getAbsolutePath().substring(0, apkFile.getAbsolutePath().lastIndexOf('.')));

        APKInfo.getInstance().setOutputDir(outputDir);

        // decompileWithAPKTool(apkFile, outputDir);
        System.out.println("==== DONE DECOMPILING ====");
        // TODO: work on obfuscate
        // StartProcess obf = new StartProcess();
        // obf.obfuscate();
        buildWithAPKTool(outputDir);
        signAPKWithUber(apkFile, apkDir, outputDir);
    }

    public static void main(String[] args) {
        MainClass m = new MainClass();
        m.start();

        // SmaliStringObfuscator s = new SmaliStringObfuscator()
    }
}
