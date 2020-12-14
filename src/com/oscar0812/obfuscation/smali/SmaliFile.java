package com.oscar0812.obfuscation.smali;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class SmaliFile extends File {
    private final ArrayList<SmaliLine> lines = new ArrayList<>();
    private SmaliFile baseSmaliDir;
    private String smaliClass = "";

    public SmaliFile(String pathname) {
        super(pathname);
    }

    public ArrayList<SmaliLine> getLines() {
        return lines;
    }

    public void appendString(String text) {
        for(String s: text.split("\\r?\\n")) { // split text by new line
            processSingleLine(s);
        }
    }

    public void setBaseSmaliDir(SmaliFile baseSmaliDir) {
        this.baseSmaliDir = baseSmaliDir;
    }

    public SmaliFile getBaseSmaliDir() {
        return baseSmaliDir;
    }

    public String getSmaliClass() {
        return smaliClass;
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
        ArrayList<SmaliLine> processedLines = SmaliLine.process(line);

        if(smaliClass.isEmpty()) {
            // set the package
            // .class public Lcom/oscar0812/sample_navigation/StringUtil;
            for(SmaliLine l: processedLines) {
                String[] parts = l.getParts();
                if(parts[0].equals(".class")) {
                    smaliClass = parts[parts.length-1];
                    break;
                }
            }
        }

        lines.addAll(processedLines);
    }
}
