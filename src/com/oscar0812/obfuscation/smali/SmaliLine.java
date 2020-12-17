package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.StartProcess;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * const/4 is for 0-7 (fit in 8 bits)
 * const/16 is for 8-127 (fit in 16 bits)
 * const is for arbitrary 32-bit
 * <p>
 * Remove .line # lines for harder debugging
 */

public class SmaliLine {
    private final String originalText;
    private final String[] parts;

    private final SmaliFile inFile;

    public SmaliLine(String originalText, SmaliFile inFile) {
        this.originalText = originalText;

        assert inFile != null;
        this.inFile = inFile;

        String trimmed = originalText.trim();
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
    }

    public String getOriginalText() {
        return originalText;
    }

    public String[] getParts() {
        return parts;
    }

    public SmaliFile getInFile() {
        return inFile;
    }

    public boolean isEmpty() {
        return originalText.trim().isEmpty();
    }

    private static boolean ignoreLine(String text) {
        String trimmed = text.trim();
        boolean ignore = StartProcess.REMOVE_DOT_LINE && trimmed.startsWith(".line");

        return ignore;
    }

    // 1 line of text can become nothing (if ignored) or multiple lines (i.e, reflection makes multiple lines)
    public static ArrayList<SmaliLine> process(String text, SmaliFile inFile) {
        SmaliLine originalLine = new SmaliLine(text, inFile);
        ArrayList<SmaliLine> smaliLines = new ArrayList<>();
        // some lines should be ignored
        if (!ignoreLine(text)) {
            String[] parts = originalLine.getParts();
            if (parts.length > 0 && parts[0].equals("const-string")) {
                // ["const-string", "v0,", "String here"]
                // obfuscate string
                SmaliLineObfuscator obf = SmaliLineObfuscator.getInstance();
                smaliLines.addAll(obf.stringToStaticCall(originalLine));
            } else {
                smaliLines.add(originalLine);
            }
        }

        return smaliLines;
    }


}
