package com.oscar0812.obfuscation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GlobalOptions {
    // ignore smali lines that start with .line, ...
    public static final Set<String> IGNORE_START_LINES = new HashSet<>(Arrays.asList(".line", ".local", ".param", "#"));
}
