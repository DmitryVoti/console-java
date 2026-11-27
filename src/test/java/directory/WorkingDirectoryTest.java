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
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkingDirectory: расширенное тестирование (исключения, ветки, дефекты)")
class WorkingDirectoryTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() throws IOException {
        System.setOut(new PrintStream(outputStreamCaptor));

        val rootPath = Paths.get("my_folder");
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
        for (val subPath : structure) {
            val element = new File(rootPath.toFile(), subPath);
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
        return outputStreamCaptor
            .toString()
            .replace("\r\n", "\n")
            .trim();
    }

    private String canonical(String path) throws IOException {
        return Paths
            .get(path)
            .toRealPath()
            .toString();
    }

    private void deleteRecursively(final String path) throws IOException {
        Path currentPath = Paths.get(path);
        if (!Files.exists(currentPath)) return;
        try (val walk = Files.walk(currentPath)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
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
    void getInstance_File_ThrowsIOException() {
        val filePath = "my_folder/documents/readme.txt";
        assertTrue(Files.isRegularFile(Paths.get(filePath)));
        val thrown = assertThrows(
            IOException.class,
            () -> WorkingDirectory.getInstance(filePath));
        assertTrue(
            thrown.getMessage().contains("не является каталогом"),
            "Ожидалось сообщение про 'не является каталогом', получено: " + thrown.getMessage());
    }

    @Test
    @DisplayName("getInstance(файл) не портит состояние Singleton")
    void getInstance_File_DoesNotCorruptSingleton() throws IOException {
        val workingDirectory = WorkingDirectory.getInstance("my_folder");
        assertEquals(canonical("my_folder"), workingDirectory.getDirectoryName());
        assertThrows(
            IOException.class,
            () -> WorkingDirectory.getInstance("my_folder/documents/readme.txt"));
        assertSame(workingDirectory, WorkingDirectory.getInstance("my_folder"));
        assertEquals(canonical("my_folder"), workingDirectory.getDirectoryName());
    }

    @Test
    @DisplayName("getInstance: повторный вызов возвращает тот же экземпляр")
    void getInstance_ReturnsSameInstance() throws IOException {
        val first = WorkingDirectory.getInstance("my_folder");
        val second = WorkingDirectory.getInstance("my_folder/downloads");
        assertSame(first, second);
    }

    // ==================== toSubdirectory ====================

    @Test
    @DisplayName("toSubdirectory(null): ошибка для пустого ввода")
    void toSubdirectory_Null_ThrowsNPE() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.toSubdirectory(null);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("toSubdirectory(\".\"): остаётся в текущем каталоге, статус OK")
    void toSubdirectory_Dot_StaysInPlace() throws IOException {
        val workingDirectory = fresh();
        val before = workingDirectory.getDirectoryName();
        val currentResult = workingDirectory.toSubdirectory(".");
        assertEquals(OperationResult.Status.OK, currentResult.status());
        assertEquals(before, workingDirectory.getDirectoryName());
    }

    @Test
    @DisplayName("toSubdirectory: ошибка для несуществующего каталога")
    void toSubdirectory_NonExistent_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.toSubdirectory("nope");
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("toSubdirectory: ошибка для файла")
    void toSubdirectory_File_ReturnsError() throws IOException {
        val workingDirectory = WorkingDirectory.getInstance("my_folder/documents");
        val currentResult = workingDirectory.toSubdirectory("readme.txt");
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    // ==================== createNewDirectory ====================

    @Test
    @DisplayName("createNewDirectory(null): ошибка для пустого ввода")
    void createNewDirectory_Null_ThrowsNPE() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.createNewDirectory(null);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("createNewDirectory(\"a/b\"): ERROR — mkdir не создаёт вложенные")
    void createNewDirectory_NestedPath_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.createNewDirectory("a/b");
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
        assertFalse(Files.exists(Paths.get("my_folder/a")));
    }

    @Test
    @DisplayName("createNewDirectory: успешное создание")
    void createNewDirectory_Success() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.createNewDirectory("new_dir");
        assertEquals(OperationResult.Status.OK, currentResult.status());
        assertTrue(Files.isDirectory(Paths.get("my_folder/new_dir")));
    }

    @Test
    @DisplayName("createNewDirectory: ошибка если уже существует")
    void createNewDirectory_AlreadyExists_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.createNewDirectory("documents");
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    // ==================== findFile ====================

    @Test
    @DisplayName("findFile(null): возвращает статус ERROR")
    void findFile_Null_ThrowsNPE() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.findFile(null, false);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("findFile(\"\"): ошибка пустого файла")
    void findFile_Empty_ReturnsAllFiles() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.findFile("", false);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("findFile: регистронезависимый  поиск")
    void findFile_CaseInsensitive() throws IOException {
        val workingDirectory = fresh();
        workingDirectory.findFile("README", false);
        assertEquals(canonical("my_folder/documents/readme.txt"), getOutput());
    }

    @Test
    @DisplayName("findFile: поиск по расширению")
    void findFile_ByExtension() throws IOException {
        val workingDirectory = fresh();
        workingDirectory.findFile(".txt", false);
        assertEquals(canonical("my_folder/documents/readme.txt"), getOutput());
    }

    @Test
    @DisplayName("findFile: только каталоги")
    void findFile_OnlyDirectories() throws IOException {
        val workingDirectory = fresh();
        workingDirectory.findFile("doc", true);
        assertEquals(canonical("my_folder/documents"), getOutput());
    }

    @Test
    @DisplayName("findFile: статус OK")
    void findFile_StatusOk() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.findFile("readme", false);
        assertEquals(OperationResult.Status.OK, currentResult.status());
    }

    // ==================== truncate ====================

    @Test
    @DisplayName("truncate(null): ERROR (NPE перехватывается catch Exception)")
    void truncate_Null_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.truncate(null, false);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("truncate: файл вместо каталога — OK (сообщение вводит в заблуждение)")
    void truncate_FileNotDirectory_ReturnsOk() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.truncate("documents/readme.txt", false);
        assertEquals(OperationResult.Status.OK, currentResult.status());
        assertFalse(Files.exists(Paths.get("my_folder/documents/readme.txt")));
    }

    @Test
    @DisplayName("truncate: каталог с подкаталогами + includeSubdirectories=false → ERROR")
    void truncate_DirWithSubdirectories_IncludeFalse_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.truncate("projects", false);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
        assertTrue(Files.isDirectory(Paths.get("my_folder/projects")));
        assertTrue(Files.isDirectory(Paths.get("my_folder/projects/java")));
    }

    @Test
    @DisplayName("truncate: каталог с подкаталогами + includeSubdirectories=true → OK")
    void truncate_DirWithSubdirectories_IncludeTrue_ReturnsOk() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.truncate("projects", true);
        assertEquals(OperationResult.Status.OK, currentResult.status());
        assertFalse(Files.exists(Paths.get("my_folder/projects")));
    }

    @Test
    @DisplayName("truncate: несуществующий → ERROR")
    void truncate_NonExistent_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.truncate("no_such", true);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    // ==================== isItChildDirectory ====================

    @Test
    @DisplayName("isItChildDirectory(null): ERROR, ничего не печатает")
    void isItChildDirectory_Null_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.isItChildDirectory(null);
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
        assertEquals("", getOutput());
    }

    @Test
    @DisplayName("isItChildDirectory(\"\"): ERROR, ничего не печатает")
    void isItChildDirectory_Empty_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.isItChildDirectory("");
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
        assertEquals("", getOutput());
    }

    @Test
    @DisplayName("isItChildDirectory(\"   \"): ERROR (isBlank)")
    void isItChildDirectory_Blank_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.isItChildDirectory("   ");
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("isItChildDirectory: существует → OK, печатает true")
    void isItChildDirectory_Existing_ReturnsOk() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.isItChildDirectory("documents");
        assertEquals(OperationResult.Status.OK, currentResult.status());
        assertEquals("true", getOutput());
    }

    @Test
    @DisplayName("isItChildDirectory: не существует → ERROR, печатает false")
    void isItChildDirectory_NonExisting_ReturnsError() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.isItChildDirectory("nope");
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
        assertEquals("false", getOutput());
    }

    @Test
    @DisplayName("isItChildDirectory: файл — не дочерний каталог")
    void isItChildDirectory_File_ReturnsError() throws IOException {
        val workingDirectory = WorkingDirectory.getInstance("my_folder/documents");
        val currentResult = workingDirectory.isItChildDirectory("readme.txt");
        assertEquals(OperationResult.Status.ERROR, currentResult.status());
        assertEquals("false", getOutput());
    }

    // ==================== print / drawFileTree: ERROR-ветки ====================

    @Test
    @DisplayName("print: каталог удалён → ERROR")
    void print_AfterDirectoryDeleted_ReturnsError() throws IOException {
        val workingDirectory = WorkingDirectory.getInstance("my_folder/documents");
        deleteRecursively("my_folder/documents");
        val currentResult = workingDirectory.print(false, true);

        assertEquals(OperationResult.Status.ERROR, currentResult.status());
    }

    @Test
    @DisplayName("drawFileTree после удаления каталога → ERROR")
    void drawFileTree_AfterDirectoryDeleted_ReturnsError() throws IOException {
        val workingDirectory = WorkingDirectory.getInstance("my_folder/empty_subfolder");
        Files.delete(Paths.get("my_folder/empty_subfolder"));

        val currentResult = workingDirectory.drawFileTree(null);

        assertEquals(OperationResult.Status.ERROR, currentResult.status());
        assertTrue(
            currentResult.message().contains("не существует"),
            "Ожидалось сообщение про отсутствие пути, получено: " + currentResult.message());
    }

    @Test
    @DisplayName("print: статус OK при существующем каталоге")
    void print_StatusOk() throws IOException {
        val workingDirectory = fresh();
        assertEquals(OperationResult.Status.OK, workingDirectory.print(false, true).status());
    }

    @Test
    @DisplayName("drawFileTree: статус OK при существующем каталоге")
    void drawFileTree_StatusOk() throws IOException {
        val workingDirectory = fresh();
        assertEquals(OperationResult.Status.OK, workingDirectory.drawFileTree(null).status());
    }

    @Test
    @DisplayName("printParentPath: статус OK и корректный абсолютный путь")
    void printParentPath_StatusAndPath() throws IOException {
        val workingDirectory = fresh();
        val currentResult = workingDirectory.printParentPath();

        assertEquals(OperationResult.Status.OK, currentResult.status());
        assertEquals(Paths.get("my_folder").toRealPath().getParent().toString(), getOutput());
    }

    @Test
    @DisplayName("toParentDirectory: статус OK и переход из подкаталога")
    void toParentDirectory_StatusAndPath() throws IOException {
        val workingDirectory = WorkingDirectory.getInstance("my_folder/documents");
        val currentResult = workingDirectory.toParentDirectory();

        assertEquals(OperationResult.Status.OK, currentResult.status());
        assertEquals(canonical("my_folder"), workingDirectory.getDirectoryName());
    }

    @Test
    @DisplayName("printCurrentDirectory: canonical путь с '>'")
    void printCurrentDirectory_PrintsCanonicalPath() throws IOException {
        val workingDirectory = fresh();
        workingDirectory.printCurrentDirectory();
        assertEquals(canonical("my_folder") + ">", getOutput());
    }

    @Test
    @DisplayName("print: только первый уровень — содержимое")
    void print_FirstLevel_Content() throws IOException {
        val workingDirectory = fresh();
        workingDirectory.print(false, true);

        val expected = """
            my_folder
            │  documents
            │  downloads
            │  empty_subfolder
            │  projects""";
        assertEquals(expected, getOutput());
    }

    @Test
    @DisplayName("print: рекурсивный, только каталоги — содержимое")
    void print_Recursive_DirectoriesOnly_Content() throws IOException {
        val workingDirectory = fresh();
        workingDirectory.print(true, false);

        val expected = """
            my_folder
            │  documents
            │  downloads
            │  empty_subfolder
            │  projects
            │  │  java
            │  │  web""";
        assertEquals(expected, getOutput());
    }

    @Test
    @DisplayName("print: рекурсивный, все — содержимое")
    void print_Recursive_All_Content() throws IOException {
        val workingDirectory = fresh();
        workingDirectory.print(false, false);

        val expected = """
            my_folder
            │  documents
            │  │  notes.docx
            │  │  readme.txt
            │  │  report.pdf
            │  downloads
            │  │  archive.zip
            │  │  music.mp3
            │  │  photo.png
            │  empty_subfolder
            │  projects
            │  │  java
            │  │  │  Main.java
            │  │  │  Utils.class
            │  │  web
            │  │  │  index.html
            │  │  │  style.css""";
        assertEquals(expected, getOutput());
    }

    @Test
    @DisplayName("drawFileTree: иерархическое дерево")
    void drawFileTree_Content() throws IOException {
        val workingDirectory = fresh();
        workingDirectory.drawFileTree(null);

        val expected = """
            my_folder
            ├── documents
            │   ├── notes.docx
            │   ├── readme.txt
            │   └── report.pdf
            ├── downloads
            │   ├── archive.zip
            │   ├── music.mp3
            │   └── photo.png
            ├── empty_subfolder
            └── projects
                ├── java
                │   ├── Main.java
                │   └── Utils.class
                └── web
                    ├── index.html
                    └── style.css""";
        assertEquals(expected, getOutput());
    }


    @Test
    @DisplayName("drawFileTree(path): успешное сохранение структуры в текстовый файл")
    void drawFileTree_WithValidPath_SavesToFile() throws IOException {
        val workingDirectory = fresh();
        val targetDir = "my_folder/empty_subfolder";

        val currentResult = workingDirectory.drawFileTree(targetDir);

        assertEquals(OperationResult.Status.OK, currentResult.status());
        val expectedFile = new File(targetDir, "directory_structure.txt");
        assertTrue(expectedFile.exists(), "Файл directory_structure.txt должен быть создан");
        assertTrue(expectedFile.length() > 0, "Файл не должен быть пустым");
    }

    @Test
    @DisplayName("toSubdirectory: успешный переход в дочерний каталог и обратно через '..'")
    void toSubdirectory_SuccessAndParent() throws IOException {
        val workingDirectory = fresh();
        val initial = workingDirectory.getDirectoryName();

        val currentResult = workingDirectory.toSubdirectory("documents");
        assertEquals(OperationResult.Status.OK, currentResult.status());
        assertTrue(workingDirectory.getDirectoryName().endsWith("documents"));

        val sBack = workingDirectory.toSubdirectory("..");
        assertEquals(OperationResult.Status.OK, sBack.status());
        assertEquals(initial, workingDirectory.getDirectoryName());
    }

    @Test
    @DisplayName("printParentPath и toParentDirectory: поведение на уровне корня диска ( ERROR )")
    void parentOperations_AtRoot_ReturnsError() throws IOException {
        // Получаем корневой каталог текущей системы (например, "C:\" или "/")
        val rootFile = File.listRoots()[0];
        val rootWd = WorkingDirectory.getInstance(rootFile.getAbsolutePath());

        val printStatus = rootWd.printParentPath();
        assertEquals(OperationResult.Status.ERROR, printStatus.status());

        val jumpStatus = rootWd.toParentDirectory();
        assertEquals(OperationResult.Status.ERROR, jumpStatus.status());
    }
}