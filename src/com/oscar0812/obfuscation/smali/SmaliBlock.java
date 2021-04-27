package com.oscar0812.obfuscation.smali;

import com.oscar0812.obfuscation.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public interface SmaliBlock {

    String getIdentifier();

    Set<String> getMapKeys(SmaliFile smaliFile);

    SmaliFile getParentSmaliFile();

    default String appendAfterName() {
        return "";
    }

    HashMap<String, String> parentNameChanges();

    default Set<String> getTakenIDs() {
        Set<String> takenIDs = new HashSet<>(getMapKeys(this.getParentSmaliFile()));
        for (SmaliFile parentSmaliFile : this.getParentSmaliFile().getParentFileMap().values()) {
            takenIDs.addAll(getMapKeys(parentSmaliFile));
        }
        return takenIDs;
    }

    default String getAvailableID() {
        Set<String> takenIDs = getTakenIDs();
        ArrayList<String> permutations = StringUtils.getStringPermutations();

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
