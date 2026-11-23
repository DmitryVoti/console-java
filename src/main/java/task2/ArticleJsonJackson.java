package task2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ArticleJsonJackson {

    private final ObjectMapper mapper;

    public ArticleJsonJackson() {
        SimpleModule dateModule = new SimpleModule();
        dateModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(Article.DATE_FORMAT));
        dateModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(Article.DATE_FORMAT));

        this.mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(dateModule)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    public void write(Path file, List<Article> articles) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), articles);
    }

    public List<Article> read(Path file) throws IOException {
        return mapper.readValue(file.toFile(),
                new TypeReference<List<Article>>() {});
    }
}
