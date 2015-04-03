# Filename Comparator

**A string comparator for intuitively ordering files**

Overall comparison strategy:

1. Differences between letters **OR** differences in numbers
3. The first 'soft' difference (e.g. *[* vs *{* or *A* vs *a*) will then be the decider
4. The raw length of the string, if none of the above

This file comparator treats contiguous digits as whole numbers, and everything else as delimiters. Punctuation is totally ignored, apart from being used as a delimiter for numbers. This allows the more meaningful elements of strings (i.e. the text and numbers) to be compared, whilst still recognising that punctuation may break up numbers.

So:
> file1 < file(1) < file10

The code is extensible in that rather than ignoring punctuation, it can be modified to use them as a secondary ordering criteria.

Sorting by file extension
-------------------------

Client code can request the comparator to sort firstly by extension (and then by normal criteria) by:

    FilenameComparator fc = new FilenameComparator();
    fc.setFileExtensionCmp(true);


Potential Improvements
----------------------

- This could be reimplemented as a state machine -- it may lead to simpler and more efficient code
- Dates in various formats could be identified and sorted
- Group words using camel case and other delimiters, similarly to how integers are
- Expand to all unicode filenames
- Expand to ordering with respect to a given locale
