package task2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main() throws Exception {
        Path inputTxt    = Path.of("src/main/resources/articles.txt");
        Path jacksonJson = Path.of("src/main/resources/articles-jackson.json");
        Path gsonJson    = Path.of("src/main/resources/articles-gson.json");

        List<Article> articles = Article.parse(inputTxt);
        log.info("Прочитано статей из текстового файла: {}", articles.size());

        ArticleJsonJackson jackson = new ArticleJsonJackson();
        jackson.write(jacksonJson, articles);
        log.info("Jackson: JSON сохранён в {}", jacksonJson);

        List<Article> fromJackson = jackson.read(jacksonJson);
        checkEquals(articles, fromJackson, "Jackson");

        ArticleJsonGson gson = new ArticleJsonGson();
        gson.write(gsonJson, articles);
        log.info("Gson: JSON сохранён в {}", gsonJson);

        List<Article> fromGson = gson.read(gsonJson);
        checkEquals(articles, fromGson, "Gson");
    }

    private static void checkEquals(List<Article> a, List<Article> b, String library) {
        if (!a.equals(b)) {
            log.error("{}: round-trip не совпал!", library);
            throw new AssertionError(library + " round-trip mismatch");
        }
        log.info("{}: round-trip совпал — {} статей", library, b.size());
    }
}
