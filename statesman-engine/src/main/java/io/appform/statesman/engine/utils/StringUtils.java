package io.appform.statesman.engine.utils;

import com.github.jknack.handlebars.internal.text.WordUtils;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {

    public static String alphaNumeric(String s) {
        if(Strings.isNullOrEmpty(s)) {
            return "";
        }
        return s.replaceAll("[^ \\s\\w]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }    

    public static String normalize(String s) {
        if(Strings.isNullOrEmpty(s)) {
            return "";
        }
        return s.toLowerCase()
                .replaceAll("[^ \\s\\w]+", " ")
                .replaceAll("[_\\s]+", "_");
    }

    public static String normalizeInitCap(String s) {
        if(Strings.isNullOrEmpty(s)) {
            return "";
        }
        return  WordUtils.capitalize(s);
    }

    public static String removeNewLine(String s) {
        if(Strings.isNullOrEmpty(s)) {
            return "";
        }
        return s.replaceAll("\\r\\n|\\r|\\n", " ");
    }
}
