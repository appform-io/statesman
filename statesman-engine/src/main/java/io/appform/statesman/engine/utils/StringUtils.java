package io.appform.statesman.engine.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {
    public static String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^ \\s\\w]+", " ")
                .replaceAll("[_\\s]+", "_");
    }
}
