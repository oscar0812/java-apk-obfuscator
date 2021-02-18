package com.oscar0812.obfuscation.res;

import com.oscar0812.obfuscation.APKInfo;
import com.oscar0812.obfuscation.utils.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

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

    private final HashMap<String, ArrayList<File>> nameNoExtensionToFile = new HashMap<>();

    public HashMap<String, String> XMLNameAttrChangeMap = new HashMap<>();
    public HashMap<String, ArrayList<File>> valueXMLFiles = new HashMap<>();

    private final HashMap<String, XMLFile> allXMLFiles = new HashMap<>();

    // Map: renameThis->toThis
    private final Set<String> renameTheseFiles = new HashSet<>();
    private final HashMap<String, String> renameFilesMap = new HashMap<>();

    ArrayList<String> permutations = StringUtils.getStringPermutations();
    int permIndex = 0;

    Set<String> ignoredAttributes = new HashSet<>(Arrays.asList("buttonGravity", "nanananananan"));

    Map<String, String> typeToFileName = Map.ofEntries(
            new AbstractMap.SimpleEntry<>("id", "ids")
    );

    private ResourceInfo() {
        fetchFiles();
        parseValuesDir();
        int aa = 1;
    }

    private void parseValuesDir() {
        // we need a copy, don't want to mess around with og hashmap
        HashMap<String, ArrayList<File>> nameNoExtensionToFileCopy = new HashMap<>(nameNoExtensionToFile);

        // first parse public.xml as it contains all elements
        XMLFile publicXMLFile = new XMLFile(APKInfo.getInstance().getResDir(), "values" + File.separator + "public.xml");
        valueXMLFiles.remove("public");

        Document document = parseValueXML(publicXMLFile, nameNoExtensionToFileCopy);
        XMLFile.saveXMLFile(publicXMLFile, document);

        for (File valueXMLFile : valueXMLFiles.get(typeToFileName.get("id"))) {
            document = parseValueXML(valueXMLFile, nameNoExtensionToFileCopy);
            XMLFile.saveXMLFile(valueXMLFile, document);
        }

        // now add files to rename map
        for (String fileName : renameTheseFiles) {
            addToRenameMap(fileName, nameNoExtensionToFileCopy);
        }
    }

    private Document parseValueXML(File valueXMLFile, HashMap<String, ArrayList<File>> nameNoExtensionToFileCopy) {
        Document doc = readXMLFile(valueXMLFile);
        assert doc != null;

        Queue<Element> q = new LinkedList<>();
        q.add(doc.getRootElement());

        while (!q.isEmpty()) {
            Element qElement = q.poll();
            String name = qElement.attributeValue("name");
            String type = qElement.attributeValue("type");

            if (name != null && !name.isEmpty() && !name.startsWith("android:")) {
                if (nameNoExtensionToFileCopy.containsKey(name)) {
                    // We are going to rename files, make sure that the new file name is not taken, otherwise we will override files
                    // renameTheseFiles.add(name);
                    continue;
                }
                // LETS WORRY ABOUT ID first
                if (type != null && type.equals("id")) {
                    if (!XMLNameAttrChangeMap.containsKey(name)) {
                        String newName = "r" + permutations.get(permIndex++);
                        if (name.contains(".")) {
                            newName = name.substring(0, name.lastIndexOf(".") + 1) + newName;
                        }
                        XMLNameAttrChangeMap.put(name, newName);
                    }

                    if (XMLNameAttrChangeMap.containsKey(name)) {
                        qElement.addAttribute("name", XMLNameAttrChangeMap.get(name));
                    }
                }
            }
            q.addAll(qElement.elements());
        }

        return doc;
    }

    private void addToRenameMap(String name, HashMap<String, ArrayList<File>> nameNoExtensionToFileCopy) {
        HashMap<String, File> allChildrenFiles = new HashMap<>();
        for (File renameChild : nameNoExtensionToFileCopy.get(name)) {
            File[] siblingList = renameChild.getParentFile().listFiles();
            if (siblingList != null) {
                for (File sibling : siblingList) {
                    String pathNoExt = sibling.getAbsolutePath();
                    pathNoExt = pathNoExt.substring(0, pathNoExt.lastIndexOf("."));
                    allChildrenFiles.put(pathNoExt, sibling);
                }
            }
        }

        String newName = permutations.get(++permIndex);
        // loop until its a new file
        while (allChildrenFiles.containsKey(newName)) {
            newName = "r_" + permutations.get(permIndex++);
        }
        for (File renameThis : nameNoExtensionToFileCopy.get(name)) {
            String ext = renameThis.getName();
            int lastIndex = ext.lastIndexOf(".");
            ext = ext.substring(lastIndex);

            File renameToThis = new File(renameThis.getParentFile(), newName + ext);
            renameFilesMap.put(renameThis.getAbsolutePath(), renameToThis.getAbsolutePath());
        }

        nameNoExtensionToFileCopy.remove(name);
    }

    public static Document readXMLFile(File XMLFile) {
        try {
            return new SAXReader().read(XMLFile);
        } catch (DocumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    // go into res/ and fetch all files, keep a hashmap of files without extension since xml refers to some files that way
    private void fetchFiles() {
        Queue<File> q = new LinkedList<>();
        q.add(APKInfo.getInstance().getResDir());

        while (!q.isEmpty()) {
            File parent = q.poll(); // retrieve and remove the first element
            File[] files = parent.listFiles();

            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isFile()) {
                    String fileNameNoExt = file.getName();
                    // some files have more than 1 period!
                    fileNameNoExt = fileNameNoExt.substring(0, fileNameNoExt.indexOf(".")); // remove extension

                    if (!nameNoExtensionToFile.containsKey(fileNameNoExt)) {
                        nameNoExtensionToFile.put(fileNameNoExt, new ArrayList<>());
                    }
                    nameNoExtensionToFile.get(fileNameNoExt).add(file);

                    if (file.getName().endsWith(".xml")) {
                        // check if this is an important value xml file
                        String pName = file.getParentFile().getName();
                        if (pName.equals("values") || pName.startsWith("values-")) {
                            if (!valueXMLFiles.containsKey(fileNameNoExt)) {
                                valueXMLFiles.put(fileNameNoExt, new ArrayList<>());
                            }
                            valueXMLFiles.get(fileNameNoExt).add(new XMLFile(file.getAbsolutePath()));
                        }

                        allXMLFiles.put(file.getAbsolutePath(), new XMLFile(file.getAbsolutePath()));
                    }
                } else if (file.isDirectory()) {
                    // found directory
                    q.add(file);
                }
            }
        }
    }

    public HashMap<String, XMLFile> getAllXMLFiles() {
        return allXMLFiles;
    }

    public HashMap<String, ArrayList<File>> getNameNoExtensionToFile() {
        return nameNoExtensionToFile;
    }

    public HashMap<String, String> getXMLNameAttrChangeMap() {
        return XMLNameAttrChangeMap;
    }

    public HashMap<String, String> getRenameFilesMap() {
        return renameFilesMap;
    }
}
