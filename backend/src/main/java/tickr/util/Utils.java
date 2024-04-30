package tickr.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {
    // From https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression
    private static final Pattern EMAIL_REGEX = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])");
    public static boolean isValidUrl (String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    public static boolean isValidEmail (String email) {
        return EMAIL_REGEX.matcher(email.toLowerCase().trim()).matches();
    }

    public static Set<String> toWords (String text) {
        if (text == null) {
            return new HashSet<>();
        }
        return Arrays.stream(text.replaceAll("\\p{P}", "").toLowerCase(Locale.ROOT) // remove all punctuation and convert to lowercase
                .split("\\s")) // split by whitespace
                .filter(Predicate.not(String::isBlank)) // Remove blanks
                .collect(Collectors.toSet()); // make set
    }

    public static Map<String, Long> toWordsMap (String text) {
        if (text == null) {
            return new HashMap<>();
        }

        return Arrays.stream(text.replaceAll("\\p{P}", "").toLowerCase(Locale.ROOT).split("\\s")) // remove all punctuation and convert to lowercase
                .filter(Predicate.not(String::isBlank)) // split by whitespace
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())); // make set
    }

    public static double getIdf (int documentCount, int numDocuments) {
        assert documentCount > 0; // Sanity check
        return Math.log10((double) numDocuments / documentCount);
    }
}
