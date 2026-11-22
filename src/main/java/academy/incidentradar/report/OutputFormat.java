package academy.incidentradar.report;

import java.nio.file.Path;
import java.util.Locale;

public enum OutputFormat {
    JSON(".json") {
        @Override
        public ReportRenderer createRenderer() {
            return new JsonReportRenderer();
        }
    },
    MARKDOWN(".md") {
        @Override
        public ReportRenderer createRenderer() {
            return new MarkdownReportRenderer();
        }
    },
    ADOC(".adoc") {
        @Override
        public ReportRenderer createRenderer() {
            return new AsciiDocReportRenderer();
        }
    };

    private final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public abstract ReportRenderer createRenderer();

    public boolean matchesExtension(Path path) {
        var fileName = path.getFileName();
        return fileName != null && fileName.toString().toLowerCase(Locale.ROOT).endsWith(extension);
    }
}
