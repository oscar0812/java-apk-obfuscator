package com.oscar0812.obfuscation.res;

import com.oscar0812.obfuscation.APKInfo;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class ResourceInfo {
    private static ResourceInfo instance = null;

    public static ResourceInfo getInstance() {
        if (instance == null) {
            instance = new ResourceInfo();
        }

        return instance;
    }

    private File resDir, publicXML;

    /**
     * HASHMAP that groups the xml lines in public.xml according to their type
     * i.e. all type "anim" get appended into a list. {"anim": list[element]}
     */
    private final HashMap<String, ArrayList<Element>> publicXMLTypeElementListMap = new HashMap<>();

    /**
     * HASHMAP that connects a @type/name to a list of elements
     * i.e. {"@anim/abc_fade_in": [<public type="anim" name="abc_fade_in" id="0x7f010000" />]}
     */
    private final HashMap<String, Element> publicXMLAtSymbolElementListMap = new HashMap<>();

    /**
     * HASHMAP that connects an id to an element.
     * i.e. {"0x7f010000": <public type="anim" name="abc_fade_in" id="0x7f010000" />}
     */
    private final HashMap<String, Element> publicXMLIDElementMap = new HashMap<>();

    private ResourceInfo() {
        resDir = APKInfo.getInstance().getResDir();
        parsePublicXML();
    }

    private Document getXMLDocument(File XMLFile) {
        try {
            return new SAXReader().read(XMLFile);
        } catch (DocumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    // public.xml is the LINK between code and resources
    // <public type="anim" name="abc_fade_in" id="0x7f010000" />
    // make structures: type=>[line, line, line], name=>line, id=>name
    private void parsePublicXML() {
        publicXML = new File(resDir, "values" + File.separator + "public.xml");
        Document doc = getXMLDocument(publicXML);
        assert doc != null;
        Element parentTag = doc.getRootElement();
        for (Element element : parentTag.elements()) {
            String type = element.attributeValue("type");
            String name = element.attributeValue("name");
            String id = element.attributeValue("id");

            if (!publicXMLTypeElementListMap.containsKey(type)) {
                publicXMLTypeElementListMap.put(type, new ArrayList<>());
            }

            publicXMLAtSymbolElementListMap.put("@" + type + "/" + name, element);
            publicXMLTypeElementListMap.get(type).add(element);

            publicXMLIDElementMap.put(id, element);
        }
    }

    private void fetchResFiles() {
        Queue<File> q = new LinkedList<>();
        q.add(resDir);

        while (!q.isEmpty()) {
            File parent = q.poll(); // retrieve and remove the first element
            File[] files = parent.listFiles();

            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isFile()) {
                    System.out.println(file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    // found directory
                    q.add(file);
                }
            }
        }
    }
}
