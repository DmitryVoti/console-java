package directory;

import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkingDirectory: расширенное тестирование (исключения, ветки, дефекты)")
class WorkingDirectoryTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() throws IOException {
        System.setOut(new PrintStream(outputStreamCaptor));

        Path rootPath = Paths.get("my_folder");
        if (Files.exists(rootPath)) {
            try (Stream<Path> walk = Files.walk(rootPath)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
        Files.createDirectories(rootPath);

        String[] structure = {
            "documents/readme.txt", "documents/report.pdf", "documents/notes.docx",
            "projects/java/Main.java", "projects/java/Utils.class",
            "projects/web/index.html", "projects/web/style.css",
            "downloads/archive.zip", "downloads/photo.png", "downloads/music.mp3",
            "empty_subfolder/"
        };
        for (String subPath : structure) {
            File element = new File(rootPath.toFile(), subPath);
            if (subPath.endsWith("/")) element.mkdirs();
            else {
                element.getParentFile().mkdirs();
                element.createNewFile();
            }
        }
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
    }

    private String getOutput() {
        return outputStreamCaptor.toString().replace("\r\n", "\n").trim();
    }

    private String canonical(String path) throws IOException {
        return Paths.get(path).toRealPath().toString();
    }

    private void deleteRecursively(String path) throws IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) return;
        try (Stream<Path> walk = Files.walk(p)) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    private WorkingDirectory fresh() throws IOException {
        return WorkingDirectory.getInstance("my_folder");
    }

    // ==================== getInstance ====================

    @Test
    @DisplayName("getInstance(null): NPE (new File(null))")
    void getInstance_Null_ThrowsNPE() {
        assertThrows(
            NullPointerException.class,
            () -> WorkingDirectory.getInstance(null));
    }

    @Test
    @DisplayName("getInstance: несуществующий каталог → IOException")
    void getInstance_NonExistent_ThrowsIOException() {
        assertThrows(
            IOException.class,
            () -> WorkingDirectory.getInstance("NonExistentDirectory12345"));
    }

    @Test
    @DisplayName("getInstance: пробельное имя → IOException")
    void getInstance_Whitespace_ThrowsIOException() {
        assertThrows(
            IOException.class,
            () -> WorkingDirectory.getInstance("   "));
    }

    @Test
    @DisplayName("getInstance(путь к файлу) → IOException")
    void getInstance_File_ThrowsIOException() throws IOException {
        String filePath = "my_folder/documents/readme.txt";
        assertTrue(Files.isRegularFile(Paths.get(filePath)));

        IOException ex = assertThrows(
            IOException.class,
            () -> WorkingDirectory.getInstance(filePath));
        assertTrue(
            ex.getMessage().contains("не является каталогом"),
            "Ожидалось сообщение про 'не является каталогом', получено: " + ex.getMessage());
    }

    @Test
    @DisplayName("getInstance(файл) не портит состояние Singleton")
    void getInstance_File_DoesNotCorruptSingleton() throws IOException {
        WorkingDirectory wd = WorkingDirectory.getInstance("my_folder");
        assertEquals(canonical("my_folder"), wd.getDirectoryName());

        assertThrows(
            IOException.class,
            () -> WorkingDirectory.getInstance("my_folder/documents/readme.txt"));

        // состояние Singleton не должно измениться
        assertSame(wd, WorkingDirectory.getInstance("my_folder"));
        assertEquals(canonical("my_folder"), wd.getDirectoryName());
    }

    @Test
    @DisplayName("getInstance: повторный вызов возвращает тот же экземпляр")
    void getInstance_ReturnsSameInstance() throws IOException {
        WorkingDirectory a = WorkingDirectory.getInstance("my_folder");
        WorkingDirectory b = WorkingDirectory.getInstance("my_folder/downloads");
        assertSame(a, b);
    }

    // ==================== toSubdirectory ====================

    @Test
    @DisplayName("toSubdirectory(null): ошибка для пустого ввода")
    void toSubdirectory_Null_ThrowsNPE() throws IOException {
        WorkingDirectory wd = fresh();
        val currentResult = wd.toSubdirectory(null);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("toSubdirectory(\".\"): остаётся в текущем каталоге, статус OK")
    void toSubdirectory_Dot_StaysInPlace() throws IOException {
        WorkingDirectory wd = fresh();
        String before = wd.getDirectoryName();

        OperationResult s = wd.toSubdirectory(".");

        assertEquals(OperationResult.Status.OK, s.status());
        assertEquals(before, wd.getDirectoryName());
    }

    @Test
    @DisplayName("toSubdirectory: ошибка для несуществующего каталога")
    void toSubdirectory_NonExistent_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.toSubdirectory("nope");
        assertEquals(OperationResult.Status.ERROR, s.status());
    }

    @Test
    @DisplayName("toSubdirectory: ошибка для файла")
    void toSubdirectory_File_ReturnsError() throws IOException {
        WorkingDirectory wd = WorkingDirectory.getInstance("my_folder/documents");
        OperationResult s = wd.toSubdirectory("readme.txt");
        assertEquals(OperationResult.Status.ERROR, s.status());
    }

    // ==================== createNewDirectory ====================

    @Test
    @DisplayName("createNewDirectory(null): ошибка для пустого ввода")
    void createNewDirectory_Null_ThrowsNPE() throws IOException {
        WorkingDirectory wd = fresh();
        val currentResult = wd.createNewDirectory(null);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("createNewDirectory(\"a/b\"): ERROR — mkdir не создаёт вложенные")
    void createNewDirectory_NestedPath_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.createNewDirectory("a/b");

        assertEquals(OperationResult.Status.ERROR, s.status());
        assertFalse(Files.exists(Paths.get("my_folder/a")));
    }

    @Test
    @DisplayName("createNewDirectory: успешное создание")
    void createNewDirectory_Success() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.createNewDirectory("new_dir");

        assertEquals(OperationResult.Status.OK, s.status());
        assertTrue(Files.isDirectory(Paths.get("my_folder/new_dir")));
    }

    @Test
    @DisplayName("createNewDirectory: ошибка если уже существует")
    void createNewDirectory_AlreadyExists_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.createNewDirectory("documents");
        assertEquals(OperationResult.Status.ERROR, s.status());
    }

    // ==================== findFile ====================

    @Test
    @DisplayName("findFile(null): возвращает статус ERROR")
    void findFile_Null_ThrowsNPE() throws IOException {
        WorkingDirectory wd = fresh();
        val currentResult = wd.findFile(null, false);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("findFile(\"\"): ошибка пустого файла")
    void findFile_Empty_ReturnsAllFiles() throws IOException {
        WorkingDirectory wd = fresh();
        val currentResult = wd.findFile("", false);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("findFile: регистронезависимый поиск")
    void findFile_CaseInsensitive() throws IOException {
        WorkingDirectory wd = fresh();
        wd.findFile("README", false);
        assertEquals(canonical("my_folder/documents/readme.txt"), getOutput());
    }

    @Test
    @DisplayName("findFile: поиск по расширению")
    void findFile_ByExtension() throws IOException {
        WorkingDirectory wd = fresh();
        wd.findFile(".txt", false);
        assertEquals(canonical("my_folder/documents/readme.txt"), getOutput());
    }

    @Test
    @DisplayName("findFile: только каталоги")
    void findFile_OnlyDirectories() throws IOException {
        WorkingDirectory wd = fresh();
        wd.findFile("doc", true);
        assertEquals(canonical("my_folder/documents"), getOutput());
    }

    @Test
    @DisplayName("findFile: статус OK")
    void findFile_StatusOk() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.findFile("readme", false);
        assertEquals(OperationResult.Status.OK, s.status());
    }

    // ==================== truncate ====================

    @Test
    @DisplayName("truncate(null): ERROR (NPE перехватывается catch Exception)")
    void truncate_Null_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.truncate(null, false);
        assertEquals(OperationResult.Status.ERROR, s.status());
    }

    @Test
    @DisplayName("truncate: файл вместо каталога — OK (сообщение вводит в заблуждение)")
    void truncate_FileNotDirectory_ReturnsOk() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.truncate("documents/readme.txt", false);

        assertEquals(OperationResult.Status.OK, s.status());
        assertFalse(Files.exists(Paths.get("my_folder/documents/readme.txt")));
    }

    @Test
    @DisplayName("truncate: каталог с подкаталогами + includeSubdirectories=false → ERROR")
    void truncate_DirWithSubdirs_IncludeFalse_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.truncate("projects", false);

        assertEquals(OperationResult.Status.ERROR, s.status());
        assertTrue(Files.isDirectory(Paths.get("my_folder/projects")));
        assertTrue(Files.isDirectory(Paths.get("my_folder/projects/java")));
    }

    @Test
    @DisplayName("truncate: каталог с подкаталогами + includeSubdirectories=true → OK")
    void truncate_DirWithSubdirs_IncludeTrue_ReturnsOk() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.truncate("projects", true);

        assertEquals(OperationResult.Status.OK, s.status());
        assertFalse(Files.exists(Paths.get("my_folder/projects")));
    }

    @Test
    @DisplayName("truncate: несуществующий → ERROR")
    void truncate_NonExistent_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.truncate("no_such", true);
        assertEquals(OperationResult.Status.ERROR, s.status());
    }

    // ==================== isItChildDirectory ====================

    @Test
    @DisplayName("isItChildDirectory(null): ERROR, ничего не печатает")
    void isItChildDirectory_Null_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.isItChildDirectory(null);

        assertEquals(OperationResult.Status.ERROR, s.status());
        assertEquals("", getOutput());
    }

    @Test
    @DisplayName("isItChildDirectory(\"\"): ERROR, ничего не печатает")
    void isItChildDirectory_Empty_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.isItChildDirectory("");

        assertEquals(OperationResult.Status.ERROR, s.status());
        assertEquals("", getOutput());
    }

    @Test
    @DisplayName("isItChildDirectory(\"   \"): ERROR (isBlank)")
    void isItChildDirectory_Blank_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.isItChildDirectory("   ");
        assertEquals(OperationResult.Status.ERROR, s.status());
    }

    @Test
    @DisplayName("isItChildDirectory: существует → OK, печатает true")
    void isItChildDirectory_Existing_ReturnsOk() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.isItChildDirectory("documents");

        assertEquals(OperationResult.Status.OK, s.status());
        assertEquals("true", getOutput());
    }

    @Test
    @DisplayName("isItChildDirectory: не существует → ERROR, печатает false")
    void isItChildDirectory_NonExisting_ReturnsError() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.isItChildDirectory("nope");

        assertEquals(OperationResult.Status.ERROR, s.status());
        assertEquals("false", getOutput());
    }

    @Test
    @DisplayName("isItChildDirectory: файл — не дочерний каталог")
    void isItChildDirectory_File_ReturnsError() throws IOException {
        WorkingDirectory wd = WorkingDirectory.getInstance("my_folder/documents");
        OperationResult s = wd.isItChildDirectory("readme.txt");

        assertEquals(OperationResult.Status.ERROR, s.status());
        assertEquals("false", getOutput());
    }

    // ==================== print / drawFileTree: ERROR-ветки ====================

    @Test
    @DisplayName("print: каталог удалён → ERROR")
    void print_AfterDirectoryDeleted_ReturnsError() throws IOException {
        WorkingDirectory wd = WorkingDirectory.getInstance("my_folder/documents");
        deleteRecursively("my_folder/documents");

        OperationResult s = wd.print(false, true);

        assertEquals(OperationResult.Status.ERROR, s.status());
    }

    @Test
    @DisplayName("drawFileTree после удаления каталога → ERROR")
    void drawFileTree_AfterDirectoryDeleted_ReturnsError() throws IOException {
        WorkingDirectory wd = WorkingDirectory.getInstance("my_folder/empty_subfolder");
        Files.delete(Paths.get("my_folder/empty_subfolder"));

        OperationResult s = wd.drawFileTree(null);

        assertEquals(OperationResult.Status.ERROR, s.status());
        assertTrue(
            s.message().contains("не существует"),
            "Ожидалось сообщение про отсутствие пути, получено: " + s.message());
    }

    @Test
    @DisplayName("print: статус OK при существующем каталоге")
    void print_StatusOk() throws IOException {
        WorkingDirectory wd = fresh();
        assertEquals(OperationResult.Status.OK, wd.print(false, true).status());
    }

    @Test
    @DisplayName("drawFileTree: статус OK при существующем каталоге")
    void drawFileTree_StatusOk() throws IOException {
        WorkingDirectory wd = fresh();
        assertEquals(OperationResult.Status.OK, wd.drawFileTree(null).status());
    }

    @Test
    @DisplayName("printParentPath: статус OK и корректный абсолютный путь")
    void printParentPath_StatusAndPath() throws IOException {
        WorkingDirectory wd = fresh();
        OperationResult s = wd.printParentPath();

        assertEquals(OperationResult.Status.OK, s.status());
        assertEquals(Paths.get("my_folder").toRealPath().getParent().toString(), getOutput());
    }

    @Test
    @DisplayName("toParentDirectory: статус OK и переход из подкаталога")
    void toParentDirectory_StatusAndPath() throws IOException {
        WorkingDirectory wd = WorkingDirectory.getInstance("my_folder/documents");
        OperationResult s = wd.toParentDirectory();

        assertEquals(OperationResult.Status.OK, s.status());
        assertEquals(canonical("my_folder"), wd.getDirectoryName());
    }

    @Test
    @DisplayName("printCurrentDirectory: canonical путь с '>'")
    void printCurrentDirectory_PrintsCanonicalPath() throws IOException {
        WorkingDirectory wd = fresh();
        wd.printCurrentDirectory();
        assertEquals(canonical("my_folder") + ">", getOutput());
    }

    @Test
    @DisplayName("print: только первый уровень — содержимое")
    void print_FirstLevel_Content() throws IOException {
        WorkingDirectory wd = fresh();
        wd.print(false, true);

        String expected = "my_folder\n"
            + "│  documents\n"
            + "│  downloads\n"
            + "│  empty_subfolder\n"
            + "│  projects";
        assertEquals(expected, getOutput());
    }

    @Test
    @DisplayName("print: рекурсивный, только каталоги — содержимое")
    void print_Recursive_DirectoriesOnly_Content() throws IOException {
        WorkingDirectory wd = fresh();
        wd.print(true, false);

        String expected = "my_folder\n"
            + "│  documents\n"
            + "│  downloads\n"
            + "│  empty_subfolder\n"
            + "│  projects\n"
            + "│  │  java\n"
            + "│  │  web";
        assertEquals(expected, getOutput());
    }

    @Test
    @DisplayName("print: рекурсивный, все — содержимое")
    void print_Recursive_All_Content() throws IOException {
        WorkingDirectory wd = fresh();
        wd.print(false, false);

        String expected = "my_folder\n"
            + "│  documents\n"
            + "│  │  notes.docx\n"
            + "│  │  readme.txt\n"
            + "│  │  report.pdf\n"
            + "│  downloads\n"
            + "│  │  archive.zip\n"
            + "│  │  music.mp3\n"
            + "│  │  photo.png\n"
            + "│  empty_subfolder\n"
            + "│  projects\n"
            + "│  │  java\n"
            + "│  │  │  Main.java\n"
            + "│  │  │  Utils.class\n"
            + "│  │  web\n"
            + "│  │  │  index.html\n"
            + "│  │  │  style.css";
        assertEquals(expected, getOutput());
    }

    @Test
    @DisplayName("drawFileTree: иерархическое дерево")
    void drawFileTree_Content() throws IOException {
        WorkingDirectory wd = fresh();
        wd.drawFileTree(null);

        String expected = "my_folder\n"
            + "├── documents\n"
            + "│   ├── notes.docx\n"
            + "│   ├── readme.txt\n"
            + "│   └── report.pdf\n"
            + "├── downloads\n"
            + "│   ├── archive.zip\n"
            + "│   ├── music.mp3\n"
            + "│   └── photo.png\n"
            + "├── empty_subfolder\n"
            + "└── projects\n"
            + "    ├── java\n"
            + "    │   ├── Main.java\n"
            + "    │   └── Utils.class\n"
            + "    └── web\n"
            + "        ├── index.html\n"
            + "        └── style.css";
        assertEquals(expected, getOutput());
    }


    @Test
    @DisplayName("drawFileTree(path): успешное сохранение структуры в текстовый файл")
    void drawFileTree_WithValidPath_SavesToFile() throws IOException {
        WorkingDirectory wd = fresh();
        String targetDir = "my_folder/empty_subfolder";

        OperationResult s = wd.drawFileTree(targetDir);

        assertEquals(OperationResult.Status.OK, s.status());
        File expectedFile = new File(targetDir, "directory_structure.txt");
        assertTrue(expectedFile.exists(), "Файл directory_structure.txt должен быть создан");
        assertTrue(expectedFile.length() > 0, "Файл не должен быть пустым");
    }

    @Test
    @DisplayName("toSubdirectory: успешный переход в дочерний каталог и обратно через '..'")
    void toSubdirectory_SuccessAndParent() throws IOException {
        WorkingDirectory wd = fresh();
        String initial = wd.getDirectoryName();

        OperationResult s = wd.toSubdirectory("documents");
        assertEquals(OperationResult.Status.OK, s.status());
        assertTrue(wd.getDirectoryName().endsWith("documents"));

        OperationResult sBack = wd.toSubdirectory("..");
        assertEquals(OperationResult.Status.OK, sBack.status());
        assertEquals(initial, wd.getDirectoryName());
    }

    @Test
    @DisplayName("printParentPath и toParentDirectory: поведение на уровне корня диска ( ERROR )")
    void parentOperations_AtRoot_ReturnsError() throws IOException {
        // Получаем корневой каталог текущей системы (например, "C:\" или "/")
        File rootFile = File.listRoots()[0];
        WorkingDirectory rootWd = WorkingDirectory.getInstance(rootFile.getAbsolutePath());

        OperationResult printStatus = rootWd.printParentPath();
        assertEquals(OperationResult.Status.ERROR, printStatus.status());

        OperationResult jumpStatus = rootWd.toParentDirectory();
        assertEquals(OperationResult.Status.ERROR, jumpStatus.status());
    }
}