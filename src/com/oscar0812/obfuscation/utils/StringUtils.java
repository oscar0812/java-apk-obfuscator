package com.oscar0812.obfuscation.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class StringUtils {
    private static final Set<String> stringsUsed = new HashSet<>();

    public static String getRandomUniqueString() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder builder = new StringBuilder();
        int length = 2;
        int tries = 0;
        do {
            // collisions, make it longer
            if (tries > 0 && tries % 5 == 0) {
                length += 1;
            }
            tries++;

            // clear out string builder
            builder.setLength(0);
            // make a random string
            for (int x = 0; x < length; x++) {
                int randomNum = ThreadLocalRandom.current().nextInt(0, alphabet.length());
                builder.append(alphabet.charAt(randomNum));
            }
        } while (stringsUsed.contains(builder.toString()));

        stringsUsed.add(builder.toString());

        return builder.toString();
    }

    // value = Lcom/oscar0812/sample_navigation/MainActivity;->onCreate(Landroid/os/Bundle;)V
    // => ["Lcom/oscar0812/sample_navigation/MainActivity;", "Landroid/os/Bundle;"]
    public static ArrayList<Substring> getSmaliClassSubstrings(String input) {
        ArrayList<Substring> list = new ArrayList<>();

        if (!input.contains("L") || !input.contains(";")) {
            // avoid headache's, there's nothing here
            return list;
        }

        int from = 0;
        int indL = input.indexOf('L', from);
        while (indL >= 0) {
            // + 1 to include ;
            int indC = input.indexOf(';', indL + 1);
            if (indC > 0) {
                list.add(new Substring(indL, indC+1, input.substring(indL, indC + 1)));
            }

            from = indL + 1;
            indL = input.indexOf('L', from);
        }

        return list;
    }

    public static String getLeadingWhitespace(String text) {
        StringBuilder wsb = new StringBuilder();
        for (char c: text.toCharArray()) {
            if(Character.isWhitespace(c)) {
                wsb.append(c);
            }
        }
        return wsb.toString();
    }

    public static ArrayList<Integer> getIndicesOf(String text, String c) {
        int index = text.indexOf(c);
        ArrayList<Integer> indices = new ArrayList<>();
        while (index>= 0) {
            indices.add(index);
            index = text.indexOf(c, index+1);
        }

        return indices;
    }
}
