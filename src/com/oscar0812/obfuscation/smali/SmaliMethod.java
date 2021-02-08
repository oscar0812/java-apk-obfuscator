package com.oscar0812.obfuscation.smali;

import java.util.Arrays;

public class SmaliMethod {
    private boolean ended;
    private final SmaliFile parentFile;

    private SmaliLine firstSmaliLine; // .method ...
    private SmaliLine lastSmaliLine = null; // .end method ...


    private boolean isConstructor = false;
    private String[] accessSpecifiers = null;
    private final String methodName;
    private final String[] methodParameters; // how to hold parameters? just like this for now
    private final String methodReturnType;

    // onCreate(Landroid/os/Bundle;)V
    private final String methodIdentifier;

    // parentFile is the file which this method resides: onCreate()'s parentFile is MainActivity.smali
    // firstLine: .method protected onCreate(Landroid/os/Bundle;)V
    public SmaliMethod(SmaliFile parentFile, SmaliLine firstLine) {
        this.parentFile = parentFile;
        this.firstSmaliLine = firstLine;

        String[] parts = firstLine.getParts();

        // "default" access modifier: .method mName()V
        if (parts.length > 2) {
            // ["public"], ["public", "static", "final"], ["private"], etc..
            this.accessSpecifiers = Arrays.copyOfRange(parts, 1, parts.length - 1);
            this.isConstructor = accessSpecifiers[accessSpecifiers.length - 1].equals("constructor");
        }

        // onCreate(Landroid/os/Bundle;)V
        this.methodIdentifier = parts[parts.length - 1];

        this.methodName = methodIdentifier.substring(0, methodIdentifier.indexOf("("));

        // get string in (...)
        int firstBracket = methodIdentifier.indexOf('(');
        // Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;
        String contents = methodIdentifier.substring(firstBracket + 1, methodIdentifier.indexOf(')', firstBracket));
        // ["Landroid/view/LayoutInflater", "Landroid/view/ViewGroup", "Landroid/os/Bundle"]
        this.methodParameters = contents.split(";");

        // the return is after the )
        // Landroid/view/View
        this.methodReturnType = methodIdentifier.substring(methodIdentifier.indexOf(")") + 1, methodIdentifier.length() - 1);
    }

    public void setLastLine(SmaliLine lastSmaliLine) {
        this.lastSmaliLine = lastSmaliLine;
        this.ended = true;

        updateChildSmaliLines();

    }

    // call this when child smali lines change (to set the parent method)
    public void updateChildSmaliLines() {
        SmaliLine runner = firstSmaliLine;
        while (runner != null && !runner.equals(lastSmaliLine)) {
            runner.setParentMethod(this);
            runner = runner.getNextSmaliLine();
        }
        if (runner != null) {
            runner.setParentMethod(this);
        }
    }

    public SmaliLine getFirstSmaliLine() {
        return firstSmaliLine;
    }

    public SmaliLine getLastSmaliLine() {
        return lastSmaliLine;
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

    public String getMethodIdentifier() {
        return methodIdentifier;
    }

    @Override
    public String toString() {
        return "SmaliMethod{" +
                "ended=" + ended +
                ", parentFile=" + parentFile +
                ", firstSmaliLine=" + firstSmaliLine +
                ", lastSmaliLine=" + lastSmaliLine +
                ", isConstructor=" + isConstructor +
                ", accessSpecifiers=" + Arrays.toString(accessSpecifiers) +
                ", methodName='" + methodName + '\'' +
                ", methodParameters=" + Arrays.toString(methodParameters) +
                ", methodReturnType='" + methodReturnType + '\'' +
                ", methodIdentifier='" + methodIdentifier + '\'' +
                '}';
    }
}
