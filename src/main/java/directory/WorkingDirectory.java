package directory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Класс, реализующий управление рабочим каталогом в соответствии с паттерном проектирования «Одиночка» (Singleton).
 * Обеспечивает навигацию по файловой системе, просмотр содержимого, создание и удаление директорий,
 * а также поиск файлов и построение дерева каталогов.
 */
@Getter
@Slf4j
public class WorkingDirectory {
    private static WorkingDirectory instance;
    private String directoryName;

    /**
     * Приватный конструктор для реализации шаблона Singleton.
     * Инициализирует рабочий каталог по заданному пути, приводя его к каноническому виду.
     *
     * @param directoryName путь к каталогу
     * @throws IOException если путь некорректен
     */
    private WorkingDirectory(String directoryName) throws IOException {
        this.directoryName = new File(directoryName).getCanonicalPath();
        log.info("Инициализирован рабочий каталог Singleton: {}", this.directoryName);
    }

    /**
     * Возвращает единственный экземпляр класса {@link WorkingDirectory}.
     * Если экземпляр еще не создан, проверяет существование и тип переданного пути.
     *
     * @param directoryName путь к рабочему каталогу
     * @return единственный экземпляр {@link WorkingDirectory}
     * @throws IOException если каталог не существует или путь не является директорией
     */
    public static WorkingDirectory getInstance(final String directoryName) throws IOException {
        val dir = new File(directoryName);
        if (!dir.exists()) {
            val errorMsg = "Каталога не существует: " + directoryName;
            log.error("Внештатная ситуация при getInstance: {}", errorMsg);
            throw new IOException(errorMsg);
        }
        if (!dir.isDirectory()) {
            val errorMsg = "Путь не является каталогом: " + directoryName;
            log.error("Внештатная ситуация при getInstance: {}", errorMsg);
            throw new IOException(errorMsg);
        }
        if (instance == null) {
            instance = new WorkingDirectory(directoryName);
        } else {
            instance.directoryName = dir.getCanonicalPath();
            log.info("Экземпляр Singleton обновлен на каталог: {}", instance.directoryName);
        }
        return instance;
    }

    /**
     * Выводит в стандартный поток вывода приглашение командной строки с текущим путем (например, `C:\Path>`).
     *
     * @throws IOException если не удалось получить канонический путь
     */
    public void printCurrentDirectory() throws IOException {
        System.out.print(new File(directoryName).getCanonicalPath() + ">");
    }

    /**
     * Выводит содержимое текущего рабочего каталога с учетом флагов фильтрации и глубины.
     *
     * @param directoriesOnly если true, выводить только директории
     * @param firstLevelOnly  если true, выполнять поиск только на первом уровне вложений
     * @return объект {@link OperationResult} со статусом выполнения операции
     */
    public OperationResult print(final boolean directoriesOnly, final boolean firstLevelOnly) {
        try {
            val dir = new File(directoryName);
            printRecursive(dir, "", directoriesOnly, firstLevelOnly);
            val msg = "Выведено содержимое каталога: " + directoryName;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (Exception thrown) {
            val errorMsg = "Ошибка при выводе содержимого: " + thrown.getMessage();
            log.error("Внештатная ситуация: {}", errorMsg, thrown);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Рекурсивный вспомогательный метод для обхода и вывода содержимого каталога.
     *
     * @param dir             текущий файл или директория
     * @param prefix          текстовый префикс для визуального оформления дерева
     * @param directoriesOnly флаг вывода только директорий
     * @param firstLevelOnly  флаг ограничения глубины первым уровнем
     * @throws IOException если путь не существует
     */
    private void printRecursive(
        final File dir,
        final String prefix,
        final boolean directoriesOnly,
        final boolean firstLevelOnly) throws IOException {

        if (!dir.exists()) {
            throw new IOException("Путь не существует: " + dir.getAbsolutePath());
        }
        File[] filesInDir = dir.listFiles();
        if (!(directoriesOnly && dir.isFile())) {
            System.out.println(prefix + dir.getName());
        }
        if (filesInDir != null) {
            for (var currentFile : filesInDir) {
                if (firstLevelOnly) {
                    if (directoriesOnly) {
                        if (currentFile.isDirectory()) {
                            System.out.println(prefix + "│  " + currentFile.getName());
                        }
                    } else {
                        System.out.println(prefix + "│  " + currentFile.getName());
                    }
                } else {
                    printRecursive(currentFile, prefix + "│  ", directoriesOnly, false);
                }
            }
        }
    }

    /**
     * Отрисовывает иерархическое дерево файлов и папок.
     * Если передан путь, сохраняет структуру в файл `directory_structure.txt` по указанному пути,
     * иначе выводит результат в консоль.
     *
     * @param path директория для сохранения файла структуры (может быть null)
     * @return объект {@link OperationResult} со статусом выполнения операции
     */
    public OperationResult drawFileTree(final String path) {
        try {
            val dir = new File(directoryName);
            if (!dir.exists()) {
                val errorMsg = "Невозможно отрисовать дерево, путь " + directoryName + " не существует";
                log.error("Внештатная ситуация: {}", errorMsg);
                return new OperationResult(OperationResult.Status.ERROR, errorMsg);
            }
            if (StringUtils.isNotBlank(path)) {
                val outputFile = new File(path, "directory_structure.txt");
                val parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                try (val fileOut = new PrintStream(
                    new FileOutputStream(outputFile),
                    true,
                    StandardCharsets.UTF_8)) {

                    fileOut.println(dir.getName());
                    drawFileTreeRecursive(dir, "", fileOut);
                } catch (IOException thrown) {
                    val errorMsg = "Не удалось записать дерево в файл: " + thrown.getMessage();
                    log.error("Внештатная ситуация: {}", errorMsg, thrown);
                    return new OperationResult(OperationResult.Status.ERROR, errorMsg);
                }
            } else {
                System.out.println(dir.getName());
                drawFileTreeRecursive(dir, "", System.out);
            }
            val msg = "Дерево каталогов успешно отрисовано для: " + directoryName;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (Exception thrown) {
            val errorMsg = "Не удалось отрисовать дерево: " + thrown.getMessage();
            log.error("Внештатная ситуация: {}", errorMsg, thrown);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Рекурсивный вспомогательный метод для генерации текстового представления дерева файлов.
     *
     * @param dir    текущая директория
     * @param prefix текущий отступ для ветвления
     * @param ps     поток вывода (в файл или консоль)
     */
    private void drawFileTreeRecursive(final File dir, final String prefix, final PrintStream ps) {
        File[] listFiles = dir.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            val length = listFiles.length;
            for (int index = 0; index < length - 1; index++) {
                ps.println(prefix + "├── " + listFiles[index].getName());
                if (listFiles[index].isDirectory()) {
                    drawFileTreeRecursive(listFiles[index], prefix + "│   ", ps);
                }
            }
            val lastFile = listFiles[length - 1];
            ps.println(prefix + "└── " + lastFile.getName());
            if (lastFile.isDirectory()) {
                drawFileTreeRecursive(lastFile, prefix + "    ", ps);
            }
        }
    }

    /**
     * Выводит в консоль абсолютный путь родительского каталога текущей рабочей директории.
     *
     * @return объект {@link OperationResult} со статусом выполнения
     */
    public OperationResult printParentPath() {
        val dir = new File(directoryName);
        val parent = dir.getParentFile();
        if (parent != null) {
            System.out.println(parent.getAbsolutePath());
            val msg = "Родительский каталог: " + parent.getAbsolutePath();
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } else {
            val errorMsg = "Родительский каталог отсутствует (текущий каталог — корень)";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Выполняет переход к родительскому каталогу, изменяя значение рабочего каталога.
     *
     * @return объект {@link OperationResult} со статусом выполнения
     */
    public OperationResult toParentDirectory() {
        val dir = new File(directoryName);
        val parent = dir.getParentFile();
        if (parent != null) {
            this.directoryName = parent.getAbsolutePath();
            val msg = "Выполнен переход в родительский каталог: " + directoryName;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } else {
            val errorMsg = "Невозможно перейти к родителю — родительский каталог отсутствует";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Проверяет, существует ли дочерний каталог с заданным именем в текущем рабочем каталоге.
     *
     * @param targetName имя проверяемого каталога
     * @return объект {@link OperationResult} со статусом выполнения
     */
    public OperationResult isItChildDirectory(final String targetName) {
        if (StringUtils.isBlank(targetName)) {
            val errorMsg = "Имя искомой директории не может быть пустым";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        val dir = new File(directoryName);
        val files = dir.listFiles();
        if (files != null) {
            for (val file : files) {
                if (file.getName().equals(targetName) && file.isDirectory()) {
                    System.out.println("true");
                    val msg = "Подкаталог '" + targetName + "' существует в текущем каталоге";
                    log.info("Штатная ситуация: {}", msg);
                    return new OperationResult(OperationResult.Status.OK, msg);
                }
            }
        }
        System.out.println("false");
        val errorMsg = "Дочерняя папка '" + targetName + "' не найдена";
        log.warn("Внештатная ситуация: {}", errorMsg);
        return new OperationResult(OperationResult.Status.ERROR, errorMsg);
    }

    /**
     * Выполняет переход в подкаталог по его имени или возврат к родителю при передаче "..".
     *
     * @param name имя подкаталога или ".."
     * @return объект {@link OperationResult} со статусом выполнения
     */
    public OperationResult toSubdirectory(final String name) {
        val dir = new File(directoryName);
        if (!dir.exists()) {
            val errorMsg = "Родительский каталог не существует: " + directoryName;
            log.error("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        if (StringUtils.isBlank(name)) {
            val errorMsg = "Имя подкаталога не может быть пустым";
            log.error("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        if ("..".equals(name)) {
            return toParentDirectory();
        }
        val subFolder = new File(dir, name);
        if (!subFolder.exists() || !subFolder.isDirectory()) {
            val errorMsg = "Невозможно перейти в подкаталог, каталога не существует: '" + name + "'";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        try {
            this.directoryName = subFolder.getCanonicalPath();
            val msg = "Успешный переход в подкаталог: " + name;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (IOException thrown) {
            val errorMsg = "Ошибка при вычислении canonical path для " + name;
            log.error("Внештатная ситуация: {}", errorMsg, thrown);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Создает новый каталог с указанным именем внутри текущего рабочего каталога.
     *
     * @param name имя создаваемой папки
     * @return объект {@link OperationResult} со статусом выполнения
     */
    public OperationResult createNewDirectory(final String name) {
        val dir = new File(directoryName);
        if (StringUtils.isBlank(name)) {
            val errorMsg = "Имя подкаталога не может быть пустым";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        val subFolder = new File(dir, name);
        if (!dir.exists()) {
            val errorMsg = "Невозможно создать подкаталог, родительский каталог не существует: " + directoryName;
            log.error("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        if (subFolder.exists()) {
            val errorMsg = "Подкаталог '" + name + "' уже существует";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        if (!subFolder.mkdir()) {
            val errorMsg = "Не удалось создать подкаталог: " + name;
            log.error("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        val successMsg = "Подкаталог успешно создан: " + subFolder.getAbsolutePath();
        System.out.println("Подкаталог успешно создан: " + subFolder.getName());
        log.info("Штатная ситуация: {}", successMsg);
        return new OperationResult(OperationResult.Status.OK, successMsg);
    }

    /**
     * Выполняет поиск файлов или папок по маске/имени в текущем каталоге и всех подкаталогах.
     *
     * @param filename        имя файла или расширение для поиска
     * @param onlyDirectories если true, искать только директории
     * @return объект {@link OperationResult} со статусом выполнения
     */
    public OperationResult findFile(final String filename, final boolean onlyDirectories) {
        if (StringUtils.isBlank(filename)) {
            val errorMsg = "Имя искомого файла или каталога не может быть пустым";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        try (val stream = Files.walk(Paths.get(directoryName))) {
            if (!onlyDirectories) {
                stream.filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .filter(path -> {
                        val name = path.getFileName().toString().toLowerCase();
                        val search = filename.toLowerCase();
                        return search.startsWith(".") ? name.endsWith(search) : name.contains(search);
                    })
                    .map(Path::toString)
                    .forEach(System.out::println);
            } else {
                stream.filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .filter(path -> {
                        val name = path.getFileName().toString().toLowerCase();
                        val search = filename.toLowerCase();
                        return name.contains(search);
                    })
                    .map(Path::toString)
                    .forEach(System.out::println);
            }
            val msg = "Поиск '" + filename + "' успешно выполнен";
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (IOException thrown) {
            val errorMsg = "Ошибка при поиске файла '" + filename + "': " + thrown.getMessage();
            log.error("Внештатная ситуация: {}", errorMsg, thrown);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Удаляет файл или подкаталог в рабочем каталоге.
     * При необходимости поддерживает рекурсивное удаление содержимого подкаталогов.
     *
     * @param filename              имя удаляемого файла или каталога
     * @param includeSubdirectories если true, удалять каталог вместе со всем содержимым
     * @return объект {@link OperationResult} со статусом выполнения
     */
    public OperationResult truncate(final String filename, final boolean includeSubdirectories) {
        if (StringUtils.isBlank(filename)) {
            val errorMsg = "Имя файла или каталога для удаления не может быть пустым";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        try {
            val file = new File(directoryName, filename);
            if (!file.exists()) {
                val errorMsg = "Не удалось удалить, " + filename + " не существует.";
                log.warn("Внештатная ситуация: {}", errorMsg);
                return new OperationResult(OperationResult.Status.ERROR, errorMsg);
            }
            val deletedContents = deleteSb(file, includeSubdirectories);
            val deletedTarget = file.delete();
            val success = deletedContents && deletedTarget;
            if (!success) {
                val errorMsg = "Не удалось полностью удалить: " + filename;
                log.warn("Внештатная ситуация: {}", errorMsg);
                return new OperationResult(OperationResult.Status.ERROR, errorMsg);
            }
            val msg = "Успешно удалено: " + filename;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (Exception thrown) {
            val errorMsg = "Ошибка при выполнении операции truncate: " + thrown.getMessage();
            log.error("Внештатная ситуация: {}", errorMsg, thrown);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Рекурсивный вспомогательный метод для очистки содержимого каталога перед удалением.
     *
     * @param dir                   каталог для очистки
     * @param includeSubdirectories флаг разрешения рекурсивного удаления вложенных папок
     * @return true, если удаление прошло успешно
     */
    private boolean deleteSb(final File dir, final boolean includeSubdirectories) {
        val files = dir.listFiles();
        if (files == null) {
            return true;
        }
        boolean result = true;
        for (val file : files) {
            if (!file.isDirectory()) {
                val deletedFile = file.delete();
                result = deletedFile && result;
            } else if (includeSubdirectories) {
                val deletedSubtree = deleteSb(file, true);
                val deletedSubdirectory = file.delete();
                result = deletedSubtree && deletedSubdirectory && result;
            }
        }
        return result;
    }
}