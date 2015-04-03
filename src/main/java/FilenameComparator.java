import java.util.Comparator;
import java.util.Hashtable;

/**
 * This DOES:
 * - Order by number, where appropriate
 * - Ignores punctuation in order to compare digits/letters e.g. file1 < file_2 (See test cases in Tests.java)
 *
 * - Compares 1st: Verbal/numeric difference
 * - Compares 2nd: Case of font, type of punctuation, etc.
 * - Compares 3rd: Length
 *
 * - Optionally compares by file extension then by name
 *
 * This DOES NOT:
 * - Support non 7-bit ASCII characters
 * - Support strings of over MAX_LENGTH
 */
public class FilenameComparator implements Comparator<String> {
    private static final String MEANINGLESS = " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"; // Not great for meaningful sorting
    private static final char PADDING_CHAR = '\0';
    private static final Hashtable<Character, Integer> syntaxLookup = new Hashtable<>();
    private static final int MAX_LENGTH = 65535;

    private boolean fileExtensionCmp = false;

    public FilenameComparator() {
        /* Populate table with symbol categories in sorted order */
        if (syntaxLookup.isEmpty()) {
            for (Character c : MEANINGLESS.toCharArray()) {
                syntaxLookup.put(c, MEANINGLESS.indexOf(c)); // "[]()" implies [] are sorted before ()
            }
        }
    }

    public void setFileExtensionCmp(boolean on) {
        fileExtensionCmp = on;
    }

    /**
     * Compares two strings returning a negative number if s1 < s2, a positive
     * number if s1 > s2, and 0 if they are identical.
     *
     * Criteria:
     *  1a. Differences between letters [A-Za-z] cause an immediate decision
     *  1b. Differences in numbers cause an immediate decision as soon as the shorter number ends
     *  2. The first soft difference (e.g. [ vs { or A vs a) will then be the decider
     *  3. The raw length of the string, if none of the above
     *
     *
     * @param   s1 A string
     * @param   s2 Another string
     * @return  -1 if s1 is "less" than s2, 1 if vice versa, 0 if they are identical.
     */
    @Override
    public int compare(String s1, String s2) {
        if (isBadArg(s1) || isBadArg(s2)) {
            throw new IllegalArgumentException();
        }
        // Put file extensions at front if option is selected
        if (fileExtensionCmp) {
            s1 = rotateExtension(s1);
            s2 = rotateExtension(s2);
        }

        // Compare by letters/numbers
        int cmp = compareAlphanumeric(s1, s2);
        if (cmp != 0) {
            return cmp;
        }
        else {
            // Compare by case/punctuation ordering
            int softDiff = softCompare(s1, s2);
            if (softDiff != 0) {
                return softDiff;
            }
            // Compare by length
            else {
                return s1.length() - s2.length();
            }
        }
    }

    /**
     * Compares two strings returning a negative number if s1 < s2, a positive
     * number if s1 > s2, and 0 if they are identical.
     *
     * Letters and numbers are compared here.
     * Lengths of arguments, and 'soft' differences are NOT compared here.
     *
     * @param s1 String1
     * @param s2 String2
     * @return Alphanumerical comparison of them,
     */
    private int compareAlphanumeric(String s1, String s2) {
        char arr1[] = s1.toCharArray(), arr2[] = s2.toCharArray();
        char c1, c2;
        int end = Math.max(s1.length(), s2.length()), c1Num = 0, c2Num = 0, softDifference = 0;

        for (int i1 = 0, i2 = 0; i1 <= end || i2 <= end; i1++, i2++) {
            i1 = advanceIndex(arr1, i1);
            i2 = advanceIndex(arr2, i2);
            c1 = getNextChar(arr1, i1);
            c2 = getNextChar(arr2, i2);

            // Two digits - awkward because even if they are different we must keep parsing
            if (areBothDigits(c1, c2)) {
                c1Num = parseDigit(c1, c1Num); // move up a digit
                c2Num = parseDigit(c2, c2Num);

                int lookahead = cmpLookahead(getNextChar(arr1, i1 + 1), c1Num, getNextChar(arr2, i2 + 1), c2Num);
                if (lookahead != 0) {
                    return lookahead;
                }
            }
            else if (oneCharIsADigit(c1, c2)) {
                return Character.isDigit(c1) ? 1 : -1;
            }
            else if (areSignificantlyDifferent(c1, c2)) { // Compare non-numerical chars
                return c1 - c2;
            }
        }
        return 0;
    }

    /*
     * Examines the next two chars with respect to the current numbers and makes a decision if possible
     */
    private int cmpLookahead(char c1Next, int c1Num, char c2Next, int c2Num) {
        // Only one of the numbers continues -- make a decision
        if (oneCharIsADigit(c1Next, c2Next)) {
            return Character.isDigit(c1Next) ? 1 : -1;
        }
        // Both numbers end and current numbers differ -- make a decision
        else if (!isEitherADigit(c1Next, c2Next) && c1Num != c2Num) {
            return c1Num - c2Num;
        }
        return 0;
    }

    /*
     * Comparator for minor differences, returns 0 if no MINOR differences are spotted, otherwise
     * acts as normal comparator
     *
     * Chars contained in syntaxLookup are at most softly different, as are chars which only differ in
     * case e.g. 'A' and 'a'
     */
    private int getSoftDifference(char c1, char c2) {
        // Padding chars are indicative of length difference
        if (c1 == PADDING_CHAR || c2 == PADDING_CHAR) {
            return c1 == PADDING_CHAR ? -1 : 1;
        }

        // Punctuation/syntax has ordering although it will not show in most cases
        if (syntaxLookup.containsKey(c1) && syntaxLookup.containsKey(c2)) {
            return syntaxLookup.get(c1) - syntaxLookup.get(c2);
        }

        // Both are alphabetic characters
        if (Character.isAlphabetic(c1) && Character.isAlphabetic(c2)) {
            // The same character in a different case constitutes a soft difference
            if (Character.toLowerCase(c1) == Character.toLowerCase(c2)) {
                return c1 - c2;
            }
        }
        return 0;
    }

    /*
     * Returns the first soft difference in two strings
     */
    private int softCompare(String s1, String s2) {
        char[] arr1 = s1.toCharArray(), arr2 = s2.toCharArray();
        int softDiff;
        for (int i = 0; i < Math.min(s1.length(), s2.length()); i++) {
            softDiff = getSoftDifference(arr1[i], arr2[i]);
            if (softDiff != 0) {
                return softDiff;
            }
        }
        return 0;
    }

    /*
     * Significant difference and soft difference are mutually exclusive
     */
    private boolean areSignificantlyDifferent(char c1, char c2) {
        return c1 != c2 && getSoftDifference(c1, c2) == 0;
    }

    private boolean areBothDigits(char c1, char c2) {
        return Character.isDigit(c1) && Character.isDigit(c2);
    }

    private boolean isEitherADigit(char c1, char c2) {
        return Character.isDigit(c1) || Character.isDigit(c2);
    }

    private boolean oneCharIsADigit(char c1, char c2) {
        return Character.isDigit(c1) ^ Character.isDigit(c2);
    }

    /*
     * Returns the accumulated number obtained when parsing chars from left ro right
     */
    private int parseDigit(char inputChar, int currentNum) {
        currentNum *= 10; // Move across by a decimal digit
        return currentNum + Character.getNumericValue(inputChar);
    }

    /*
     * Returns the char at index i, or a padding char if i is out of bounds
     */
    private char getNextChar(char[] arr, int i) {
        if (i >= arr.length) {
            return PADDING_CHAR;
        } else {
            return arr[i];
        }
    }

    /*
     * Increments and returns the index pointing to the next comparable character
     */
    private int advanceIndex(char[] arr, int i) {
        char c = getNextChar(arr, i);
        while (syntaxLookup.containsKey(c)) {
            i++;
            c = getNextChar(arr, i);
        }
        return i;
    }

    /*
     * Returns a new string with the substring starting from (and including) the last '.'
     * moved to the front
     */
    private String rotateExtension(String s) {
        // Find extension
        int periodIx = s.lastIndexOf('.');
        if (periodIx == -1) {
            return s;
        }
        s = s.substring(periodIx) + s.substring(0, periodIx);
        return s;
    }

    private boolean isBadArg(String s) {
        return s == null || s.length() > MAX_LENGTH;
    }
}
