package cli;

import directory.OperationResult;
import directory.WorkingDirectory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

@Slf4j
public class ConsoleApplication {
    public static void initiateStartDirectory() {
        val initialDir = new File("my_folder");
        if (!initialDir.exists()) {
            val created = initialDir.mkdir();
            if (created) {
                log.info("Создан стартовый каталог: {}", initialDir.getAbsolutePath());
            } else {
                log.error("Не удалось создать стартовый каталог: {}", initialDir.getAbsolutePath());
            }
        }
    }

    public static void run() {
        log.info("Запуск консольного приложения 'WorkingDirectory'");

        val scan = new Scanner(System.in);
        initiateStartDirectory();
        WorkingDirectory wd;
        try {
            // Инициализация рабочего каталога
            wd = WorkingDirectory.getInstance("my_folder");

            boolean isRunning = true;
            while (isRunning) {
                try {
                    wd.printCurrentDirectory();
                    val input = scan.nextLine().strip();
                    if (input.isEmpty()) {
                        continue;
                    }

                    val words = input.split("\\s+");
                    if (words.length > 0) {
                        val command = words[0];

                        switch (command) {
                            case "cd":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    log.warn("Внештатная ситуация: команда 'cd' вызвана без аргументов");
                                } else {
                                    val status = wd.toSubdirectory(words[words.length - 1]);
                                    handleStatus(status);
                                }
                                break;

                            case "istChildSbd":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    log.warn("Внештатная ситуация: команда 'istChildSbd' вызвана без аргументов");
                                } else {
                                    val status = wd.isItChildDirectory(words[words.length - 1]);
                                    handleStatus(status);
                                }
                                break;

                            case "parPath":
                                val parStatus = wd.printParentPath();
                                handleStatus(parStatus);
                                break;

                            case "dir":
                                val onlyDirectoryFlag = Arrays.asList(words).contains("-d");
                                val onlyFirstLevelSearchFlag = !Arrays.asList(words).contains("-all");
                                val dirStatus = wd.print(onlyDirectoryFlag, onlyFirstLevelSearchFlag);
                                handleStatus(dirStatus);
                                break;

                            case "tree":
                                String targetPath = null;
                                for (int i = 1; i < words.length; i++) {
                                    if ("-o".equals(words[i]) && i + 1 < words.length) {
                                        targetPath = words[i + 1];
                                        break;
                                    }
                                }
                                val treeStatus = wd.drawFileTree(targetPath);
                                handleStatus(treeStatus);
                                break;

                            case "mkdir":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    log.warn("Внештатная ситуация: команда 'mkdir' вызвана без аргументов");
                                } else {
                                    val status = wd.createNewDirectory(words[words.length - 1]);
                                    handleStatus(status);
                                }
                                break;

                            case "find":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    log.warn("Внештатная ситуация: команда 'find' вызвана без аргументов");
                                } else {
                                    val status = wd.findFile(
                                        words[words.length - 1],
                                        Arrays.asList(words).contains("-d"));
                                    handleStatus(status);
                                }
                                break;

                            case "truncate":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    log.warn("Внештатная ситуация: команда 'truncate' вызвана без аргументов");
                                } else if (Arrays.asList(words).contains("-d") && words.length >= 3) {
                                    val truncStatus = wd.truncate(words[words.length - 1], true);
                                    handleStatus(truncStatus);
                                } else {
                                    val truncStatus = wd.truncate(words[words.length - 1], false);
                                    handleStatus(truncStatus);
                                }
                                break;

                            case "help":
                                printHelp();
                                log.info("Штатная ситуация: выведена справка по командам");
                                break;

                            case "exit":
                                isRunning = false;
                                log.info("Штатная ситуация: завершение работы программы пользователем");
                                break;

                            default:
                                System.out.println("Неизвестная команда. Введите 'help' для справки.");
                                log.warn("Внештатная ситуация: введена неизвестная команда -> '{}'", command);
                                break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка: " + e.getMessage());
                    log.error("Внештатная ситуация: необработанное исключение во время выполнения команды", e);
                }
            }
        } catch (Exception thrown) {
            System.out.println("Ошибка инициализации: " + thrown.getMessage());
            log.error("Критическая ошибка при инициализации WorkingDirectory", thrown);
        } finally {
            scan.close();
            log.info("Программа завершила свою работу");
        }
    }

    private static void handleStatus(final OperationResult status) {
        if (status.status() == OperationResult.Status.OK) {
            log.info("Штатная ситуация: {}", status.message());
        } else {
            System.out.println("Ошибка: " + status.message());
            log.warn("Внештатная ситуация: {}", status.message());
        }
    }

    private static void printHelp() {
        System.out.println("""
        Доступные команды:
          cd <имя_папки>              - перейти в подпапку (или '..' для перехода в родительскую)
          parPath                     - вывести абсолютный путь родительской папки
          istChildSbd <имя_папки>     - проверить, является ли папка дочерней (выводит true/false)
          dir [-d] [-all]             - вывести содержимое текущего каталога
            [-d]       показывать только директории
            [-all]     включать содержимое всех вложенных папок
          tree [путь | -o <каталог>] - построить дерево (в консоль или в directory_structure.txt)
            [путь | -o <каталог>] каталог, куда сохранить directory_structure.txt
          mkdir <имя>                 - создать новую папку
          find <имя_или_расширение> [-d] - найти файл или папку (подстрока или '.ext')
            [-d]       искать только директории
          truncate [-d] <имя>         - удалить файл или папку
            [-d]       удалить вложенные директории рекурсивно
          help                        - показать эту справку
          exit                        - выйти из программы""");
    }
}
