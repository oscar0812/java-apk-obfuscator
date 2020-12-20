package com.oscar0812.obfuscation;

import java.util.ArrayList;
import java.util.Arrays;
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
    public static ArrayList<String> getSmaliClassSubstrings(String input) {
        ArrayList<String> list = new ArrayList<>();

        if (!input.contains("L") || !input.contains(";")) {
            // avoid headache's, there's nothing here
            return list;
        }

        int from = 0;
        int indL = input.indexOf('L', from);
        while (indL >= 0) {
            if (indL == 0 || !Character.isLetterOrDigit(input.charAt(indL - 1))) {
                // L can't have a alphanum character before it
                // + 1 to include ;
                int indC = input.indexOf(';', indL + 1);
                if (indC > 0) {
                    list.add(input.substring(indL, indC + 1));
                }
            }
            from = indL + 1;
            indL = input.indexOf('L', from);
        }

        return list;
    }
}
