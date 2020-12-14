package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.StartProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
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

    public SmaliLine(String originalText) {
        this.originalText = originalText;
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

    public boolean isEmpty() {
        return originalText.trim().isEmpty();
    }

    private static boolean ignoreLine(String text) {
        String trimmed = text.trim();
        boolean ignore = StartProcess.REMOVE_DOT_LINE && trimmed.startsWith(".line");

        return ignore;
    }

    // 1 line of text can become nothing (if ignored) or multiple lines (i.e, reflection makes multiple lines)
    public static ArrayList<SmaliLine> process(String text) {
        SmaliLine originalLine = new SmaliLine(text);
        ArrayList<SmaliLine> smaliLines = new ArrayList<>();
        // some lines should be ignored
        if (!ignoreLine(text)) {
            String[] parts = originalLine.getParts();
            if (parts.length > 0 && parts[0].equals("const-string")) {
                // ["const-string", "v0,", "String here"]
                // obfuscate string
                Obfuscator obf = SmaliLine.Obfuscator.getInstance();
                smaliLines.addAll(obf.stringToStaticCall(originalLine));
            } else {
                smaliLines.add(originalLine);
            }
        }

        return smaliLines;
    }

    public static class Obfuscator {
        // FUNCTIONS call parent->method()
        // parent calls child->toString()
        // SINCE the string obfuscation creates an object and messes around with bytes in it's toString() method

        private static Obfuscator instance = null;

        public static Obfuscator getInstance() {
            if (instance == null) {
                instance = new Obfuscator();
            }
            return instance;
        }

        private SmaliFile parentFile;
        private SmaliFile childFile;

        private final Set<String> methodsUsed = new HashSet<>();

        public Obfuscator() {
            String SMALI_DIR = "./example_smali_files/";
            parentFile = new SmaliFile(SMALI_DIR + "StringUtil.smali");
            childFile = new SmaliFile(SMALI_DIR + "StringUtil$1.smali");

            parentFile.processLines();
            childFile.processLines();
        }

        private String getRandomMethodName() {
            String alphabet = "abcdefghijklmnopqrstuvwxyz";
            StringBuilder builder = new StringBuilder();
            int length = 2;
            int tries = 0;
            do {
                // collisions, make it longer
                if (tries > 0 && tries % 5 == 0) {
                    length += 1;
                }
                tries++;

                // clear out string builder
                builder.setLength(0);
                // make a random string
                for (int x = 0; x < length; x++) {
                    int randomNum = ThreadLocalRandom.current().nextInt(0, alphabet.length());
                    builder.append(alphabet.charAt(randomNum));
                }
            } while (methodsUsed.contains(builder.toString()));

            methodsUsed.add(builder.toString());

            return builder.toString();
        }

        // const-string v0, "Replace with your own action" =>
        // invoke-static {}, Lcom/oscar0812/sample_navigation/StringUtil;->a()Ljava/lang/String;
        //
        // move-result-object v0
        public ArrayList<SmaliLine> stringToStaticCall(SmaliLine line) {
            String register = line.getParts()[1].replace(",", ""); // v0, => v0
            String methodName = getRandomMethodName();

            ArrayList<SmaliLine> lines = new ArrayList<>();

            // add method to parent
            parentFile.appendString(
                    ".method public static " + methodName + "()Ljava/lang/String;\n\n" +
                    "       .locals 1\n\n" +
                    "       new-instance v0, Lcom/oscar0812/sample_navigation/StringUtil$1;\n\n" +
                    "       invoke-direct {v0}, Lcom/oscar0812/sample_navigation/StringUtil$1;-><init>()V\n\n" +
                    "       invoke-virtual {v0}, Lcom/oscar0812/sample_navigation/StringUtil$1;->toString()Ljava/lang/String;\n\n" +
                    "       move-result-object v0\n\n" +
                    "       return-object v0\n\n" +
                    ".end method\n");

            // TODO: dont hardcode path to stringUtil
            lines.add(new SmaliLine("       invoke-static {}, Lcom/oscar0812/sample_navigation/StringUtil;->" + methodName + "()Ljava/lang/String;"));
            // lines.add(new SmaliLine(""));
            lines.add(new SmaliLine("       move-result-object " + register));

            return lines;
        }
    }
}
