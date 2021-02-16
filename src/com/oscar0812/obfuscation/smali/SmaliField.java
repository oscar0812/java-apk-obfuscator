package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.APKInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/***
 #static fields
 .field <access rights> static [modification keyword]<field name>:<field type> [= value]

 #instance fields
 .field <access rights> [modification keyword]<field name>:<field type> [= value]
 */

public class SmaliField implements SmaliBlock{
    private SmaliLine smaliLine;

    private String identifier; // field name
    private String fieldType;
    private int indexOfField = -1;  // <field name>:<field type>
    private String value = null;

    public SmaliField(SmaliLine smaliLine) {
        setSmaliLine(smaliLine);
    }

    public void setSmaliLine(SmaliLine smaliLine) {
        this.smaliLine = smaliLine;
        String[] parts = smaliLine.getParts();

        for (int x = 0; x < parts.length; x++) {
            if (parts[x].contains(":")) {
                indexOfField = x;
                break;
            }
        }

        assert indexOfField >= 0;
        int cIndex = parts[indexOfField].indexOf(":");
        identifier = parts[indexOfField].substring(0, cIndex);
        fieldType = parts[indexOfField].substring(cIndex + 1);

        if (indexOfField + 1 < parts.length) {
            // has a value
            this.value = parts[parts.length - 1];
        }
    }

    @Override
    public SmaliFile getParentFile() {
        return this.getSmaliLine().getParentFile();
    }

    @Override
    public Set<String> getMapKeys(SmaliFile smaliFile) {
        return smaliFile.getFieldMap().keySet();
    }

    @Override
    public HashMap<String, String> parentNameChanges() {
        HashMap<String, String> changes = new HashMap<>();
        for(SmaliFile parentFile: this.getParentFile().getParentFileMap().values()) {
            changes.putAll(parentFile.getFieldNameChange());
        }
        return changes;
    }

    // return the new identifier
    @Override
    public void rename() {
        // 1. get new field name
        String[] parts = this.getSmaliLine().getParts();
        StringBuilder builder = new StringBuilder(this.getSmaliLine().getWhitespace());

        for (int x = 0; x < this.getIndexOfField(); x++) {
            builder.append(parts[x]);
            builder.append(" ");
        }

        String oldFieldName = this.getIdentifier();
        String newFieldName = getAvailableID();

        builder.append(newFieldName).append(":").append(this.getFieldType());

        if (getIndexOfField() + 1 < parts.length) {
            builder.append(" ");
        }

        for (int x = getIndexOfField() + 1; x < parts.length; x++) {
            builder.append(parts[x]);
            builder.append(" ");
        }

        this.getSmaliLine().getParentFile().getFieldNameChange().put(oldFieldName, newFieldName);

        // 2. make a new field line
        SmaliLine newFieldLine = new SmaliLine(builder.toString().stripTrailing(), this.getSmaliLine().getParentFile());
        this.getSmaliLine().getPrevSmaliLine().insertAfter(newFieldLine);
        this.getSmaliLine().delete();
        this.setSmaliLine(newFieldLine);

        // 3. unlink method name from parent file
        HashMap<String, SmaliField> map = this.getSmaliLine().getParentFile().getFieldMap();
        map.put(newFieldName, map.remove(oldFieldName));

        // 4. change all lines that called this method by the old name
        ArrayList<SmaliLine> smaliLinesPointingToThisField = this.getSmaliLine().getParentFile().getFieldReferences().get(oldFieldName);

        if (smaliLinesPointingToThisField == null) {
            smaliLinesPointingToThisField = new ArrayList<>();
        }

        // child files may refer to parent file fields
        for (SmaliFile childFile : this.getSmaliLine().getParentFile().getChildFileMap().values()) {
            if (childFile.getFieldReferences().containsKey(oldFieldName)) {
                smaliLinesPointingToThisField.addAll(childFile.getFieldReferences().get(oldFieldName));
            }
        }

        for (SmaliLine smaliLine : smaliLinesPointingToThisField) {
            String replaceThis = "->" + oldFieldName + ":" + this.getFieldType();
            String newText = "->" + newFieldName + ":" + this.getFieldType();
            String text = smaliLine.getText();
            smaliLine.setText(text.replace(replaceThis, newText));
        }
    }

    public boolean canRename() {
        // if its in an R.smali file dont rename
        return !APKInfo.getInstance().getRFileMap().containsKey(this.getSmaliLine().getParentFile().getAbsolutePath());
    }

    public SmaliLine getSmaliLine() {
        return smaliLine;
    }

    public String getFullField() {
        return smaliLine.getParts()[indexOfField];
    }

    public int getIndexOfField() {
        return indexOfField;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public String getFieldType() {
        return fieldType;
    }

    public String getValue() {
        return value;
    }
}
