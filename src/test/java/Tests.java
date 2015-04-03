import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class Tests {
    @Test
    public void fileExtensions() {
        FilenameComparator fileComp = new FilenameComparator();
        fileComp.setFileExtensionCmp(true);

        String[][] cases = {
                { "hello.bmp", "aaa.jpg" },
                { "zzz.png", "ello.ps1", "elmo.ps1" },
                { "z.pdf", "1.pdf", "src.7zip" }
        };

        for (String[] list : cases) {
            String[] sorted = list.clone();
            // shuffle the array
            reverse(sorted, 0, sorted.length - 1);
            reverse(sorted, 1, sorted.length - 2);
            Arrays.sort(sorted, fileComp);
            Assert.assertEquals(concatArray(list), concatArray(sorted));
        }
    }

    @Test
    public void alphanumerical() {
        FilenameComparator fileComp = new FilenameComparator();
        // Input lists given in correct order
        String[][] cases = {
                { "1", "2", "3" },
                { "1", "2", "10"},
                { "1 2 10", "1 10 2" },
                { "1102", "1210" },
                { "1/5", "1/20"},
                { "-1", "-5" },
                { "A", "a" },
                { "a1", "A2" },
                { "file.txt", "file1.txt" },
                { "file", "file.txt", "file1", "file1.txt" },
                { "File(1).txt", "file(1)", "file2.txt", "file(10).txt" },
                { generateAscii(200), generateAscii(60000) },
                { "myFile.jpg", "myfile.jpg", "my_file.jpg" }, // capitals before non capitals, shorter before longer
                { "a file", "bongo", "catpics", "dvd.rip", "elephant.txt", "elephant_2.txt" },
                { "myFile.jpg", "myfile.jpg", "my_file.jpg", "my_file.png",  },
                { "1A", "1A", "1A", "1A", "1a", "1a", "1a" },
                { generateAscii(30000), generateAscii(30000) + "1", generateAscii(30000) + "11" },
                { "11", "11.jpg", "21", "21", "31" },
                { "", "e", "e11" },
                { generateAscii(64000), generateAscii(64000) + "(1)", generateAscii(64000) + "_(2)", generateAscii(64000) + "3.txt" },
                { "__1", "{2}", ".4lions.mkv", "\"5guysmenu.pdf\"", "--%%%3239457209485"},
                { "----", "---_", "--__", "-___", "____" }, // No difference in length, only soft diffs
                { "(hello)", "(helloworld)", "[hello]"}, // Shows soft differences take prec over length diffs
                { "--", "---", "----", "-----" }, // only length diffs
                { "Apple", "apple", "[Apple]", "apple1", "apple2", "apple(10)", "apple((10))"}
        };

        for (String[] list : cases) {
            String[] clone1 = list.clone();
            String[] clone2 = list.clone();

            // Sort one reversed list, and one in correct order
            reverse(clone1, 0, clone1.length - 1);

            Arrays.sort(clone1, fileComp);
            Arrays.sort(clone2, fileComp);

            Assert.assertEquals(concatArray(list), concatArray(clone1));
            Assert.assertEquals(concatArray(list), concatArray(clone2));
        }
    }

    @Test
    public void equality() {
        FilenameComparator fileComp = new FilenameComparator();
        String[] cases = {
                "",
                "hello",
                generateAscii(200),
                generateAscii(65535),
                "A",
                "a",
                "1",
                "2"
        };

        // Check false negatives
        for (String str : cases) {
            Assert.assertEquals(fileComp.compare(str, str), 0);
        }

        // Check false positives
        for (int i = 0; i < cases.length; i++) {
            // Check against next in array
            int next = (i + 1) % cases.length;
            Assert.assertNotEquals(fileComp.compare(cases[i], cases[next]), 0);
        }
    }

    @Test
    public void badArgs() {
        FilenameComparator fileComp = new FilenameComparator();
        // Input lists given in correct order
        String[][] cases = {
                { null , null },
                { "hello", null},
                { generateAscii(100000), "blah" }
        };

        for (String[] list : cases) {
            try {
                Arrays.sort(list, fileComp);
                Assert.assertFalse(true);
            } catch (IllegalArgumentException e) {
                continue;
            }
        }
    }

    /**
     * Create human-friendly string from array
     */
    private static String concatArray(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (String str : arr) {
            sb.append("{"); // For ease of debugging
            sb.append(str);
            sb.append("} ");
        }
        return sb.toString();
    }

    private static String[] reverse(String[] arr, int a, int b) {
        while (a < b) {
            String tmp = arr[a];
            arr[a] = arr[b];
            arr[b] = tmp;
            a++;
            b--;
        }
        return arr;
    }

    /**
     * Create repeating ASCII character string with default ordering
     * @param   length The length of the output generated string
     * @return  A string
     */
    private static String generateAscii(int length) {
        /* Only ASCII chars 32 to 126 inclusive are printable */
        int start = 32;
        int end = 127;
        int nPrintableChars = start - end;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(Character.toString( (char) (start + (i % nPrintableChars))) );
        }
        return sb.toString();
    }
}
