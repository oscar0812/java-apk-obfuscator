package com.oscar0812.obfuscation.res;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class XMLFile extends File {
    private final HashMap<String, ArrayList<Element>> attributeToElementsMap = new HashMap<>();
    private final HashMap<String, ArrayList<Element>> textToElementsMap = new HashMap<>();

    public XMLFile(@NotNull String pathname) {
        super(pathname);
    }

    public void processLines() {
        Document document = ResourceInfo.readXMLFile(this.getAbsoluteFile());
        assert document != null;
        Element element = document.getRootElement();
        Queue<Element> q = new LinkedList<>();
        q.add(element);

        while(!q.isEmpty()) {
            Element qElement = q.poll();

            // TODO: this is android:id, but also id?
            List<Attribute> attrs = qElement.attributes();
            for(Attribute attr: attrs) {
                String attrValue = attr.getValue();
                if(attrValue.startsWith("@") || attrValue.startsWith("?")) {
                    if(!attributeToElementsMap.containsKey(attrValue)) {
                        attributeToElementsMap.put(attrValue, new ArrayList<>());
                    }
                    attributeToElementsMap.get(attrValue).add(element);
                } else if(attrValue.length() > 0) {
                    if(!Character.isLetterOrDigit(attrValue.charAt(0)) && !attrValue.startsWith("#") && attrValue.startsWith("-") && attrValue.startsWith("\\")) {
                        // doesn't start with alphanum, check
                        System.out.println("UNKNOWN ATTR: " + attrValue);
                    }
                }
            }

            // can reference in text, such as @id/something
            String text = qElement.getText();
            if(text.startsWith("@") || text.startsWith("?")) {
                if(!textToElementsMap.containsKey(text)) {
                    textToElementsMap.put(text, new ArrayList<>());
                }
                textToElementsMap.get(text).add(element);
            }

            q.addAll(qElement.elements());
        }

        HashMap<String, ArrayList<Element>> a=attributeToElementsMap;
        int aa = 1;
    }
}
