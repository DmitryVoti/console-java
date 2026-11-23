package task2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article {
    private String author;
    private String title;
    private LocalDateTime publicationDate; // время выхода
    private int readingTimeMinutes;        // время прочтения
    private long views;                    // количество просмотров
    private String imageUrl;               // адрес изображения
    private List<String> rubrics;          // рубрики
    private String preview;                // превью/прехедер


    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

    public static List<Article> parse(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<Article> result = new ArrayList<>();
        List<String> block = new ArrayList<>();

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                if (!block.isEmpty()) {
                    result.add(fromBlock(block));
                    block.clear();
                }
            } else if (!line.startsWith("#")) {
                block.add(line);
            }
        }
        if (!block.isEmpty()) {
            result.add(fromBlock(block));
        }
        return result;
    }

    private static Article fromBlock(List<String> block) {
        if (block.size() < 7) {
            throw new IllegalStateException(
                    "Недостаточно полей в блоке: " + block.size() + " (нужно >= 7)");
        }
        return Article.builder()
                .author(block.get(0))
                .title(block.get(1))
                .publicationDate(LocalDateTime.parse(block.get(2), DATE_FORMAT))
                .readingTimeMinutes(parseReadingTime(block.get(3)))
                .views(parseViews(block.get(4)))
                .rubrics(Arrays.stream(block.get(5).split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList()))
                .imageUrl(block.get(6))
                .preview(String.join(" ", block.subList(7, block.size())))
                .build();
    }

    private static int parseReadingTime(String s) {
        String digits = s.replaceAll("^(\\d+).*$", "$1");
        if (!digits.matches("^\\d+$")) {
            throw new IllegalArgumentException("Не удалось распарсить время чтения: " + s);
        }
        return Integer.parseInt(digits);
    }

    private static long parseViews(String s) {
        String t = s.trim().toUpperCase().replace(",", ".");
        double multiplier = 1;
        if (t.endsWith("K")) {
            multiplier = 1_000;
            t = t.substring(0, t.length() - 1);
        } else if (t.endsWith("M")) {
            multiplier = 1_000_000;
            t = t.substring(0, t.length() - 1);
        }
        return (long) (Double.parseDouble(t) * multiplier);
    }
}
