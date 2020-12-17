package com.oscar0812.obfuscation;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.oscar0812.obfuscation.smali.SmaliFile;
import com.oscar0812.obfuscation.smali.SmaliLine;
import org.dom4j.io.SAXReader;
import org.dom4j.*;

public class StartProcess {
    // remove .line makes for harder debugging
    // (https://stackoverflow.com/questions/18274031/what-does-line-mean-in-smali-code-syntax-android-smali-code)

    public ArrayList<SmaliFile> findFiles(File root)
    {
        // meh recursion, use queue
        ArrayList<SmaliFile> smaliFiles = new ArrayList<>();
        Queue<File> q = new LinkedList<>();
        q.add(root);

        while (!q.isEmpty()) {
            File parent = q.poll(); // retrieve and remove the first element
            File[] files = parent.listFiles();

            if(files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isFile()) {
                    if(file.getName().endsWith(".smali")) {
                        // append this smali file and set the base smali/ directory
                        SmaliFile sf = new SmaliFile(file.getAbsolutePath());
                        smaliFiles.add(sf);
                    }
                } else if (file.isDirectory()) {
                    // found directory
                    q.add(file);
                }
            }
        }

        return smaliFiles;
    }



    // read the files in parallel to finish faster (might be alot of files)
    private void processFiles(ArrayList<SmaliFile> smaliFiles) {

        // TEST
        // smaliFiles.get(0).processLines();


        // start the max number of threads for this machine
        // ExecutorService service = Executors.newFixedThreadPool(2);
        for(SmaliFile s: smaliFiles) {
            //service.execute(s::processLines);
            s.processLines();
        }

        // shutdown
        // this will get blocked until all task finish
        /*
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

         */


    }

    // start the obfuscation process
    public void obfuscate() {

        // TODO: what about apks with smali/ AND smali_classes2/
        File smaliDir = new File(APKInfo.getInstance().getOutputDir(), "smali");
        if(!(smaliDir.exists() && smaliDir.isDirectory())) {
            System.out.println("SMALI folder not found");
            return;
        }

        // get all files in smali/ directory
        ArrayList<SmaliFile> smaliFiles = findFiles(smaliDir);

        processFiles(smaliFiles); // read files line by line and extract SmaliLine's (class)

    }
}
