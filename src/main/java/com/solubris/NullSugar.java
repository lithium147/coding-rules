package com.solubris;

public class NullSugar {
    @SafeVarargs
    public static <T> T coalesce(T... t) {
        for (T candidate : t) {
            if (candidate != null) return candidate;
        }

        return null;
    }
}
