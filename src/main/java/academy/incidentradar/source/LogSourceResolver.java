package academy.incidentradar.source;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

/** Выбор источников логов по значению параметра --path. */
public final class LogSourceResolver {

    public List<LogSource> resolve(String value) {
        Objects.requireNonNull(value, "value");

        var normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("http:") || normalized.startsWith("https:")) {
            return resolveHttp(value);
        }

        var globIndex = firstGlobIndex(value);
        try {
            var path = Path.of(value);
            if (Files.exists(path) || globIndex < 0) {
                return List.of(new LocalLogSource(path));
            }
        } catch (InvalidPathException exception) {
            if (globIndex < 0) {
                throw new LogSourceException("Некорректный путь к локальному логу: " + value, exception);
            }
        }

        return resolveGlob(value, globIndex);
    }

    private static List<LogSource> resolveGlob(String value, int globIndex) {
        List<LogSource> sources;

        try {
            var pattern = value.replace(File.separatorChar, '/');
            var prefixEnd = pattern.lastIndexOf('/', globIndex) + 1;
            var root = prefixEnd == 0 ? Path.of(".") : Path.of(pattern.substring(0, prefixEnd));
            var relativePattern = pattern.substring(prefixEnd);
            var matcher = root.getFileSystem().getPathMatcher("glob:" + relativePattern);
            var maxDepth = relativePattern.contains("**") ? Integer.MAX_VALUE : relativePattern.split("/").length;

            try (var paths = Files.walk(root, maxDepth)) {
                sources = paths.filter(Files::isRegularFile)
                        .filter(LogFileNames::hasSupportedExtension)
                        .filter(path -> matcher.matches(root.relativize(path)))
                        .sorted(Comparator.comparing(Path::toString))
                        .<LogSource>map(LocalLogSource::new)
                        .toList();
            }
        } catch (InvalidPathException | PatternSyntaxException exception) {
            throw new LogSourceException("Некорректная glob-маска: " + value, exception);
        } catch (IOException | UncheckedIOException exception) {
            throw new LogSourceException(
                    "Не удалось раскрыть glob-маску '" + value + "': " + exception.getMessage(), exception);
        }

        if (sources.isEmpty()) {
            throw new LogSourceException("По маске не найдены файлы .log или .txt: " + value);
        }
        return sources;
    }

    private static int firstGlobIndex(String value) {
        for (int index = 0; index < value.length(); index++) {
            if ("*?[{".indexOf(value.charAt(index)) >= 0) {
                return index;
            }
        }
        return -1;
    }

    private static List<LogSource> resolveHttp(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new LogSourceException("Некорректный HTTP/HTTPS URL: " + value, exception);
        }
        return List.of(new HttpLogSource(uri));
    }
}
