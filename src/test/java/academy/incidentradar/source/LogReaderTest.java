package academy.incidentradar.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

/** Проверки результата чтения нескольких источников. */
@DisplayName("Чтение источников логов")
class LogReaderTest {

    @TempDir
    private Path tempDir;

    private final LogReader reader = new LogReader();

    @Test
    @DisplayName("Успешно прочитанный пустой файл включается в результат")
    void includesEmptyFilesInResult() throws IOException {
        var emptyPath = Files.createFile(tempDir.resolve("empty.log"));
        var logPath = tempDir.resolve("access.log");
        Files.writeString(logPath, "entry\n", StandardCharsets.UTF_8);
        var lines = new ArrayList<String>();

        var result = reader.read(List.of(new LocalLogSource(emptyPath), new LocalLogSource(logPath)), lines::add);

        assertThat(result.files()).containsExactly(emptyPath.toString(), logPath.toString());
        assertThat(result.linesRead()).isEqualTo(1);
        assertThat(lines).containsExactly("entry");
    }

    @Test
    @DisplayName("Ошибка одного из источников прерывает операцию чтения")
    void sourceFailureAbortsReading() throws IOException {
        var firstPath = tempDir.resolve("first.log");
        Files.writeString(firstPath, "entry\n", StandardCharsets.UTF_8);
        var missingPath = tempDir.resolve("missing.log");
        var lines = new ArrayList<String>();

        assertThatThrownBy(() -> reader.read(
                        List.of(new LocalLogSource(firstPath), new LocalLogSource(missingPath)), lines::add))
                .isInstanceOf(LogSourceException.class)
                .hasCauseInstanceOf(NoSuchFileException.class);
    }
}
