package academy.incidentradar.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.source.LocalLogSource;
import academy.incidentradar.source.LogReader;
import academy.incidentradar.source.LogSourceResolver;
import academy.incidentradar.support.CliRunner;
import academy.incidentradar.support.TestHttpServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Обязательные тесты: источники логов. */
@DisplayName("MR1. Источники логов")
class LogSourceTest {

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("Локальный файл читается")
    void readsLocalFile() throws IOException {
        var path = tempDir.resolve("access.log");
        Files.writeString(path, "первая строка\n\nпоследняя строка", StandardCharsets.UTF_8);
        var lines = new ArrayList<String>();

        new LocalLogSource(path).forEachLine(lines::add);
        assertThat(lines).containsExactly("первая строка", "", "последняя строка");
    }

    @Test
    @DisplayName("Glob-маска: в отчёт попадают все подошедшие файлы")
    void readsAllFilesMatchingGlob() throws IOException {
        var first = tempDir.resolve("access-01.log");
        var second = tempDir.resolve("access-02.log");
        var other = tempDir.resolve("other.log");

        Files.writeString(first, "first-1\nfirst-2\n", StandardCharsets.UTF_8);
        Files.writeString(second, "second-1\nsecond-2\nsecond-3\n", StandardCharsets.UTF_8);
        Files.writeString(other, "ignored\n", StandardCharsets.UTF_8);

        var sources = new LogSourceResolver().resolve(tempDir + "/access-*.log");
        var lines = new ArrayList<String>();

        var result = new LogReader().read(sources, lines::add);

        assertThat(result.files()).containsExactly(first.toString(), second.toString());
        assertThat(result.linesRead()).isEqualTo(5);
        assertThat(lines).containsExactly("first-1", "first-2", "second-1", "second-2", "second-3");
    }

    @Test
    @DisplayName("Лог читается по URL")
    void readsRemoteFile() throws IOException {
        try (var server = new TestHttpServer(200, "первая\n\nпоследняя")) {
            var uri = server.uri();
            var sources = new LogSourceResolver().resolve(uri.toString());
            var lines = new ArrayList<String>();

            var result = new LogReader().read(sources, lines::add);

            assertThat(result.files()).containsExactly(uri.toString());
            assertThat(result.linesRead()).isEqualTo(3);
            assertThat(lines).containsExactly("первая", "", "последняя");
        }
    }

    @Test
    @DisplayName("URL вернул 404 -> exit code 2")
    void remote404ExitsWithTwo() throws IOException {
        try (var server = new TestHttpServer(404, "not found")) {
            var result = CliRunner.run("--path", server.uri().toString());

            assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
            assertThat(result.stderr()).contains("404", server.uri().toString());
            assertThat(result.stdout()).isEmpty();
        }
    }
}
