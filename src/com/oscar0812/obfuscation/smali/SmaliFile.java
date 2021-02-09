package com.oscar0812.obfuscation.smali;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class SmaliFile extends File {
    // chain the lines
    private SmaliLine firstSmaliLine = null;
    private SmaliLine lastSmaliLine = null;

    private final HashMap<String, ArrayList<SmaliLine>> firstWordSmaliLineMap = new HashMap<>(); // first word->list[SmaliLines]: ".field"->[....], "const-string"->[...]

    // what lines reference/link/use this file
    private final ArrayList<SmaliLine> referencedInlines = new ArrayList<>();
    private String smaliPackage = "";
    private String smaliClass = "";

    // field lines
    private final ArrayList<SmaliField> childFieldList = new ArrayList<>();
    private final HashMap<String, SmaliField> childFieldMap = new HashMap<>();

    // file lines (parent - child)
    private final HashMap<String, SmaliFile> childFileMap = new HashMap<>();
    private final HashMap<String, SmaliFile> parentFileMap = new HashMap<>();

    // method blocks
    private final ArrayList<SmaliMethod> childMethodList = new ArrayList<>();

    // getCurrentTrack() -> [line, line]
    private final HashMap<String, SmaliMethod> childMethodWithNoReturnMap = new HashMap<>();

    private final HashMap<String, ArrayList<SmaliLine>> methodReferences = new HashMap<>(); // link method name to lines that reference it
    private final HashMap<String, ArrayList<SmaliLine>> fieldReferences = new HashMap<>();  // link field name to lines that reference it

    public long debugLine = 50;

    public SmaliFile(String pathname) {
        super(pathname);
    }

    public SmaliFile(File parent, String child) {
        super(parent, child);
    }

    public ArrayList<SmaliLine> getReferencedInSmaliLines() {
        return referencedInlines;
    }

    public void addReferenceSmaliLine(SmaliLine inLine) {
        referencedInlines.add(inLine);

        inLine.getReferenceSmaliFileList().add(this);
        inLine.getReferenceSmaliFileMap().put(this.getSmaliPackage(), this);

        // check if parent - child
        if (inLine.getParts()[0].equals(".implements")) {
            this.getChildFileMap().put(inLine.getParentFile().getAbsolutePath(), inLine.getParentFile());
            inLine.getParentFile().getParentFileMap().put(this.getAbsolutePath(), this);
        }
    }

    public void appendString(String text) {
        for (String s : text.split("\\r?\\n|\\r")) { // split text by new line
            appendSmaliLine(new SmaliLine(s, this));
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
                appendSmaliLine(new SmaliLine(line, this));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendSmaliLine(SmaliLine sl) {
        if (firstSmaliLine == null) {
            firstSmaliLine = sl;
        }
        if (lastSmaliLine == null) {
            lastSmaliLine = sl;
        } else {
            lastSmaliLine = lastSmaliLine.insertAfter(sl);
        }

        // link the first word to a list of smali lines
        String firstWord = sl.getParts()[0];
        if (!firstWordSmaliLineMap.containsKey(firstWord))
            this.firstWordSmaliLineMap.put(firstWord, new ArrayList<>());
        firstWordSmaliLineMap.get(firstWord).add(sl);
    }

    public void addFieldLine(SmaliLine smaliLine) {
        SmaliField sf = new SmaliField(smaliLine);
        childFieldList.add(sf);
        childFieldMap.put(sf.getFullField(), sf);
    }

    public void addMethodLine(SmaliLine smaliLine) {
        String[] parts = smaliLine.getParts();
        if (parts[0].equals(".method")) {
            // start of a method
            SmaliMethod sm = new SmaliMethod(this, smaliLine);
            childMethodList.add(sm);

            // update the hashmap, to search for method faster by name
            String id = sm.getMethodIdentifier();
            childMethodWithNoReturnMap.put(id.substring(0, id.indexOf(")") + 1), sm);
        } else if (childMethodList.size() > 0 && !childMethodList.get(childMethodList.size() - 1).isEnded()) {
            // this line is part of a method
            if (parts[0].equals(".end") && parts[1].equals("method")) {
                SmaliMethod sm = childMethodList.get(childMethodList.size() - 1);
                smaliLine.setParentMethod(sm);
                sm.setLastLine(smaliLine);
            }
        }
    }

    public void saveToDisk() {
        try {
            if (exists() || createNewFile()) {

                FileWriter writer = new FileWriter(getAbsolutePath(), false);

                SmaliLine line = firstSmaliLine;
                while (line != null) {
                    if (line.getParts()[0].equals(".method") && line.getPrevSmaliLine() != null
                            && !line.getPrevSmaliLine().isEmpty() && !line.getPrevSmaliLine().isComment()) {
                        // we need a space before .method (bothers me)
                        writer.write("\n");
                    }

                    writer.write(line.getText());
                    writer.write("\n");

                    line = line.getNextSmaliLine();
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

    public HashMap<String, SmaliMethod> getChildMethodWithNoReturnMap() {
        return childMethodWithNoReturnMap;
    }

    public ArrayList<SmaliField> getChildFieldList() {
        return childFieldList;
    }

    public HashMap<String, SmaliField> getChildFieldMap() {
        return childFieldMap;
    }

    public HashMap<String, ArrayList<SmaliLine>> getMethodReferences() {
        return methodReferences;
    }

    public HashMap<String, ArrayList<SmaliLine>> getFieldReferences() {
        return fieldReferences;
    }

    public HashMap<String, ArrayList<SmaliLine>> getFirstWordSmaliLineMap() {
        return firstWordSmaliLineMap;
    }

    public HashMap<String, SmaliFile> getChildFileMap() {
        return childFileMap;
    }

    public HashMap<String, SmaliFile> getParentFileMap() {
        return parentFileMap;
    }
}
