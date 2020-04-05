package io.appform.statesman.engine.utils;

import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {
    public static String normalize(String s) {
        if(Strings.isNullOrEmpty(s)) {
            return "";
        }
        return s.toLowerCase()
                .replaceAll("[^ \\s\\w]+", " ")
                .replaceAll("[_\\s]+", "_");
    }
}
