package academy.incidentradar.source;

import java.nio.file.Path;
import java.util.Locale;

/** Проверка поддерживаемых расширений входных логов. */
final class LogFileNames {

    private LogFileNames() {}

    static boolean hasSupportedExtension(Path path) {
        var fileName = path.getFileName();
        return fileName != null && hasSupportedExtension(fileName.toString());
    }

    static boolean hasSupportedExtension(String name) {
        var normalized = name.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".log") || normalized.endsWith(".txt");
    }
}
