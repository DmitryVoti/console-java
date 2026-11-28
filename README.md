# console-java

Интерактивный консольный файловый менеджер на Java (учебный проект — Лабораторная работа №2). Управление рабочим каталогом построено вокруг класса `WorkingDirectory` (паттерн *Singleton*), а работа с файловой системой, поиск, построение дерева и удаление оформлены через единый результат операции `OperationResult`.

## Стек и зависимости

* **Язык:** Java 26 (`maven.compiler.release = 26`)

* **Сборка и тесты:** Apache Maven, JUnit Jupiter (`5.14.4`), Maven Surefire Plugin (`3.2.5`)

* **Библиотеки:** Project Lombok (`1.18.48`), Apache Commons Lang (`3.17.0`), SLF4J (`2.0.16`) + Logback Classic (`1.6.6`)

* **Стиль и окружение:** `.editorconfig`, `lombok.config`

## Сборка и запуск

Клонирование и сборка проекта:
```bash
git clone [https://github.com/DmitryVoti/console-java.git](https://github.com/DmitryVoti/console-java.git)
cd console-java
mvn clean package
```

Запуск тестов:
```bash
mvn test
```

Запуск консольного приложения (при первом запуске автоматически создаётся стартовый каталог `my_folder` в текущей директории):

```bash
java -cp target/classes Main
```

## Команды CLI

| Команда | Описание |
| --- | --- |
| `cd <имя_папки>` | Перейти в подкаталог (или `..` для перехода в родительский каталог) |
| `parPath` | Вывести абсолютный путь родительской папки |
| `istChildSbd <имя_папки>` | Проверить, является ли каталог дочерним для текущего (`true` / `false`) |
| `dir [-d] [-all]` | Вывести содержимое каталога (`-d` — только папки, `-all` — рекурсивно с вложенными папками) |
| `tree [путь \| -o\|--output <файл>]` | Отрисовать дерево файлов и папок в консоль или сохранить в `<путь>/directory_structure.txt` |
| `mkdir <имя>` | Создать новую папку в текущем рабочем каталоге |
| `find <имя> [-d]` | Найти файл или папку по подстроке/расширению (`-d` — только каталоги) |
| `truncate [-d] <имя_папки_файла>` | Удалить файл или пустую папку (`-d` — удалить рекурсивно вместе с содержимым) |
| `help` | Вывести справку по доступным командам |
| `exit` | Завершить работу приложения |

## Пример сессии

```text
Запуск консольного приложения 'WorkingDirectory'
D:\console-java\my_folder>mkdir notes
Подкаталог успешно создан: notes
D:\console-java\my_folder>cd notes
D:\console-java\my_folder\notes>tree
notes
D:\console-java\my_folder\notes>cd ..
D:\console-java\my_folder>dir -d
my_folder
│  notes
D:\console-java\my_folder>exit
```

## Логирование

Конфигурация находится в `src/main/resources/logback.xml`:

* `logs/standard.log` — штатные информационные сообщения (`INFO`)
* `logs/error.log` — предупреждения и ошибки (`WARN` и `ERROR`)
* `logs/archive/` — ежедневная ротация логов с хранением истории за 30 дней



## Структура проекта

```text
.
├── .editorconfig
├── .gitignore
├── lombok.config
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   ├── Main.java
    │   │   ├── cli
    │   │   │   └── ConsoleApplication.java
    │   │   └── directory
    │   │       ├── OperationResult.java
    │   │       └── WorkingDirectory.java
    │   └── resources
    │       └── logback.xml
    └── test
        └── java
            └── directory
                └── WorkingDirectoryTest.java

```

## Тестирование и особенности реализации

* Модульные тесты в `WorkingDirectoryTest` проверяют работу `WorkingDirectory` (поведение *Singleton*, переходы по каталогам, создание и удаление, поиск, вывод дерева и граничные/ошибочные случаи), создавая и очищая временную тестовую папку `my_folder`.