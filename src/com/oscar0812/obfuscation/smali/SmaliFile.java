package com.oscar0812.obfuscation.smali;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class SmaliFile extends File {
    private final ArrayList<SmaliLine> lines = new ArrayList<>();
    private String smaliPackage = "";
    private String smaliClass = "";

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

        // System.out.println("PROCESSED: "+getAbsolutePath());
    }

    private void processSingleLine(String line) {
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
            FileWriter writer = new FileWriter(new File(getAbsolutePath()), false);

            for (SmaliLine line: lines) {
                writer.write(line.getOriginalText());
                writer.write("\n");
                if(line.getParts()[0].equals(".end")) {
                    writer.write("\n");
                }
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
