package directory;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Класс, реализующий управление рабочим каталогом в соответствии с паттерном проектирования «Одиночка» (Singleton).
 * Обеспечивает навигацию по файловой системе, просмотр содержимого, создание и удаление директорий,
 * а также поиск файлов и построение дерева каталогов.
 */
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
    public static WorkingDirectory getInstance(String directoryName) throws IOException {
        File dir = new File(directoryName);
        if (!dir.exists()) {
            String errorMsg = "Каталога не существует: " + directoryName;
            log.error("Внештатная ситуация при getInstance: {}", errorMsg);
            throw new IOException(errorMsg);
        }
        if (!dir.isDirectory()) {
            String errorMsg = "Путь не является каталогом: " + directoryName;
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
     * Возвращает абсолютный путь текущего рабочего каталога.
     *
     * @return строка с путем к каталогу
     */
    public String getDirectoryName() {
        return directoryName;
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
     * @param directoriesOnly        если true, выводить только директории
     * @param firstLevelOnly если true, выполнять поиск только на первом уровне вложений
     * @return объект {@link OperationResult} со статусом выполнения операции
     */
    public OperationResult print(boolean directoriesOnly, boolean firstLevelOnly) {
        try {
            File dir = new File(directoryName);
            printRecursive(dir, "", directoriesOnly, firstLevelOnly);
            String msg = "Выведено содержимое каталога: " + directoryName;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (Exception e) {
            String errorMsg = "Ошибка при выводе содержимого: " + e.getMessage();
            log.error("Внештатная ситуация: {}", errorMsg, e);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Рекурсивный вспомогательный метод для обхода и вывода содержимого каталога.
     *
     * @param dir                      текущий файл или директория
     * @param prefix                   текстовый префикс для визуального оформления дерева
     * @param directoriesOnly        флаг вывода только директорий
     * @param firstLevelOnly флаг ограничения глубины первым уровнем
     * @throws IOException если путь не существует
     */
    private void printRecursive(
        File dir,
        String prefix,
        boolean directoriesOnly,
        boolean firstLevelOnly) throws IOException {
        if (!dir.exists()) {
            throw new IOException("Путь не существует: " + dir.getAbsolutePath());
        }
        File[] filesInDir = dir.listFiles();
        if (!(directoriesOnly && dir.isFile())) {
            System.out.println(prefix + dir.getName());
        }
        if (filesInDir != null) {
            for (File currentFile : filesInDir) {
                if (firstLevelOnly) {
                    if (directoriesOnly) {
                        if (currentFile.isDirectory()) System.out.println(prefix + "│  " + currentFile.getName());
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
    public OperationResult drawFileTree(String path) {
        try {
            File dir = new File(directoryName);
            if (!dir.exists()) {
                String errorMsg = "Невозможно отрисовать дерево, путь " + directoryName + " не существует";
                log.error("Внештатная ситуация: {}", errorMsg);
                return new OperationResult(OperationResult.Status.ERROR, errorMsg);
            }

            if (path != null && !path.isBlank()) {
                File outputFile = new File(path, "directory_structure.txt");
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try (PrintStream fileOut = new PrintStream(
                    new FileOutputStream(outputFile),
                    true,
                    StandardCharsets.UTF_8)) {
                    fileOut.println(dir.getName());
                    drawFileTreeRecursive(dir, "", fileOut);
                } catch (IOException e) {
                    String errorMsg = "Не удалось записать дерево в файл: " + e.getMessage();
                    log.error("Внештатная ситуация: {}", errorMsg, e);
                    return new OperationResult(OperationResult.Status.ERROR, errorMsg);
                }
            } else {
                System.out.println(dir.getName());
                drawFileTreeRecursive(dir, "", System.out);
            }

            String msg = "Дерево каталогов успешно отрисовано для: " + directoryName;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (Exception e) {
            String errorMsg = "Не удалось отрисовать дерево: " + e.getMessage();
            log.error("Внештатная ситуация: {}", errorMsg, e);
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
    private void drawFileTreeRecursive(File dir, String prefix, PrintStream ps) {
        File[] listFiles = dir.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            int length = listFiles.length;
            for (int i = 0; i < length - 1; i++) {
                ps.println(prefix + "├── " + listFiles[i].getName());
                if (listFiles[i].isDirectory()) {
                    drawFileTreeRecursive(listFiles[i], prefix + "│   ", ps);
                }
            }
            File lastFile = listFiles[length - 1];
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
        File dir = new File(directoryName);
        File parent = dir.getParentFile();
        if (parent != null) {
            System.out.println(parent.getAbsolutePath());
            String msg = "Родительский каталог: " + parent.getAbsolutePath();
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } else {
            String errorMsg = "Родительский каталог отсутствует (текущий каталог — корень)";
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
        File dir = new File(directoryName);
        File parent = dir.getParentFile();
        if (parent != null) {
            this.directoryName = parent.getAbsolutePath();
            String msg = "Выполнен переход в родительский каталог: " + directoryName;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } else {
            String errorMsg = "Невозможно перейти к родителю — родительский каталог отсутствует";
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
    public OperationResult isItChildDirectory(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            String errorMsg = "Имя искомой директории не может быть пустым";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        File dir = new File(directoryName);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(targetName) && file.isDirectory()) {
                    System.out.println("true");
                    String msg = "Подкаталог '" + targetName + "' существует в текущем каталоге";
                    log.info("Штатная ситуация: {}", msg);
                    return new OperationResult(OperationResult.Status.OK, msg);
                }
            }
        }
        System.out.println("false");
        String errorMsg = "Дочерняя папка '" + targetName + "' не найдена";
        log.warn("Внештатная ситуация: {}", errorMsg);
        return new OperationResult(OperationResult.Status.ERROR, errorMsg);
    }

    /**
     * Выполняет переход в подкаталог по его имени или возврат к родителю при передаче "..".
     *
     * @param name имя подкаталога или ".."
     * @return объект {@link OperationResult} со статусом выполнения
     */
    public OperationResult toSubdirectory(String name) {
        File dir = new File(directoryName);
        if (!dir.exists()) {
            String errorMsg = "Родительский каталог не существует: " + directoryName;
            log.error("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        if ("..".equals(name)) {
            return toParentDirectory();
        }

        File subFolder = new File(dir, name);
        if (!subFolder.exists() || !subFolder.isDirectory()) {
            String errorMsg = "Невозможно перейти в подкаталог, каталога не существует: '" + name + "'";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }

        try {
            this.directoryName = subFolder.getCanonicalPath();
            String msg = "Успешный переход в подкаталог: " + name;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (IOException e) {
            String errorMsg = "Ошибка при вычислении canonical path для " + name;
            log.error("Внештатная ситуация: {}", errorMsg, e);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Создает новый каталог с указанным именем внутри текущего рабочего каталога.
     *
     * @param name имя создаваемой папки
     * @return объект {@link OperationResult} со статусом выполнения
     */
    public OperationResult createNewDirectory(String name) {
        File dir = new File(directoryName);
        File subFolder = new File(dir, name);

        if (!dir.exists()) {
            String errorMsg = "Невозможно создать подкаталог, родительский каталог не существует: " + directoryName;
            log.error("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        if (subFolder.exists()) {
            String errorMsg = "Подкаталог '" + name + "' уже существует";
            log.warn("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
        if (!subFolder.mkdir()) {
            String errorMsg = "Не удалось создать подкаталог: " + name;
            log.error("Внештатная ситуация: {}", errorMsg);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }

        String successMsg = "Подкаталог успешно создан: " + subFolder.getAbsolutePath();
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
    public OperationResult findFile(String filename, boolean onlyDirectories) {
        try (Stream<Path> stream = Files.walk(Paths.get(directoryName))) {
            if (!onlyDirectories) {
                stream.filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        String search = filename.toLowerCase();
                        return search.startsWith(".") ? name.endsWith(search) : name.contains(search);
                    })
                    .map(Path::toString)
                    .forEach(System.out::println);
            } else {
                stream.filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        String search = filename.toLowerCase();
                        return name.contains(search);
                    })
                    .map(Path::toString)
                    .forEach(System.out::println);
            }
            String msg = "Поиск '" + filename + "' успешно выполнен";
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (IOException e) {
            String errorMsg = "Ошибка при поиске файла '" + filename + "': " + e.getMessage();
            log.error("Внештатная ситуация: {}", errorMsg, e);
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
    public OperationResult truncate(String filename, boolean includeSubdirectories) {
        try {
            File file = new File(directoryName, filename);
            if (!file.exists()) {
                String errorMsg = "Не удалось удалить, " + filename + " не существует.";
                log.warn("Внештатная ситуация: {}", errorMsg);
                return new OperationResult(OperationResult.Status.ERROR, errorMsg);
            }
            boolean success = deleteSb(file, includeSubdirectories);
            success = file.delete() && success;
            if (!success) {
                String errorMsg = "Не удалось полностью удалить: " + filename;
                log.warn("Внештатная ситуация: {}", errorMsg);
                return new OperationResult(OperationResult.Status.ERROR, errorMsg);
            }
            String msg = "Успешно удалено: " + filename;
            log.info("Штатная ситуация: {}", msg);
            return new OperationResult(OperationResult.Status.OK, msg);
        } catch (Exception e) {
            String errorMsg = "Ошибка при выполнении операции truncate: " + e.getMessage();
            log.error("Внештатная ситуация: {}", errorMsg, e);
            return new OperationResult(OperationResult.Status.ERROR, errorMsg);
        }
    }

    /**
     * Рекурсивный вспомогательный метод для очистки содержимого каталога перед удалением.
     *
     * @param dir        каталог для очистки
     * @param includeSubdirectories флаг разрешения рекурсивного удаления вложенных папок
     * @return true, если удаление прошло успешно
     */
    private boolean deleteSb(File dir, boolean includeSubdirectories) {
        File[] files = dir.listFiles();
        if (files == null) return true;
        boolean result = true;
        for (File f : files) {
            if (!f.isDirectory()) {
                result = f.delete() && result;
            } else if (includeSubdirectories) {
                result = deleteSb(f, includeSubdirectories) && f.delete() && result;
            }
        }
        return result;
    }
}