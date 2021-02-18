package com.oscar0812.obfuscation.res;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;

public class XMLFile extends File {

    // some attributes should not be changed
    Set<String> ignoredAttributes = new HashSet<>(Arrays.asList("format", "quantity", "type"));

    public XMLFile(String pathname) {
        super(pathname);
    }

    public XMLFile(File file, String subpath) {
        super(file, subpath);
    }

    public void processLines() {
        HashMap<String, String> publicXMLNameMap = ResourceInfo.getInstance().getXMLNameAttrChangeMap();

        Document document = ResourceInfo.readXMLFile(this.getAbsoluteFile());
        assert document != null;
        Element element = document.getRootElement();
        Queue<Element> q = new LinkedList<>();
        q.add(element);

        while (!q.isEmpty()) {
            Element qElement = q.poll();

            for (Attribute attr : qElement.attributes()) {
                if (ignoredAttributes.contains(attr.getName())) {
                    continue;
                }

                String attrValue = attr.getValue();
                int splitIndex = getSplitIndex(attrValue);

                if (splitIndex > 0) {
                    String checkThis = attrValue.substring(splitIndex);
                    if (publicXMLNameMap.containsKey(checkThis) && !publicXMLNameMap.get(checkThis).isEmpty()) {
                        // can rename this
                        String newAttrValue = attrValue.substring(0, splitIndex) + publicXMLNameMap.get(checkThis);
                        qElement.addAttribute(attr.getName(), newAttrValue);
                    }
                }
            }

            // can reference in text, such as @id/something
            int splitIndex = getSplitIndex(qElement.getText());
            if (splitIndex > 0) {
                String checkThis = qElement.getText().substring(splitIndex);
                if (publicXMLNameMap.containsKey(checkThis) && !publicXMLNameMap.get(checkThis).isEmpty()) {
                    String newText = qElement.getText().substring(0, splitIndex) + publicXMLNameMap.get(checkThis);
                    qElement.setText(newText);
                }
            }

            q.addAll(qElement.elements());
        }

        saveXMLFile(this, document);
    }

    public static void saveXMLFile(File file, Document document) {
        XMLWriter xmlWriter;
        try {
            xmlWriter = new XMLWriter(new FileOutputStream(file), OutputFormat.createPrettyPrint());
            xmlWriter.write(document);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getSplitIndex(String text) {
        if (text.equals("@null")) {
            return -1;
        }

        // if (text.startsWith("@") && !text.startsWith("@android:")) {
        if(text.startsWith("@id/")) {
            // @id/something
            return text.indexOf("/") + 1;
        } else if (text.startsWith("?") && !text.startsWith("?android:")) {
            // ?something
            // return 1;
            return -1;
        }
        else if (text.length() > 0) {
            if (!Character.isLetterOrDigit(text.charAt(0)) && !text.startsWith("#") && text.startsWith("-") && text.startsWith("\\")) {
                // doesn't start with alphanum, check
                System.out.println("UNKNOWN ATTR: " + text);
            }
        }

        return -1;
    }
}
