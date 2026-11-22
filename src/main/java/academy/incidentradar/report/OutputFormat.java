package academy.incidentradar.report;

import java.nio.file.Path;
import java.util.Locale;

public enum OutputFormat {
    JSON(".json"),
    MARKDOWN(".md"),
    ADOC(".adoc");

    private final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public boolean matchesExtension(Path path) {
        var fileName = path.getFileName();
        return fileName != null && fileName.toString().toLowerCase(Locale.ROOT).endsWith(extension);
    }
}
