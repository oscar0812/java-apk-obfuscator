package com.oscar0812.obfuscation.smali;

import java.util.ArrayList;

public class SmaliMethod {

    private final SmaliFile parentFile;
    private final ArrayList<SmaliLine> childLines;

    private boolean isConstructor;
    private String accessSpecifier;
    private String methodName;
    private String[] methodParameters; // how to hold parameters? just like this for now
    private String methodReturnType;

    private boolean ended; // reached ".end method" line

    // parentFile is the file which this method resides: onCreate()'s parentFile is MainActivity.smali
    // firstLine: .method protected onCreate(Landroid/os/Bundle;)V
    public SmaliMethod(SmaliFile parentFile, SmaliLine firstLine) {
        this.parentFile = parentFile;
        childLines = new ArrayList<>();
        childLines.add(firstLine);

        String[] parts = firstLine.getParts();
        if (!parts[0].equals(".method")) {
            System.out.println("Not a method start: " + firstLine.getOriginalText());
            return;
        }

        this.accessSpecifier = parts[1];
        this.isConstructor = parts[2].equals("constructor");

        // onCreateView(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;)Landroid/view/View;
        String lastPart = parts[parts.length - 1];

        this.methodName = lastPart.substring(0, lastPart.indexOf("("));

        // get string in (...)
        int firstBracket = lastPart.indexOf('(');
        // Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;
        String contents = lastPart.substring(firstBracket + 1, lastPart.indexOf(')', firstBracket));
        // ["Landroid/view/LayoutInflater", "Landroid/view/ViewGroup", "Landroid/os/Bundle"]
        this.methodParameters = contents.split(";");

        // the return is after the )
        // Landroid/view/View
        this.methodReturnType = lastPart.substring(lastPart.indexOf(")") + 1, lastPart.length() - 1);
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

    public SmaliFile getParentFile() {
        return parentFile;
    }

    public ArrayList<SmaliLine> getChildLines() {
        return childLines;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public String getAccessSpecifier() {
        return accessSpecifier;
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
}
