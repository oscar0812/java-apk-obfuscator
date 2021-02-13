package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.APKInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/***
 #static fields
 .field <access rights> static [modification keyword]<field name>:<field type> [= value]

 #instance fields
 .field <access rights> [modification keyword]<field name>:<field type> [= value]
 */

public class SmaliField {
    private SmaliLine smaliLine;

    private String fieldName;
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
        fieldName = parts[indexOfField].substring(0, cIndex);
        fieldType = parts[indexOfField].substring(cIndex + 1);

        if (indexOfField + 1 < parts.length) {
            // has a value
            this.value = parts[parts.length - 1];
        }
    }

    private String getAvailableID() {
        Set<String> takenIDs = new HashSet<>(this.getSmaliLine().getParentFile().getFieldMap().keySet());
        for (SmaliFile parentSmaliFile : this.getSmaliLine().getParentFile().getParentFileMap().values()) {
            for(String fieldName: parentSmaliFile.getFieldMap().keySet()) {
                if(parentSmaliFile.getFieldNameChange().containsKey(this.getFieldName())) {
                    return parentSmaliFile.getFieldNameChange().get(this.getFieldName());
                }
                takenIDs.add(fieldName);
            }
        }

        String id;

        for (char x = 97; x <= 122; x++) {
            for (int y = 1; y < 32; y++) {
                id = ((x + "").repeat(y));
                if (!takenIDs.contains(id)) {
                    // new field!
                    return id;
                }
            }
        }

        return "";
    }

    // return the new identifier
    public void changeFieldName() {
        // 1. get new method name
        String[] parts = this.getSmaliLine().getParts();
        StringBuilder builder = new StringBuilder(this.getSmaliLine().getWhitespace());

        for (int x = 0; x < this.getIndexOfField(); x++) {
            builder.append(parts[x]);
            builder.append(" ");
        }

        String oldFieldName = this.getFieldName();
        String newFieldName = getAvailableID();

        builder.append(newFieldName).append(":").append(this.getFieldType());

        if(getIndexOfField() + 1 < parts.length) {
            builder.append(" ");
        }

        for(int x = getIndexOfField() + 1; x<parts.length; x++) {
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

        if(smaliLinesPointingToThisField == null) {
            smaliLinesPointingToThisField = new ArrayList<>();
        }

        // child files may refer to parent file fields
        for(SmaliFile childFile: this.getSmaliLine().getParentFile().getChildFileMap().values()) {
            if(childFile.getFieldReferences().containsKey(oldFieldName)) {
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

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public String getValue() {
        return value;
    }
}
