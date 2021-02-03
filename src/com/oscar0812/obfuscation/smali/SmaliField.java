package com.oscar0812.obfuscation.smali;

/***
 #static fields
 .field <access rights> static [modification keyword]<field name>:<field type> [= value]

 #instance fields
 .field <access rights> [modification keyword]<field name>:<field type> [= value]
 */

public class SmaliField {
    private final SmaliLine smaliLine;

    private final String fieldName;
    private final String fieldType;
    private int indexOfField = -1;  // <field name>:<field type>
    private String value = null;

    public SmaliField(SmaliLine smaliLine) {
        this.smaliLine = smaliLine;
        String[] parts = smaliLine.getParts();

        for (int x = 0; x < parts.length; x++) {
            if (parts[x].contains(":")) {
                indexOfField = x;
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
