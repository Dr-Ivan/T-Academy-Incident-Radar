package academy.incidentradar.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.incidentradar.support.CliRunner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Граничные случаи чтения локального источника. */
@DisplayName("Локальный источник логов")
class LocalLogSourceTest {

    @TempDir
    private Path tempDir;

    private final List<String> receivedLines = new ArrayList<>();

    @ParameterizedTest
    @ValueSource(strings = {"access.txt", "access.LOG", "access.TXT"})
    @DisplayName("Поддерживаются .log и .txt без учёта регистра")
    void acceptsSupportedExtensions(String fileName) throws IOException {
        var path = tempDir.resolve(fileName);
        Files.writeString(path, "строка", StandardCharsets.UTF_8);

        new LocalLogSource(path).forEachLine(receivedLines::add);
        assertThat(receivedLines).containsExactly("строка");
    }

    @Test
    @DisplayName("Пустой файл успешно читается без вызова обработчика")
    void readsEmptyFile() throws IOException {
        var path = Files.createFile(tempDir.resolve("empty.log"));
        new LocalLogSource(path).forEachLine(receivedLines::add);
        assertThat(receivedLines).isEmpty();
    }

    @Test
    @DisplayName("Ошибка отсутствующего файла сохраняет исходную причину")
    void missingFilePreservesCause() {
        var path = tempDir.resolve("missing.log");

        assertThatThrownBy(() -> new LocalLogSource(path).forEachLine(receivedLines::add))
                .isInstanceOf(LogSourceException.class)
                .hasCauseInstanceOf(NoSuchFileException.class)
                .hasMessageContaining(path.toString());

        assertThat(receivedLines).isEmpty();
    }

    @Test
    @DisplayName("Каталог нельзя использовать как файл лога")
    void rejectsDirectory() throws IOException {
        var path = Files.createDirectory(tempDir.resolve("directory.log"));

        assertThatThrownBy(() -> new LocalLogSource(path).forEachLine(receivedLines::add))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("обычный файл");

        assertThat(receivedLines).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"access.csv", "access.log.bak"})
    @DisplayName("Неподдерживаемое расширение отклоняется")
    void rejectsUnsupportedExtension(String fileName) {
        var path = tempDir.resolve(fileName);
        assertThatThrownBy(() -> new LocalLogSource(path))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining(fileName);
    }

    @Test
    @DisplayName("Ошибка обработчика передаётся без оборачивания и останавливает чтение")
    void propagatesActionFailure() throws IOException {
        var path = tempDir.resolve("access.log");
        Files.writeString(path, "first\nsecond", StandardCharsets.UTF_8);
        var failure = new IllegalStateException("Тестовый сбой обработчика");

        assertThatThrownBy(() -> new LocalLogSource(path).forEachLine(line -> {
                    receivedLines.add(line);
                    throw failure;
                }))
                .isSameAs(failure);

        assertThat(receivedLines).containsExactly("first");
    }

    @Test
    @DisplayName("Ошибка UTF-8 в user-agent даёт один WARN, анализ продолжается")
    void analyzesValidRecordsOnBothSidesOfInvalidUtf8() throws IOException {
        var path = tempDir.resolve("mixed.log");
        var prefix = "127.0.0.1 - - [01/Mar/2025:12:00:00 +0000] \"GET / HTTP/1.1\" 200 10 \"-\" \"";
        var valid = prefix + "клиент\"";
        try (var output = Files.newOutputStream(path)) {
            output.write((valid + "\n" + prefix).getBytes(StandardCharsets.UTF_8));
            output.write(0xFF);
            output.write(("\"\n" + valid).getBytes(StandardCharsets.UTF_8));
        }

        var result = CliRunner.run("--path", path.toString());

        assertThat(result.exitCode()).as("%s", result).isZero();
        assertThat(result.stderr()).contains("строк: 3", "Некорректных строк: 1, запросов в основном периоде: 2");
        assertThat(result.stderr()
                        .lines()
                        .filter(line -> line.contains("WARN") && line.contains("skipped malformed log line"))
                        .count())
                .isEqualTo(1);
    }
}
