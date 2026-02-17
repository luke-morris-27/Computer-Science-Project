package parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/*
 * Class: Main
 * Created by: Archisha Sasson
 * Description: Command-line entry point that parses a text file, prints a
 * summary, and writes parse output as JSON.
 */
// Code by Archisha Sasson
public class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java parser.Main <file-path>");
            System.exit(1);
        }

        Path inputFile = Path.of(args[0]);
        TextParser parser = new TextParser();

        try {
            ParseResult result = parser.parse(inputFile);
            printSummary(result);
            Path outputPath = resolveOutputPath();
            writeJson(result, outputPath);
            System.out.println("JSON written to: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to parse file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printSummary(ParseResult result) {
        System.out.println("Parse summary:");
        System.out.println("File: " + result.getFileName());
        System.out.println("Imported at: " + result.getImportedAt());
        System.out.println("Total words: " + result.getTotalWords());
        System.out.println("Total sentences: " + result.getTotalSentences());
        // Sammy Pandey: total paragraph counter: -----------------------------------
        System.out.println("Total paragraphs: " + result.getTotalParagraphs());
        // --------------------------------------------------------------------------
        System.out.println("Unique words: " + result.getWordCounts().size());
    }

    private static Path resolveOutputPath() {
        if (Files.isDirectory(Path.of("src", "main", "java"))) {
            return Path.of("output", "parse_result.json");
        }
        if (Files.isDirectory(Path.of("parser", "src", "main", "java"))) {
            return Path.of("parser", "output", "parse_result.json");
        }
        return Path.of("output", "parse_result.json");
    }

    private static void writeJson(ParseResult result, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, toJson(result));
    }

    private static String toJson(ParseResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"fileMeta\": {\n");
        sb.append("    \"fileName\": \"").append(escapeJson(result.getFileName())).append("\",\n");
        sb.append("    \"totalWords\": ").append(result.getTotalWords()).append(",\n");
        sb.append("    \"totalSentences\": ").append(result.getTotalSentences()).append(",\n");
        // Shriram Janardhan: Include totalParagraphs in JSON output
        sb.append("    \"totalParagraphs\": ").append(result.getTotalParagraphs()).append(",\n");
        sb.append("    \"importedAt\": \"").append(result.getImportedAt()).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"wordCounts\": ");
        appendIntMap(sb, result.getWordCounts(), 2);
        sb.append(",\n");
        sb.append("  \"sentenceStartCounts\": ");
        appendIntMap(sb, result.getSentenceStartCounts(), 2);
        sb.append(",\n");
        sb.append("  \"sentenceEndCounts\": ");
        appendIntMap(sb, result.getSentenceEndCounts(), 2);
        sb.append(",\n");
        sb.append("  \"nextWordCounts\": ");
        appendNestedIntMap(sb, result.getNextWordCounts(), 2);
        sb.append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendIntMap(StringBuilder sb, Map<String, Integer> map, int indent) {
        String indentText = " ".repeat(indent);
        String childIndent = " ".repeat(indent + 2);
        sb.append("{");

        if (map.isEmpty()) {
            sb.append("}");
            return;
        }

        sb.append("\n");
        int i = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            sb.append(childIndent)
                .append("\"")
                .append(escapeJson(entry.getKey()))
                .append("\": ")
                .append(entry.getValue());

            if (++i < map.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(indentText).append("}");
    }

    private static void appendNestedIntMap(
        StringBuilder sb,
        Map<String, Map<String, Integer>> map,
        int indent
    ) {
        String indentText = " ".repeat(indent);
        String childIndent = " ".repeat(indent + 2);
        sb.append("{");

        if (map.isEmpty()) {
            sb.append("}");
            return;
        }

        sb.append("\n");
        int i = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : map.entrySet()) {
            sb.append(childIndent)
                .append("\"")
                .append(escapeJson(entry.getKey()))
                .append("\": ");
            appendIntMap(sb, entry.getValue(), indent + 2);

            if (++i < map.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(indentText).append("}");
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
            }
        }
        return escaped.toString();
    }
}
// End of Code by Archisha Sasson
