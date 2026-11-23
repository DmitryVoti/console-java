package task2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ArticleJsonGson {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeGsonAdapter())
            .setPrettyPrinting()
            .create();

    public void write(Path file, List<Article> articles) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(articles, writer);
        }
    }

    public List<Article> read(Path file) throws IOException {
        Type type = new TypeToken<List<Article>>() {}.getType();
        try (Reader reader = Files.newBufferedReader(file)) {
            return gson.fromJson(reader, type);
        }
    }
}
