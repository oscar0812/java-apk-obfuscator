package com.oscar0812.obfuscation.smali;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SmaliLineObfuscator {
    // FUNCTIONS call parent->method()
    // parent calls child->toString()
    // SINCE the string obfuscation creates an object and messes around with bytes in it's toString() method

    private static SmaliLineObfuscator instance = null;

    public static SmaliLineObfuscator getInstance() {
        if (instance == null) {
            instance = new SmaliLineObfuscator();
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

    // smali of string obfuscator object (look at StringUtil.smali)
    private SmaliFile createObfFile(SmaliLine line) {
        SmaliFile obfFile = createNewFile(line.getInFile());

        obfFile.appendString(
                ".class public " + obfFile.getSmaliPackage() + "\n" +
                        ".super Ljava/lang/Object;\n" +
                        ".source \"" + obfFile.getSmaliClass() + ".java\"\n\n\n" +
                        "# direct methods\n" +
                        ".method public constructor <init>()V\n" +
                        "    .locals 0\n\n" +
                        "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n\n" +
                        "    return-void\n" +
                        ".end method\n\n");

        parentFileKeys.add(obfFile.getAbsolutePath());
        parentFiles.put(obfFile.getAbsolutePath(), obfFile);

        return obfFile;
    }


    private String getConstTypeForInt(int num) {
        if (num >= -8 && num <= 7) {
            return "const/4";
        } else if ((num >= -32768 && num <= -9) || (num >= 8 && num <= 32767)) {
            return "const/16";
        } else if ((num >= -1073741824 && num <= -65536) || (num >= 32768 && num <= 1073741824)) {
            // return "const/high16";
            return "const";
        } else if (num >= -65535 && num <= -32769) {
            return "const";
        }

        return "const";
    }

    // add a method to the obfuscator file (will return a string from bytes)
    private void appendMethod(SmaliFile obfFile, SmaliLine smaliLine, String methodName) {

        obfFile.appendString(
                ".method public static "+methodName+"()Ljava/lang/String;\n" +
                "    .locals 4\n\n");

        String ogString = smaliLine.getParts()[smaliLine.getParts().length - 1]; // the last index holds the string
        int byteSize = ogString.length();
        String constType = getConstTypeForInt(byteSize);
        String sizeHex = "0x" + Integer.toHexString(byteSize);

        obfFile.appendString("\t\t" + constType + " v0, " + sizeHex + "\n\n\t\tnew-array v0, v0, [B");

        Random r = new Random(System.currentTimeMillis());
        byte[] b = ogString.getBytes();

        for (int i = 0; i < byteSize; ++i) {
            int tr = r.nextInt();
            int f = r.nextInt(24) + 1;

            int a1, t;
            a1 = (0xff << f);

            t = (tr & ~a1) | (b[i] << f);

            String sign = t < 0 ? "-" : "";
            String tHex = sign + "0x" + Integer.toHexString(Math.abs(t));
            sign = f < 0 ? "-" : "";
            String fHex = sign + "0x" + Integer.toHexString(Math.abs(f));

            String indexHex = "0x" + Integer.toHexString(i);
            obfFile.appendString(
                    "\t\t.line " + (obfFile.debugLine++) + "\n" +
                            "\t\tconst v1, " + tHex + "\n\n" +
                            "\t\tushr-int/lit8 v2, v1, " + fHex +"\n\n" +
                            "\t\tint-to-byte v2, v2\n\n" +
                            "\t\t" + getConstTypeForInt(i) + " v3, " + indexHex + "\n\n" +
                            "\t\taput-byte v2, v0, v3\n\n");

        }

        obfFile.appendString("\t\t.line " + obfFile.debugLine + "\n" +
                "\t\tnew-instance v2, Ljava/lang/String;\n\n" +
                "\t\tinvoke-direct {v2, v0}, Ljava/lang/String;-><init>([B)V\n\n"+
                "\t\treturn-object v2\n" +
                ".end method");

        obfFile.debugLine+=10;
    }

    // const-string v0, "Replace with your own action" =>
    // invoke-static {}, Lcom/oscar0812/sample_navigation/StringUtil;->a()Ljava/lang/String;
    //
    // move-result-object v0
    public ArrayList<SmaliLine> stringToStaticCall(SmaliLine line) {
        ArrayList<SmaliLine> lines = new ArrayList<>();

        String register = line.getParts()[1].replace(",", ""); // v0, => v0
        String methodName = getRandomUniqueString();

        int randomNum = ThreadLocalRandom.current().nextInt(0, parentFileKeys.size() + 2);
        // int randomNum = 0;

        SmaliFile obfFile;
        if (randomNum >= parentFileKeys.size()) {
            obfFile = createObfFile(line);
        } else {
            obfFile = parentFiles.get(parentFileKeys.get(randomNum));
        }

        appendMethod(obfFile, line, methodName);

        obfFile.saveToDisk();


        lines.add(new SmaliLine("       invoke-static {}, " + obfFile.getSmaliPackage() + "->" + methodName + "()Ljava/lang/String;", line.getInFile()));
        // lines.add(new SmaliLine(""));
        lines.add(new SmaliLine("       move-result-object " + register, line.getInFile()));

        return lines;
    }
}