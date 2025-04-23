package ShellParser;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

import com.lab1.distributedfs.ShellParser.ShellParser;
import com.lab1.distributedfs.ShellParser.ParseException;

public class ShellParserTest {
    @Test
    public void testEmpty() {
        Assertions.assertEquals(new ArrayList<String>(), ShellParser.safeParseString(""));
    }

    @Test
    public void testWord() {
        Assertions.assertEquals(new ArrayList<String>() {{ add("test"); }}, ShellParser.safeParseString("test"));
    }

    @Test
    public void testTwoWords() {
        Assertions.assertEquals(new ArrayList<String>() {{ add("a"); add("b"); }}, ShellParser.safeParseString("a b"));
    }

    @Test
    public void testManyWords() {
        List<String> expected = new ArrayList<String>() {{
            add("a");
            add("b");
            add("c");
            add("d");
            add("e");
        }};

        Assertions.assertEquals(expected, ShellParser.safeParseString("a b c d e"));
    }

    @Test
    public void testEscapedLiteral() {
        Assertions.assertEquals(new ArrayList<String>() {{ add("test"); }}, ShellParser.safeParseString("\\test"));
    }

    @Test
    public void testDoubleQuotes() {
        Assertions.assertEquals(new ArrayList<String>() {{ add("test test"); }}, ShellParser.safeParseString("\"test test\""));
    }

    @Test
    public void testMixedDoubleQuotes() {
        Assertions.assertEquals(new ArrayList<String>() {{ add("test"); add("test test"); add("test"); }}, ShellParser.safeParseString("test \"test test\" test"));
    }

    @Test
    public void testSingleQuotes() {
        Assertions.assertEquals(new ArrayList<String>() {{ add("test test"); }}, ShellParser.safeParseString("'test test'"));
    }

    @Test
    public void testMixedSingleQuotes() {
        Assertions.assertEquals(new ArrayList<String>() {{ add("test"); add("test test"); add("test"); }}, ShellParser.safeParseString("test 'test test' test"));
    }

    @Test
    public void testMixedQuotes() {
        Assertions.assertEquals(new ArrayList<String>() {{ add("test test"); add("test test"); }}, ShellParser.safeParseString("\"test test\" 'test test'"));
    }

    @Test
    public void testNestedQuotes() {
        Assertions.assertEquals(new ArrayList<String>() {{ add("test 'test test'"); }}, ShellParser.safeParseString("\"test 'test test'\""));
    }

    @Test
    public void testMismatchedDoubleQuote() throws ParseException {
//        ShellParser.parseString("\"");
        Assertions.assertThrows(ParseException.class, () -> ShellParser.parseString("\""));
    }

    @Test
    public void testMismatchedSingleQuote() throws ParseException {
//        ShellParser.parseString("'");
        Assertions.assertThrows(ParseException.class, () -> ShellParser.parseString("'"));
    }

    @Test
    public void testBadEscape() throws ParseException {
//        ShellParser.parseString("\\");
        Assertions.assertThrows(ParseException.class, () -> ShellParser.parseString("\""));
    }
}