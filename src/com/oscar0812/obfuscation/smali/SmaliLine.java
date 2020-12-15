package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.StartProcess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

        private final Set<String> stringsUsed = new HashSet<>();
        // parent = StringUtil
        private final HashMap<String, SmaliFile> parentFiles = new HashMap<>();
        private final ArrayList<String> parentFileKeys = new ArrayList<>();

        // children = StringUtil$1, StringUtil$1, etc...
        private final HashMap<String, SmaliFile> childFiles = new HashMap<>();

        private String getRandomUniqueString() {
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
            } while (stringsUsed.contains(builder.toString()));

            stringsUsed.add(builder.toString());

            return builder.toString();
        }

        private SmaliFile createNewFile(SmaliFile siblingFile) {
            // create a file with a name that doesn't exist in the same directory as siblingFile
            SmaliFile file;
            String fileClassName;
            do {
                fileClassName = getRandomUniqueString();
                file = new SmaliFile(siblingFile.getParentFile(), fileClassName + ".smali");
            } while (file.exists());

            String sp = siblingFile.getSmaliPackage();           // Lcom/oscar0812/sample_navigation/BuildConfig;
            sp = sp.substring(0, sp.lastIndexOf("/") + 1);      // Lcom/oscar0812/sample_navigation/
            sp += fileClassName + ";";                            // Lcom/oscar0812/sample_navigation/okxd;

            // set the package and class
            file.setSmaliPackage(sp);
            return file;
        }

        // parent smali of string obfuscator object (look at StringUtil.smali)
        private SmaliFile createParentFile(SmaliLine line) {
            SmaliFile parentFile = createNewFile(line.getInFile());

            parentFile.appendString(
                    ".class public " + parentFile.getSmaliPackage() + "\n" +
                            ".super Ljava/lang/Object;\n" +
                            ".source \"" + parentFile.getSmaliClass() + ".java\"\n\n\n" +
                            "# direct methods\n" +
                            ".method public constructor <init>()V\n" +
                            "    .locals 0\n\n" +
                            "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n\n" +
                            "    return-void\n" +
                            ".end method\n\n");

            parentFileKeys.add(parentFile.getAbsolutePath());
            parentFiles.put(parentFile.getAbsolutePath(), parentFile);

            return parentFile;
        }

        // parent smali of string obfuscator object (look at StringUtil$1.smali)
        private SmaliFile createChildFile(SmaliFile parentFile, SmaliLine smaliLine, String methodName) {
            SmaliFile childFile = createNewFile(parentFile);

            childFile.appendString(".class final " + childFile.getSmaliPackage() + "\n" +
                    ".super Ljava/lang/Object;\n" +
                    ".source \"" + childFile.getSmaliClass() + ".java\"\n\n\n" +
                    "# annotations\n" +
                    ".annotation system Ldalvik/annotation/EnclosingMethod;\n" +
                    "    value = " + parentFile.getSmaliPackage() + "->" + methodName + "()Ljava/lang/String;\n" +
                    ".end annotation\n\n" +
                    ".annotation system Ldalvik/annotation/InnerClass;\n" +
                    "    accessFlags = 0x8\n" +
                    "    name = null\n" +
                    ".end annotation\n\n\n" +
                    "# instance fields\n" +
                    ".field t:I\n\n\n" +
                    "# direct methods\n" +
                    ".method constructor <init>()V\n" +
                    "    .locals 0\n\n" +
                    "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n\n" +
                    "    return-void\n" +
                    ".end method\n\n\n" +
                    "# virtual methods\n" +
                    ".method public toString()Ljava/lang/String;\n"+
                    "    .locals 4\n\n");

            return childFile;
        }

        // const-string v0, "Replace with your own action" =>
        // invoke-static {}, Lcom/oscar0812/sample_navigation/StringUtil;->a()Ljava/lang/String;
        //
        // move-result-object v0
        public ArrayList<SmaliLine> stringToStaticCall(SmaliLine line) {
            String register = line.getParts()[1].replace(",", ""); // v0, => v0
            String methodName = getRandomUniqueString();

            int randomNum = ThreadLocalRandom.current().nextInt(0, parentFileKeys.size() + 2);

            SmaliFile parentFile;
            if (randomNum >= parentFileKeys.size()) {
                parentFile = createParentFile(line);
            } else {
                parentFile = parentFiles.get(parentFileKeys.get(randomNum));
            }

            // TODO: LEFT WORK HERE!

            SmaliFile childFile = createChildFile(parentFile, line, methodName);

            // add method to parent
            parentFile.appendString(
                    ".method public static " + methodName + "()Ljava/lang/String;\n" +
                            "       .locals 1\n\n" +
                            "       new-instance v0, Lcom/oscar0812/sample_navigation/StringUtil$1;\n\n" +
                            "       invoke-direct {v0}, Lcom/oscar0812/sample_navigation/StringUtil$1;-><init>()V\n\n" +
                            "       invoke-virtual {v0}, Lcom/oscar0812/sample_navigation/StringUtil$1;->toString()Ljava/lang/String;\n\n" +
                            "       move-result-object v0\n\n" +
                            "       return-object v0\n" +
                            ".end method\n\n");

            parentFile.saveToDisk();

            ArrayList<SmaliLine> lines = new ArrayList<>();
            lines.add(new SmaliLine("       invoke-static {}, Lcom/oscar0812/sample_navigation/StringUtil;->" + methodName + "()Ljava/lang/String;", line.getInFile()));
            // lines.add(new SmaliLine(""));
            lines.add(new SmaliLine("       move-result-object " + register, line.getInFile()));

            return lines;
        }
    }
}
