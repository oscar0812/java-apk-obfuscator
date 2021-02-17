package com.oscar0812.obfuscation.res;

import com.oscar0812.obfuscation.APKInfo;
import org.dom4j.Document;
import org.dom4j.DocumentException;
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
    private final HashMap<String, XMLFile> XMLFileMap = new HashMap<>();
    private final HashMap<String, ImageFile> imageFileMap = new HashMap<>();

    private ResourceInfo() {
        fetchFiles();
    }

    public static Document readXMLFile(File XMLFile) {
        try {
            return new SAXReader().read(XMLFile);
        } catch (DocumentException e) {
            e.printStackTrace();
            return null;
        }
    }

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
                    String fileName = file.getName();
                    fileName = fileName.substring(0, fileName.lastIndexOf(".")); // remove extension

                    if(!nameNoExtensionToFile.containsKey(fileName)) {
                        nameNoExtensionToFile.put(fileName, new ArrayList<>());
                    }
                    nameNoExtensionToFile.get(fileName).add(file);

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
                }
            }
        }
    }

    public HashMap<String, XMLFile> getXMLFileMap() {
        return XMLFileMap;
    }

    public HashMap<String, ImageFile> getImageFileMap() {
        return imageFileMap;
    }

    public HashMap<String, ArrayList<File>> getNameNoExtensionToFile() {
        return nameNoExtensionToFile;
    }
}
