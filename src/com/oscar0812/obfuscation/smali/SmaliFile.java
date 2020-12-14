package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.StartProcess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class SmaliFile extends File {
    public final ArrayList<SmaliLine> lines = new ArrayList<>();

    public SmaliFile(String pathname) {
        super(pathname);
    }

    public void processLines() {
        System.out.println("PROCESSING: "+getAbsolutePath());
        // read file line by line
        try (Scanner scanner = new Scanner(new File(getAbsolutePath()))) {
            while (scanner.hasNext()){
                // check if line is valid
                String line = scanner.nextLine();
                ArrayList<SmaliLine> smaliLines = SmaliLine.process(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        // saveText();

        System.out.println("PROCESSED: "+getAbsolutePath());
    }

}
