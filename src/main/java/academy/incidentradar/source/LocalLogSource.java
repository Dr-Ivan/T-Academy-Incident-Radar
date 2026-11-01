package academy.incidentradar.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.Consumer;

/** Построчное чтение локального лога в UTF-8. */
public record LocalLogSource(Path path) implements LogSource {

    public LocalLogSource {
        Objects.requireNonNull(path, "path");

        if (!LogFileNames.hasSupportedExtension(path)) {
            throw new LogSourceException("Поддерживаются только файлы .log и .txt: " + path);
        }
    }

    @Override
    public String name() {
        return path.toString();
    }

    @Override
    public void forEachLine(Consumer<String> action) {
        Objects.requireNonNull(action, "action");

        try {
            var attributes = Files.readAttributes(path, BasicFileAttributes.class);
            if (!attributes.isRegularFile()) {
                throw new LogSourceException("Ожидался обычный файл: " + path);
            }

            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    action.accept(line);
                }
            }
        } catch (IOException exception) {
            throw new LogSourceException(
                    "Не удалось прочитать локальный лог '" + path + "': " + exception.getMessage(), exception);
        }
    }
}
