package com.oscar0812.obfuscation.smali;

import java.util.*;

public class SmaliMethod {
    private boolean ended;
    private final SmaliFile parentFile;

    private SmaliLine firstSmaliLine; // .method ...
    private SmaliLine lastSmaliLine = null; // .end method ...

    private final Set<String> accessSpecifiers = new HashSet<>();
    private String methodName;
    private String methodParameterStr;
    private String[] methodParameterArr; // how to hold parameters? just like this for now
    private String methodReturnType;

    // onCreate(Landroid/os/Bundle;)V
    private String methodIdentifier = null;

    String methodType = "direct"; // direct or virtual or idk what else

    // parentFile is the file which this method resides: onCreate()'s parentFile is MainActivity.smali
    // firstLine: .method protected onCreate(Landroid/os/Bundle;)V
    public SmaliMethod(SmaliFile parentFile, SmaliLine firstLine) {
        this.parentFile = parentFile;
        setFirstSmaliLine(firstLine);
    }

    public void setFirstSmaliLine(SmaliLine firstSmaliLine) {
        this.firstSmaliLine = firstSmaliLine;
        this.firstSmaliLine.setParentMethod(this);

        String[] parts = firstSmaliLine.getParts();
        this.accessSpecifiers.clear();

        // "default" access modifier: .method mName()V
        if (parts.length > 2) {
            // ["public"], ["public", "static", "final"], ["private"], etc..
            Collections.addAll(this.accessSpecifiers, Arrays.copyOfRange(parts, 1, parts.length - 1));
        }

        // onCreate(Landroid/os/Bundle;)V
        this.methodIdentifier = parts[parts.length - 1];

        // onCreate
        this.methodName = methodIdentifier.substring(0, methodIdentifier.indexOf("("));

        // get string in (...)
        int firstBracket = methodIdentifier.indexOf('(');
        // Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;
        this.methodParameterStr = methodIdentifier.substring(firstBracket + 1, methodIdentifier.indexOf(')', firstBracket));
        // ["Landroid/view/LayoutInflater", "Landroid/view/ViewGroup", "Landroid/os/Bundle"]
        this.methodParameterArr = this.methodParameterStr.split(";");

        // the return is after the )
        // Landroid/view/View
        this.methodReturnType = methodIdentifier.substring(methodIdentifier.indexOf(")") + 1);
    }

    private String getAvailableID() {
        Set<String> takenIDs = new HashSet<>(this.getParentFile().getMethodMap().keySet());
        for (SmaliFile smaliFile : this.getParentFile().getMarriedFileMap().values()) {
            takenIDs.addAll(smaliFile.getMethodMap().keySet());
        }

        String id;

        for (char x = 97; x <= 122; x++) {
            for (int y = 1; y < 32; y++) {
                id = (this.getMethodType() + (x + "").repeat(y)) + "(" + this.methodParameterStr + ")";
                if (!takenIDs.contains(id)) {
                    // new method!
                    return id + methodReturnType;
                }
            }
        }

        return "";
    }

    // return the new identifier
    public void changeMethodName() {
        ArrayList<SmaliLine> smaliLinesPointingToThisMethod = this.getParentFile().getMethodReferences().get(this.getMethodIdentifier());
        // 1. get new method name
        String[] parts = this.firstSmaliLine.getParts();
        StringBuilder builder = new StringBuilder(firstSmaliLine.getWhitespace());

        for (int x = 0; x < parts.length - 1; x++) {
            builder.append(parts[x]);
            builder.append(" ");
        }

        String oldMethodID = this.getMethodIdentifier();
        String oldMethodIDNoReturn = oldMethodID.substring(0, oldMethodID.indexOf(")") + 1);
        String newMethodID = getAvailableID();

        for (SmaliFile implementsParent : this.getParentFile().getParentFileMap().values()) {
            // has a parent (.implements)
            if (implementsParent.getMethodNameChange().containsKey(oldMethodIDNoReturn)) {
                newMethodID = implementsParent.getMethodNameChange().get(oldMethodIDNoReturn) + this.methodReturnType;
                break;
            }
        }

        String newMethodIDNoReturn = newMethodID.substring(0, newMethodID.indexOf(")") + 1);

        this.getParentFile().getMethodNameChange().put(oldMethodIDNoReturn, newMethodIDNoReturn);

        builder.append(newMethodID);

        // 2. make a new method start line
        SmaliLine newFirstLine = new SmaliLine(builder.toString(), this.getParentFile());
        this.firstSmaliLine.getPrevSmaliLine().insertAfter(newFirstLine);
        this.firstSmaliLine.delete();
        this.setFirstSmaliLine(newFirstLine);

        // 3. unlink method name from parent file
        HashMap<String, SmaliMethod> map = this.getParentFile().getMethodMap();
        map.put(newMethodIDNoReturn, map.remove(oldMethodIDNoReturn));

        // 4. change all lines that called this method by the old name
        for (SmaliLine smaliLine : smaliLinesPointingToThisMethod) {
            String replaceThis = this.getParentFile().getSmaliPackage() + "->" + oldMethodID;
            String newText = this.getParentFile().getSmaliPackage() + "->" + newMethodID;
            String text = smaliLine.getText();
            smaliLine.setText(text.replace(replaceThis, newText));
        }
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

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public String getMethodType() {
        return methodType;
    }

    public boolean isDirect() {
        return this.methodType.equals("direct");
    }

    public boolean isVirtual() {
        return this.methodType.equals("virtual");
    }

    public SmaliLine getFirstSmaliLine() {
        return firstSmaliLine;
    }

    public SmaliLine getLastSmaliLine() {
        return lastSmaliLine;
    }

    public boolean isConstructor() {
        return accessSpecifiers.contains("constructor");
    }

    public boolean isSynthetic() {
        return accessSpecifiers.contains("synthetic");
    }

    public boolean canRename() {
        // TODO: make renaming virtual methods possible:
        // renaming parent - child functions messes up stuff, but how do I check all parents???
        return !isConstructor() && !isSynthetic() && !isVirtual();
    }

    public Set<String> getAccessSpecifiers() {
        return accessSpecifiers;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getMethodParameterArr() {
        return methodParameterArr;
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
                ", accessSpecifiers=" + accessSpecifiers +
                ", methodName='" + methodName + '\'' +
                ", methodParameterStr='" + methodParameterStr + '\'' +
                ", methodParameterArr=" + Arrays.toString(methodParameterArr) +
                ", methodReturnType='" + methodReturnType + '\'' +
                ", methodIdentifier='" + methodIdentifier + '\'' +
                '}';
    }
}
