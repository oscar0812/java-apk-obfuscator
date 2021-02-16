package com.oscar0812.obfuscation.res;

import com.oscar0812.obfuscation.APKInfo;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

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

    // group directories into groups: anim, anim-v21 go into 1 group since they start with anim
    // drawable -> [drawable, drawable-anydpi-v21, drawable-ldrtl-mdpi-v17, ...]
    private final HashMap<String, ArrayList<String>> dirGroupMap = new HashMap<>();

    private final HashMap<String, XMLFile> XMLFileMap = new HashMap<>();
    private final HashMap<String, ImageFile> imageFileMap = new HashMap<>();

    private ResourceInfo() {
        resDir = APKInfo.getInstance().getResDir();
        parsePublicXML();
        fetchResFiles();
    }

    public static Document readXMLFile(File XMLFile) {
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
        Document doc = readXMLFile(publicXML);
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

        ArrayList<String> dirNames = new ArrayList<>();

        while (!q.isEmpty()) {
            File parent = q.poll(); // retrieve and remove the first element
            File[] files = parent.listFiles();

            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".xml")) {
                        XMLFileMap.put(file.getAbsolutePath(), new XMLFile(file.getAbsolutePath()));
                    } else if (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")) {
                        imageFileMap.put(file.getAbsolutePath(), new ImageFile(file.getAbsolutePath()));
                    } else {
                        System.out.println("UNKNOWN RES EXTENSION: " + file.getAbsolutePath());
                    }
                } else if (file.isDirectory()) {
                    // found directory
                    q.add(file);
                    String fileName = file.getName();
                    if (!fileName.contains("-")) {
                        fileName += "-";
                    }
                    dirNames.add(fileName);
                }
            }
        }

        Collections.sort(dirNames);
        String parent = null;
        String keyOfParent = null;

        for (String dirName : dirNames) {
            if (parent == null || !dirName.startsWith(parent)) {
                parent = dirName;
                if (parent.indexOf('-') < parent.length() - 1) {
                    // has more than 1 -
                    parent = parent.substring(0, parent.indexOf("-") + 1);
                }
                keyOfParent = parent.substring(0, parent.length() - 1);

            }

            if (!dirGroupMap.containsKey(keyOfParent)) {
                dirGroupMap.put(keyOfParent, new ArrayList<>());
            }

            if (dirName.endsWith("-")) {
                dirName = keyOfParent;
            }
            dirGroupMap.get(keyOfParent).add(dirName);
        }
    }

    public File getResDir() {
        return resDir;
    }

    public File getPublicXML() {
        return publicXML;
    }

    public HashMap<String, ArrayList<Element>> getPublicXMLTypeElementListMap() {
        return publicXMLTypeElementListMap;
    }

    public HashMap<String, Element> getPublicXMLAtSymbolElementListMap() {
        return publicXMLAtSymbolElementListMap;
    }

    public HashMap<String, Element> getPublicXMLIDElementMap() {
        return publicXMLIDElementMap;
    }

    public HashMap<String, XMLFile> getXMLFileMap() {
        return XMLFileMap;
    }
}
