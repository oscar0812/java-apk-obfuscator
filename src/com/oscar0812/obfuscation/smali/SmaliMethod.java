package com.oscar0812.obfuscation.smali;

import java.util.ArrayList;
import java.util.Arrays;

public class SmaliMethod {
    private boolean ended;
    private final SmaliFile parentFile;
    private final ArrayList<SmaliLine> childLines;

    private boolean isConstructor = false;
    private String[] accessSpecifiers = null;
    private final String methodName;
    private final String[] methodParameters; // how to hold parameters? just like this for now
    private final String methodReturnType;

    // onCreate(Landroid/os/Bundle;)V
    private final String shortMethodIdentifier;

    // parentFile is the file which this method resides: onCreate()'s parentFile is MainActivity.smali
    // firstLine: .method protected onCreate(Landroid/os/Bundle;)V
    public SmaliMethod(SmaliFile parentFile, SmaliLine firstLine) {
        this.parentFile = parentFile;
        childLines = new ArrayList<>();
        this.childLines.add(firstLine);

        String[] parts = firstLine.getParts();

        // "default" access modifier: .method mName()V
        if(parts.length > 2) {
            // ["public"], ["public", "static", "final"], ["private"], etc..
            this.accessSpecifiers = Arrays.copyOfRange(parts, 1, parts.length-1);
            this.isConstructor = accessSpecifiers[accessSpecifiers.length-1].equals("constructor");
        }

        // onCreate(Landroid/os/Bundle;)V
        this.shortMethodIdentifier = parts[parts.length - 1];

        this.methodName = shortMethodIdentifier.substring(0, shortMethodIdentifier.indexOf("("));

        // get string in (...)
        int firstBracket = shortMethodIdentifier.indexOf('(');
        // Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;
        String contents = shortMethodIdentifier.substring(firstBracket + 1, shortMethodIdentifier.indexOf(')', firstBracket));
        // ["Landroid/view/LayoutInflater", "Landroid/view/ViewGroup", "Landroid/os/Bundle"]
        this.methodParameters = contents.split(";");

        // the return is after the )
        // Landroid/view/View
        this.methodReturnType = shortMethodIdentifier.substring(shortMethodIdentifier.indexOf(")") + 1, shortMethodIdentifier.length() - 1);
    }

    public void appendChildLine(SmaliLine smaliLine) {
        childLines.add(smaliLine);

        String[] ps = smaliLine.getParts();
        if (ps[0].equals(".end") && ps[1].equals("method")) {
            this.ended = true;
        } else if (ended) {
            System.out.println("You are appending to an already ended method! " + smaliLine.getOriginalText());
        }
    }

    public ArrayList<SmaliLine> getChildLines() {
        return childLines;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public String[] getAccessSpecifiers() {
        return accessSpecifiers;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getMethodParameters() {
        return methodParameters;
    }

    public String getMethodReturnType() {
        return methodReturnType;
    }

    public boolean isEnded() {
        return ended;
    }

    public SmaliFile getParentFile() {
        return parentFile;
    }

    public String getShortMethodIdentifier() {
        return shortMethodIdentifier;
    }
}
