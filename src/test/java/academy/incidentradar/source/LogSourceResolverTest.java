package academy.incidentradar.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Проверки выбора локальных источников и раскрытия glob-масок. */
@DisplayName("Выбор источников логов")
class LogSourceResolverTest {

    @TempDir
    private Path tempDir;

    private final LogSourceResolver resolver = new LogSourceResolver();

    @Test
    @DisplayName("Подходящие файлы читаются полностью в стабильном порядке")
    void resolvesAndReadsMatchingFilesInStableOrder() throws IOException {
        var second = createFile("access-b.txt", "second");
        var first = createFile("access-a.log", "first");
        var third = createFile("access-c.LOG", "third");
        createFile("access-d.csv", "ignored");
        createFile("other.log", "ignored");
        createFile("nested/access-e.log", "ignored");
        Files.createDirectory(tempDir.resolve("access-directory.log"));

        var sources = resolver.resolve(glob("access-*"));
        var lines = new ArrayList<String>();

        for (var source : sources) {
            source.forEachLine(lines::add);
        }

        assertThat(sources)
                .extracting(LogSource::name)
                .containsExactly(first.toString(), second.toString(), third.toString());
        assertThat(lines).containsExactly("first", "second", "third");
    }

    @Test
    @DisplayName("Поддерживаются относительные glob-маски")
    void resolvesRelativeGlob() {
        var sources = resolver.resolve("samples/logs/access-*.log");

        assertThat(sources)
                .extracting(LogSource::name)
                .containsExactly(
                        Path.of("samples", "logs", "access-2025-03-01.log").toString(),
                        Path.of("samples", "logs", "access-2025-03-02.log").toString());
    }

    @Test
    @DisplayName("Маска поддерживает вложенные пути, диапазоны и одиночный символ")
    void resolvesNestedGlob() throws IOException {
        var first = createFile("2025/03/access-a.log", "first");
        var second = createFile("2025/04/access-b.log", "second");
        createFile("2025/04/access-long.log", "ignored");
        createFile("2025/05/access-c.log", "ignored");

        var sources = resolver.resolve(glob("2025/0[34]/access-?.log"));

        assertThat(sources).extracting(LogSource::name).containsExactly(first.toString(), second.toString());
    }

    @Test
    @DisplayName("Альтернативы позволяют включить корневые и вложенные файлы")
    void resolvesRecursiveGlobWithRootFiles() throws IOException {
        var rootFile = createFile("access-root.log", "root");
        var nestedFile = createFile("one/access-a.log", "nested");
        var deepFile = createFile("one/two/access-b.log", "deep");

        var sources = resolver.resolve(glob("{*.log,**/*.log}"));

        assertThat(sources)
                .extracting(LogSource::name)
                .containsExactly(rootFile.toString(), nestedFile.toString(), deepFile.toString());
    }

    @Test
    @DisplayName("Отсутствие подходящих файлов считается ошибкой")
    void rejectsGlobWithoutMatches() {
        assertThatThrownBy(() -> resolver.resolve(glob("missing-*.log")))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("не найдены");
    }

    @Test
    @DisplayName("Ошибка отсутствующего каталога сохраняет причину")
    void missingSearchRootPreservesCause() {
        assertThatThrownBy(() -> resolver.resolve(glob("missing/*.log")))
                .isInstanceOf(LogSourceException.class)
                .hasCauseInstanceOf(NoSuchFileException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[invalid.log", "{a,b.log"})
    @DisplayName("Некорректная glob-маска отклоняется")
    void rejectsMalformedGlob(String pattern) {
        assertThatThrownBy(() -> resolver.resolve(glob(pattern)))
                .isInstanceOf(LogSourceException.class)
                .hasCauseInstanceOf(PatternSyntaxException.class);
    }

    @Test
    @DisplayName("Существующий файл имеет приоритет перед glob-синтаксисом")
    void prefersExistingLiteralFile() throws IOException {
        var literal = createFile("access[1].log", "literal");
        createFile("access1.log", "other");

        var sources = resolver.resolve(literal.toString());

        assertThat(sources).extracting(LogSource::name).containsExactly(literal.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "     ", "\t", "\r\n"})
    @DisplayName("Пустой источник отклоняется с понятным сообщением")
    void rejectsBlankSource(String value) {
        assertThatThrownBy(() -> resolver.resolve(value))
                .isInstanceOf(LogSourceException.class)
                .hasMessage("Источник логов не должен быть пустым");
    }

    private Path createFile(String relativePath, String content) throws IOException {
        var path = tempDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    private String glob(String relativePattern) {
        return tempDir + "/" + relativePattern;
    }
}
