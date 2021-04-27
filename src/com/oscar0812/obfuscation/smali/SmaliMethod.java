package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.utils.StringUtils;

import java.util.*;

public class SmaliMethod implements SmaliBlock {
    private boolean ended;
    private final SmaliFile parentFile;

    private SmaliLine firstSmaliLine; // .method ...
    private SmaliLine lastSmaliLine = null; // .end method ...

    private String methodName;
    private String methodParameterStr;
    private String[] methodParameterArr; // how to hold parameters? just like this for now
    private String methodReturnType;

    // onCreate(Landroid/os/Bundle;)V
    private String identifier = null;

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

        // onCreate(Landroid/os/Bundle;)V
        String last = parts[parts.length - 1];
        this.identifier = last.substring(0, last.lastIndexOf(")") + 1);

        // onCreate
        this.methodName = last.substring(0, last.indexOf("("));

        // get string in (...)
        int firstBracket = last.indexOf('(');
        // Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;
        this.methodParameterStr = last.substring(firstBracket + 1, last.indexOf(')', firstBracket));
        // ["Landroid/view/LayoutInflater", "Landroid/view/ViewGroup", "Landroid/os/Bundle"]
        this.methodParameterArr = this.methodParameterStr.split(";");

        // the return is after the )
        // Landroid/view/View
        this.methodReturnType = last.substring(last.indexOf(")") + 1);
    }

    @Override
    public SmaliFile getParentFile() {
        return parentFile;
    }

    @Override
    public Set<String> getMapKeys(SmaliFile smaliFile) {
        return smaliFile.getMethodMap().keySet();
    }

    @Override
    public HashMap<String, String> parentNameChanges() {
        HashMap<String, String> changes = new HashMap<>();
        for (SmaliFile parentFile : this.getParentFile().getParentFileMap().values()) {
            changes.putAll(parentFile.getMethodNameChange());
        }
        return changes;
    }

    @Override
    public String appendAfterName() {
        return "(" + this.methodParameterStr + ")";
    }

    @Override
    public String getAvailableID() {
        Set<String> takenIDs = getTakenIDs();
        ArrayList<String> permutations = StringUtils.getStringPermutations();

        for (String perm : permutations) {
            if (!takenIDs.contains(perm + appendAfterName())) {
                // new!
                return perm + appendAfterName();
            }
        }

        return "";
    }

    @Override
    public void rename() {
        String oldMethodID = this.getIdentifier();
        String newMethodID;
        HashMap<String, String> nameChanges = parentNameChanges();
        if (nameChanges.containsKey(this.getIdentifier())) {
            // parent already renamed this
            newMethodID = nameChanges.get(this.getIdentifier());
        } else if (!this.canRename()) {
            // don't rename these
            return;
        } else {
            // 1. get new method name
            newMethodID = getAvailableID();
        }

        String[] parts = this.getFirstSmaliLine().getParts();
        StringBuilder builder = new StringBuilder(this.getFirstSmaliLine().getWhitespace());

        for (int x = 0; x < parts.length - 1; x++) {
            builder.append(parts[x]);
            builder.append(" ");
        }
        builder.append(newMethodID).append(this.getMethodReturnType());

        // 2. make a new method start line
        SmaliLine newFirstLine = new SmaliLine(builder.toString(), this.getParentFile());
        this.getFirstSmaliLine().getPrevSmaliLine().insertAfter(newFirstLine);
        // TODO: update this like smaliField
        // this.getFirstSmaliLine().delete();
        this.setFirstSmaliLine(newFirstLine);

        this.getParentFile().getMethodNameChange().put(oldMethodID, newMethodID);

        // 3. unlink method name from parent file
        HashMap<String, SmaliMethod> map = this.getParentFile().getMethodMap();
        map.put(newMethodID, map.remove(oldMethodID));

        relinkAfterRename(oldMethodID, newMethodID);
    }

    // re-link all the lines pointing to this method
    private void relinkAfterRename(String oldMethodID, String newMethodID) {
        HashMap<String, ArrayList<SmaliLine>> references = this.getParentFile().getMethodReferences();
        for (SmaliFile childFile : this.getParentFile().getChildFileMap().values()) {
            if(!childFile.getMethodMap().containsKey(oldMethodID) && childFile.getMethodReferences().containsKey(oldMethodID)) {
                // child has references to this method, but doesn't contain the method!
                if(!references.containsKey(oldMethodID)) {
                    references.put(oldMethodID, new ArrayList<>());
                }
                references.get(oldMethodID).addAll(childFile.getMethodReferences().get(oldMethodID));
            }
        }

        if (references.containsKey(oldMethodID)) {
            for (SmaliLine smaliLine : new ArrayList<>(references.get(oldMethodID))) {
                String replaceThis = "->" + oldMethodID + methodReturnType;
                String newText = "->" + newMethodID + methodReturnType;
                String text = smaliLine.getText();
                smaliLine.setText(text.replace(replaceThis, newText));
            }
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
        return this.getFirstSmaliLine().getPartsSet().contains("constructor");
    }

    public boolean isSynthetic() {
        return this.getFirstSmaliLine().getPartsSet().contains("synthetic");
    }

    public boolean isAbstract() {
        return this.getFirstSmaliLine().getPartsSet().contains("abstract") && this.getParentFile().isAbstract();
    }

    public boolean canRename() {
        // TODO: make renaming virtual methods possible:
        // renaming parent - child functions messes up stuff, but how do I check all parents???
        return (!isConstructor() && !isSynthetic() && !isVirtual()); //|| isAbstract();
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

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return "SmaliMethod{" +
                "ended=" + ended +
                ", parentFile=" + parentFile +
                ", firstSmaliLine=" + firstSmaliLine +
                ", lastSmaliLine=" + lastSmaliLine +
                ", methodName='" + methodName + '\'' +
                ", methodParameterStr='" + methodParameterStr + '\'' +
                ", methodParameterArr=" + Arrays.toString(methodParameterArr) +
                ", methodReturnType='" + methodReturnType + '\'' +
                ", methodIdentifier='" + identifier + '\'' +
                '}';
    }
}
