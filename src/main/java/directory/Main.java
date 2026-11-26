package directory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Запуск консольного приложения 'WorkingDirectory'");

        Scanner scan = new Scanner(System.in);
        File initialDir = new File("my_folder");
        if (!initialDir.exists()) {
            boolean created = initialDir.mkdir();
            if (created) {
                logger.info("Создан стартовый каталог: {}", initialDir.getAbsolutePath());
            } else {
                logger.error("Не удалось создать стартовый каталог: {}", initialDir.getAbsolutePath());
            }
        }

        WorkingDirectory wd;
        try {
            // Инициализация рабочего каталога
            wd = WorkingDirectory.getInstance("my_folder");

            boolean programToggler = true;
            while (programToggler) {
                try {
                    wd.printCurrentDirectory();
                    String input = scan.nextLine().strip();
                    if (input.isEmpty()) continue;

                    String[] words = input.split("\\s+");
                    if (words.length > 0) {
                        String command = words[0];

                        switch (command) {
                            case "cd":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    logger.warn("Внештатная ситуация: команда 'cd' вызвана без аргументов");
                                } else {
                                    OperationStatus status = wd.toSubdirectory(words[words.length - 1]);
                                    handleStatus(status);
                                }
                                break;

                            case "istChildSbd":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    logger.warn("Внештатная ситуация: команда 'istChildSbd' вызвана без аргументов");
                                } else {
                                    OperationStatus status = wd.isItChildDirectory(words[words.length - 1]);
                                    handleStatus(status);
                                }
                                break;

                            case "parPath":
                                OperationStatus parStatus = wd.printParentPath();
                                handleStatus(parStatus);
                                break;

                            case "dir":
                                boolean onlyDirectoryFlag = Arrays.asList(words).contains("-d");
                                boolean onlyFirstLevelSearchFlag = !Arrays.asList(words).contains("-all");
                                OperationStatus dirStatus = wd.print(onlyDirectoryFlag, onlyFirstLevelSearchFlag);
                                handleStatus(dirStatus);
                                break;

                            case "tree":
                                String targetPath = null;
                                for (int i = 1; i < words.length; i++) {
                                    if (("-o".equals(words[i]) || "--output".equals(words[i])) && i + 1 < words.length) {
                                        targetPath = words[i + 1];
                                        break;
                                    }
                                }
                                if (targetPath == null && words.length > 1 && !words[1].startsWith("-")) {
                                    targetPath = words[1];
                                }

                                OperationStatus treeStatus = wd.drawFileTree(targetPath);
                                handleStatus(treeStatus);
                                break;

                            case "mkdir":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    logger.warn("Внештатная ситуация: команда 'mkdir' вызвана без аргументов");
                                } else {
                                    OperationStatus status = wd.createNewDirectory(words[words.length - 1]);
                                    handleStatus(status);
                                }
                                break;

                            case "find":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    logger.warn("Внештатная ситуация: команда 'find' вызвана без аргументов");
                                } else {
                                    OperationStatus status = wd.findFile(words[words.length - 1], Arrays.asList(words).contains("-d"));
                                    handleStatus(status);
                                }
                                break;

                            case "truncate":
                                if (words.length < 2) {
                                    System.out.println("Недостаточно аргументов");
                                    logger.warn("Внештатная ситуация: команда 'truncate' вызвана без аргументов");
                                } else if (Arrays.asList(words).contains("-d") && words.length >= 3) {
                                    OperationStatus truncStatus = wd.truncate(words[words.length - 1], true);
                                    handleStatus(truncStatus);
                                }
                                else {
                                    OperationStatus truncStatus = wd.truncate(words[words.length - 1], false);
                                    handleStatus(truncStatus);
                                }
                                break;

                            case "help":
                                printHelp();
                                logger.info("Штатная ситуация: выведена справка по командам");
                                break;

                            case "exit":
                                programToggler = false;
                                logger.info("Штатная ситуация: завершение работы программы пользователем");
                                break;

                            default:
                                System.out.println("Неизвестная команда. Введите 'help' для справки.");
                                logger.warn("Внештатная ситуация: введена неизвестная команда -> '{}'", command);
                                break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка: " + e.getMessage());
                    logger.error("Внештатная ситуация: необработанное исключение во время выполнения команды", e);
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка инициализации: " + e.getMessage());
            logger.error("Критическая ошибка при инициализации WorkingDirectory", e);
        } finally {
            scan.close();
            logger.info("Программа завершила свою работу");
        }
    }

    private static void handleStatus(OperationStatus status) {
        if (status.status() == OperationStatus.Status.OK) {
            logger.info("Штатная ситуация: {}", status.message());
        } else {
            System.out.println("Ошибка: " + status.message());
            logger.warn("Внештатная ситуация: {}", status.message());
        }
    }

    private static void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  cd <имя_папки>            - перейти в подпапку (или '..' для перехода в родительскую)");
        System.out.println("  parPath                   - вывести абсолютный путь родительской папки");
        System.out.println("  istChildSbd <имя_папки>   - проверить, является ли указанная папка дочерней для текущей");
        System.out.println("  dir [-d] [-all]           - вывести содержимое папки");
        System.out.println("    [-d]     показывать только директории");
        System.out.println("    [-all]   включая содержимое всех вложенных папок");
        System.out.println("  tree [путь | -o|--output <файл>] - нарисовать дерево файлов и папок");
        System.out.println("    [путь]   путь для построения дерева");
        System.out.println("    [-o|--output <файл>] сохранить дерево в указанный файл");
        System.out.println("  mkdir <имя>               - создать новую папку");
        System.out.println("  find <имя> [-d]           - найти файл или папку");
        System.out.println("    [-d]     искать только директории");
        System.out.println("  truncate [-d] <имя_папки_файла> - удалить папку");
        System.out.println("    [-d]     удалить рекурсивно (включая вложенные папки и файлы)");
        System.out.println("  help                      - показать эту справку");
        System.out.println("  exit                      - выйти из программы");
    }
}
