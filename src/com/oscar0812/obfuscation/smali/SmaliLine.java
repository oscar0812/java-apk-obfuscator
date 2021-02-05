package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.APKInfo;
import com.oscar0812.obfuscation.utils.StringUtils;
import com.oscar0812.obfuscation.utils.Substring;

import java.io.File;
import java.util.*;

/**
 * const/4 is for 0-7 (fit in 8 bits)
 * const/16 is for 8-127 (fit in 16 bits)
 * const is for arbitrary 32-bit
 * <p>
 * Remove .line # lines for harder debugging
 */

/**
 * HEART of the project. Every text line in every smali file is a "SmaliLine"
 */

public class SmaliLine {
    // chain the lines
    private SmaliLine prevSmaliLine = null;
    private SmaliLine nextSmaliLine = null;

    public static final String SINGLE_SPACE = "    ";
    public static final String DOUBLE_SPACE = "        ";
    public static final String TRIPLE_SPACE = "            ";

    public static final Set<String> IGNORE_START_LINES = new HashSet<>(Arrays.asList(".line", ".local", ".param", "#"));

    private String text;
    private String whitespace; // how much whitespace is at the beg of text

    private String[] parts;

    // should this line be ignored? lines like .local and .line should be (makes it harder to understand)
    private boolean ignore = false;

    private final SmaliFile parentFile;
    private SmaliMethod parentMethod = null; // is this line in a method?

    // what files this line points to
    private final ArrayList<SmaliFile> referenceSmaliFileList = new ArrayList<>();
    private final HashMap<String, SmaliFile> referenceSmaliFileMap = new HashMap<>();

    public SmaliLine(String text, SmaliFile parentFile) {
        this.parentFile = parentFile;
        setText(text);
    }

    public void setText(String text) {
        this.text = text;
        this.whitespace = StringUtils.getLeadingWhitespace(text);

        String trimmed = text.trim();
        ArrayList<String> partList = new ArrayList<>();
        if (trimmed.startsWith("const-string")) {
            // break into pieces, but everything in "" stays together

            // const-string v2, "T?"
            partList.add("const-string");
            int commaIndex = trimmed.indexOf(",", "const-string".length());
            partList.add(trimmed.substring("const-string".length() + 1, commaIndex + 1));
            int firstQ = trimmed.indexOf("\"", commaIndex + 1);
            int lastQ = trimmed.lastIndexOf("\"");
            partList.add(trimmed.substring(firstQ + 1, lastQ));
            parts = partList.toArray(new String[0]);
        } else {
            // nothing special? IDK, just split by spaces
            parts = trimmed.split("\\s+");
        }
        if (parts.length > 0) {
            this.ignore = SmaliLine.IGNORE_START_LINES.contains(parts[0]);
        }
    }

    // check if this line connects/refers to other lines, and if its a special line (method, field, etc)
    public static SmaliLine process(String text, SmaliFile inFile) {
        SmaliLine smaliLine = new SmaliLine(text, inFile);

        // check if lines reference any class within the main package
        HashMap<String, SmaliFile> smaliFileMap = APKInfo.getInstance().getSmaliFileMap();

        File smaliDir = APKInfo.getInstance().getSmaliDir();

        // check if this lines references fields or methods
        int arrowIndex = smaliLine.getText().indexOf("->");

        // sget-object v2, Lcom/naman14/timber/helpers/MusicPlaybackTrack;->CREATOR:Landroid/os/Parcelable$Creator;
        for (Substring ss : StringUtils.getSmaliClassSubstrings(text)) {
            // Lcom/naman14/timber/helpers/MusicPlaybackTrack;
            // remove L and ;
            String subpath = ss.getText().substring(1, ss.getText().length() - 1);
            File referencedFile = new File(smaliDir, subpath + ".smali");

            if (smaliFileMap.containsKey(referencedFile.getAbsolutePath())) {
                // referenced class is in main package (I don't want to obfuscate ALL files including libs)
                SmaliFile referenced = smaliFileMap.get(referencedFile.getAbsolutePath());
                referenced.addReferenceSmaliLine(smaliLine);
                smaliLine.addReferenceSmaliFile(referenced);

                // // CREATOR:Landroid/os/Parcelable$Creator;
                if (ss.getEndIndex() == arrowIndex) {
                    // REFERENCE TO METHOD OR FIELD!!
                    String referenceTo = smaliLine.getText().substring(arrowIndex + 2);
                    HashMap<String, ArrayList<SmaliLine>> storedRef;

                    if (referenceTo.contains("(") && referenceTo.contains(")")) {
                        // method
                        storedRef = referenced.getMethodReferences();
                    } else {
                        // field
                        storedRef = referenced.getFieldReferences();
                    }

                    if (!storedRef.containsKey(referenceTo)) {
                        storedRef.put(referenceTo, new ArrayList<>());
                    }
                    storedRef.get(referenceTo).add(smaliLine);
                }
            }
        }


        // check if this line is part of a block (method, annotation, etc)
        inFile.addMethodLine(smaliLine);

        // check if this line is a field
        if (smaliLine.getParts()[0].equals(".field")) {
            inFile.addFieldLine(smaliLine);
        }

        return smaliLine;
    }

    public String getText() {
        return text;
    }

    public String getWhitespace() {
        return whitespace;
    }

    public String[] getParts() {
        return parts;
    }

    public SmaliFile getParentFile() {
        return parentFile;
    }

    public void setParentMethod(SmaliMethod parentMethod) {
        this.parentMethod = parentMethod;
    }

    public SmaliMethod getParentMethod() {
        return parentMethod;
    }

    public ArrayList<SmaliFile> getReferenceSmaliFileList() {
        return referenceSmaliFileList;
    }

    public HashMap<String, SmaliFile> getReferenceSmaliFileMap() {
        return referenceSmaliFileMap;
    }

    public void addReferenceSmaliFile(SmaliFile smaliFile) {
        referenceSmaliFileList.add(smaliFile);
        referenceSmaliFileMap.put(smaliFile.getSmaliPackage(), smaliFile);
    }

    public boolean isEmpty() {
        return text.trim().isEmpty();
    }

    // run to the end of inSmaliLine chain and join both chains
    public SmaliLine insertAfterLine(SmaliLine inSmaliLine) {
        SmaliLine rightOfIn = inSmaliLine;
        while (rightOfIn.nextSmaliLine != null) {
            rightOfIn = rightOfIn.nextSmaliLine;
        }

        SmaliLine rightOg = this.nextSmaliLine;

        this.nextSmaliLine = inSmaliLine;
        inSmaliLine.prevSmaliLine = this;

        if (rightOg != null) {
            rightOg.prevSmaliLine = rightOfIn;
            rightOfIn.nextSmaliLine = rightOg;
        }

        return rightOfIn;
    }

    public SmaliLine getNextSmaliLine() {
        return nextSmaliLine;
    }

    public SmaliLine getPrevSmaliLine() {
        return prevSmaliLine;
    }

    @Override
    public String toString() {
        return "SmaliLine{" +
                "originalText='" + text + '\'' +
                ", parts=" + Arrays.toString(parts) +
                ", ignore=" + ignore +
                ", parentFile=" + parentFile +
                '}';
    }
}
