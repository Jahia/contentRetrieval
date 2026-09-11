package org.jahia.modules.contentretrieval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

/**
 * The views send every JCR property value to the page through an encoder.
 *
 * <p>An EL expression in template text is written to the response as it stands. The same
 * expression inside a tag is an attribute the tag consumes, so this test looks at template text
 * only, which is what the scanner below separates.
 */
public class ViewOutputEncodingTest {

    private static final List<String> ENCODERS =
            Collections.unmodifiableList(java.util.Arrays.asList("fn:escapeXml(", "functions:escapeJavaScript("));

    /** A property value read off a JCR node, in the shapes these views use. */
    private static final List<String> VALUE_ACCESSORS =
            Collections.unmodifiableList(java.util.Arrays.asList(".string", ".name", ".properties[", ".propertiesAsString"));

    @Test
    public void everyPropertyValueInTemplateTextIsEncoded() throws Exception {
        Path root = viewRoot();
        List<Path> views = views(root);
        List<String> unencoded = new ArrayList<>();
        int expressions = 0;
        int valueExpressions = 0;

        for (Path view : views) {
            for (String expression : templateTextExpressions(read(view))) {
                expressions++;
                if (!isValueRead(expression)) {
                    continue;
                }
                valueExpressions++;
                if (!isEncoded(expression)) {
                    unencoded.add(root.relativize(view) + " -> ${" + expression + "}");
                }
            }
        }

        // A scan that reaches nothing reports the same "clean" as one that passes, so each
        // count below has to be non-zero before the assertion on the list means anything.
        assertFalse("no views were scanned under " + root, views.isEmpty());
        assertTrue("no EL expression was scanned in template text", expressions > 0);
        assertTrue("no property value was scanned in template text", valueExpressions > 0);
        assertEquals("a property value written to the page is expected to go through an encoder",
                Collections.emptyList(), unencoded);
    }

    private static boolean isValueRead(String expression) {
        return VALUE_ACCESSORS.stream().anyMatch(expression::contains);
    }

    private static boolean isEncoded(String expression) {
        return ENCODERS.stream().anyMatch(encoder -> expression.trim().startsWith(encoder));
    }

    /**
     * The bodies of every <code>${...}</code> that sits in template text, so outside any tag and
     * outside a JSP comment or scriptlet. Nesting is not a case here: an EL body carries no
     * <code>}</code> of its own.
     */
    static List<String> templateTextExpressions(String view) {
        List<String> found = new ArrayList<>();
        int i = 0;
        while (i < view.length()) {
            if (view.startsWith("<%--", i)) {
                i = skipTo(view, i, "--%>");
            } else if (view.startsWith("<%", i)) {
                i = skipTo(view, i, "%>");
            } else if (view.charAt(i) == '<' && startsTag(view, i)) {
                i = skipTo(view, i, ">");
            } else if (view.startsWith("${", i)) {
                int end = view.indexOf('}', i);
                if (end < 0) {
                    break;
                }
                found.add(view.substring(i + 2, end));
                i = end + 1;
            } else {
                i++;
            }
        }
        return found;
    }

    private static boolean startsTag(String view, int at) {
        int next = at + 1;
        if (next >= view.length()) {
            return false;
        }
        char c = view.charAt(next);
        return c == '/' || Character.isLetter(c);
    }

    /** The index just past <code>token</code>, or the end of the view when it never closes. */
    private static int skipTo(String view, int from, String token) {
        int at = view.indexOf(token, from + 1);
        return at < 0 ? view.length() : at + token.length();
    }

    private static Path viewRoot() throws URISyntaxException {
        Path codeSource = Paths.get(ViewOutputEncodingTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        Path packaged = codeSource.resolveSibling("classes");
        return Files.isDirectory(packaged) ? packaged : Paths.get("src", "main", "resources");
    }

    private static List<Path> views(Path root) throws IOException {
        try (Stream<Path> tree = Files.walk(root)) {
            return tree.filter(ViewOutputEncodingTest::isView).sorted().collect(Collectors.toList());
        }
    }

    private static boolean isView(Path path) {
        String name = path.getFileName().toString();
        return Files.isRegularFile(path) && (name.endsWith(".jsp") || name.endsWith(".jspf"));
    }

    // ISO-8859-1 maps every byte to a char, so a view in any encoding is readable and the
    // ASCII-only tokens above still match.
    private static String read(Path view) throws IOException {
        return new String(Files.readAllBytes(view), StandardCharsets.ISO_8859_1);
    }
}
