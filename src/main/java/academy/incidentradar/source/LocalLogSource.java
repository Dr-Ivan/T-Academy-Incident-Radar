package academy.incidentradar.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
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
        forEachLine(action, () -> {});
    }

    @Override
    public void forEachLine(Consumer<String> action, Runnable onDecodingError) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(onDecodingError, "onDecodingError");

        try {
            var attributes = Files.readAttributes(path, BasicFileAttributes.class);
            if (!attributes.isRegularFile()) {
                throw new LogSourceException("Ожидался обычный файл: " + path);
            }

            try (var byteLines = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
                decodeUtf8Lines(byteLines, action, onDecodingError);
            }
        } catch (IOException exception) {
            throw new LogSourceException(
                    "Не удалось прочитать локальный лог '" + path + "': " + exception.getMessage(), exception);
        }
    }

    /** ISO-8859-1 сохраняет байты один к одному до разделения строк. Текст каждой строки декодируется как UTF-8. */
    private static void decodeUtf8Lines(BufferedReader byteLines, Consumer<String> action, Runnable onDecodingError)
            throws IOException {
        var decoder = StandardCharsets.UTF_8.newDecoder();
        String byteLine;
        while ((byteLine = byteLines.readLine()) != null) {
            var bytes = ByteBuffer.wrap(byteLine.getBytes(StandardCharsets.ISO_8859_1));
            String text;
            try {
                text = decoder.decode(bytes).toString();
            } catch (CharacterCodingException ignored) {
                onDecodingError.run();
                continue;
            }
            action.accept(text);
        }
    }
}
