package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.APKInfo;
import com.oscar0812.obfuscation.MainClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class SmaliFile extends File {
    private final ArrayList<SmaliLine> lines = new ArrayList<>();
    private String smaliPackage = "";
    private String smaliClass = "";

    public long debugLine = 50;

    public SmaliFile(String pathname) {
        super(pathname);
    }

    public SmaliFile(File parent, String child) {
        super(parent, child);
    }

    public ArrayList<SmaliLine> getLines() {
        return lines;
    }

    public void appendString(String text) {
        for(String s: text.split("\\r?\\n|\\r")) { // split text by new line
            processSingleLine(s);
        }
    }

    public String getSmaliPackage() {
        return smaliPackage;
    }

    // setting the package, so override the class
    public void setSmaliPackage(String smaliPackage) {
        this.smaliPackage = smaliPackage; // Lcom/oscar0812/sample_navigation/StringUtil;
        this.smaliClass = smaliPackage.substring(smaliPackage.lastIndexOf("/") + 1, smaliPackage.length()-1); // StringUtil
    }

    public String getSmaliClass() {
        return smaliClass;
    }

    public void setSmaliClass(String smaliClass) {
        this.smaliClass = smaliClass;
    }

    // THIS method starts the process for this file
    public void processLines() {
        // System.out.println("PROCESSING: "+getAbsolutePath());
        // read file line by line
        try (Scanner scanner = new Scanner(new File(getAbsolutePath()))) {
            while (scanner.hasNext()){
                // check if line is valid
                String line = scanner.nextLine();
                processSingleLine(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        saveToDisk();

        // System.out.println("PROCESSED: "+getAbsolutePath());
    }

    private void processSingleLine(String line) {
        String manifestPackage = APKInfo.getInstance().getManifestPackage(); // com.oscar0812.sample_navigation
        String mainSmaliPackage = "L"+manifestPackage.replace(".", "/")+"/"; // Lcom/oscar0812/sample_navigation/

        if(!smaliPackage.isEmpty() && MainClass.ONLY_OBFUSCATE_MAIN_PACKAGE && !smaliPackage.startsWith(mainSmaliPackage)) {
            // DO NOT PROCESS
            lines.add(new SmaliLine(line, this));
            return;
        }

        ArrayList<SmaliLine> processedLines = SmaliLine.process(line, this);

        if(smaliPackage.isEmpty()) {
            // set the package
            // .class public Lcom/oscar0812/sample_navigation/StringUtil;
            for(SmaliLine l: processedLines) {
                String[] parts = l.getParts();
                if(parts[0].equals(".class")) {
                    setSmaliPackage(parts[parts.length-1]);

                    break;
                }
            }
        }

        lines.addAll(processedLines);
    }

    public void saveToDisk() {
        try {
            if(exists() || createNewFile()) {

                FileWriter writer = new FileWriter(new File(getAbsolutePath()), false);

                for (SmaliLine line : lines) {
                    writer.write(line.getOriginalText());
                    writer.write("\n");
                    if (line.getParts()[0].equals(".end")) {
                        writer.write("\n");
                    }
                }

                writer.close();
            } else {
                System.out.println("COULDN'T CREATE FILE: "+getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
