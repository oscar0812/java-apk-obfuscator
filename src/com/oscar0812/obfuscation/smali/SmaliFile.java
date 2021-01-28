package com.oscar0812.obfuscation.smali;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class SmaliFile extends File {
    private final ArrayList<SmaliLine> childLines = new ArrayList<>();
    // what lines reference/link/use this file
    private final ArrayList<SmaliLine> referencedInlines = new ArrayList<>();
    private String smaliPackage = "";
    private String smaliClass = "";

    private final ArrayList<SmaliMethod> childMethodList = new ArrayList<>();
    private final HashMap<String, ArrayList<SmaliMethod>> childMethodMap = new HashMap<>(); // link method name to method object

    public long debugLine = 50;

    public SmaliFile(String pathname) {
        super(pathname);
    }

    public SmaliFile(File parent, String child) {
        super(parent, child);
    }

    public ArrayList<SmaliLine> getChildLines() {
        return childLines;
    }

    public ArrayList<SmaliLine> getReferencedInSmaliLines() {
        return referencedInlines;
    }

    public void addReferenceSmaliLine(SmaliLine inLine) {
        referencedInlines.add(inLine);
    }

    public void appendString(String text) {
        for (String s : text.split("\\r?\\n|\\r")) { // split text by new line
            SmaliLine.process(s, this);
        }
    }

    public String getSmaliPackage() {
        return smaliPackage;
    }

    // setting the package, so override the class
    public void setSmaliPackage(String smaliPackage) {
        this.smaliPackage = smaliPackage; // Lcom/oscar0812/sample_navigation/StringUtil;
        this.smaliClass = smaliPackage.substring(smaliPackage.lastIndexOf("/") + 1, smaliPackage.length() - 1); // StringUtil
    }

    public String getSmaliClass() {
        return smaliClass;
    }

    // THIS method starts the process for this file
    public void processLines() {
        // System.out.println("PROCESSING: "+getAbsolutePath());
        // read file line by line
        try (Scanner scanner = new Scanner(new File(getAbsolutePath()))) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                SmaliLine.process(line, this);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        saveToDisk();
        // System.out.println("PROCESSED: "+getAbsolutePath());
    }

    public void saveToDisk() {
        try {
            if (exists() || createNewFile()) {

                FileWriter writer = new FileWriter(getAbsolutePath(), false);

                for (SmaliLine line : childLines) {
                    writer.write(line.getOriginalText());
                    writer.write("\n");
                    if (line.getParts()[0].equals(".end")) {
                        writer.write("\n");
                    }
                }

                writer.close();
            } else {
                System.out.println("COULDN'T CREATE FILE: " + getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public ArrayList<SmaliMethod> getChildMethodList() {
        return childMethodList;
    }

    public HashMap<String, ArrayList<SmaliMethod>> getChildMethodMap() {
        return childMethodMap;
    }
}
