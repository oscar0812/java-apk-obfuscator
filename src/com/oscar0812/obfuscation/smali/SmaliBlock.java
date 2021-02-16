package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public interface SmaliBlock {

    String getIdentifier();

    Set<String> getMapKeys(SmaliFile smaliFile);

    SmaliFile getParentFile();

    default String appendAfterName() {
        return "";
    }

    HashMap<String, String> parentNameChanges();

    default Set<String> getTakenIDs() {
        Set<String> takenIDs = new HashSet<>(getMapKeys(this.getParentFile()));
        for (SmaliFile parentSmaliFile : this.getParentFile().getParentFileMap().values()) {
            takenIDs.addAll(getMapKeys(parentSmaliFile));
        }
        return takenIDs;
    }

    default String getAvailableID() {
        Set<String> takenIDs = getTakenIDs();
        ArrayList<String> permutations = StringUtils.getStringPermutations();

        // parent might have already renamed
        HashMap<String, String> nameChanges = parentNameChanges();
        if(nameChanges.containsKey(getIdentifier())) {
            return nameChanges.get(getIdentifier());
        }

        for (String perm : permutations) {
            if (!takenIDs.contains(perm + appendAfterName())) {
                // new!
                return perm + appendAfterName();
            }
        }

        return "";
    }

    void rename();
}
