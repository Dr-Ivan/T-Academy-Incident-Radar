package academy.incidentradar.report;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Вывод готового отчёта в stdout или новый UTF-8 файл. */
public final class ReportWriter {

    public void write(String report, @Nullable Path outputPath, PrintWriter stdout) {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(stdout, "stdout");

        if (outputPath == null) {
            stdout.print(report);
            if (stdout.checkError()) {
                throw new ReportOutputException("Не удалось записать отчёт в stdout");
            }
            return;
        }

        writeFile(report, outputPath);
    }

    private static void writeFile(String report, Path outputPath) {
        var created = false;
        try {
            var content = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(report));
            try (var channel = Files.newByteChannel(outputPath, CREATE_NEW, WRITE)) {
                created = true;
                while (content.hasRemaining()) {
                    channel.write(content);
                }
            }
        } catch (IOException exception) {
            if (created) {
                try {
                    Files.deleteIfExists(outputPath);
                } catch (IOException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }

            var message = exception instanceof FileAlreadyExistsException
                    ? "Файл отчёта уже существует: " + outputPath
                    : "Не удалось записать отчёт '" + outputPath + "': " + exception.getMessage();
            throw new ReportOutputException(message, exception);
        }
    }
}
