package academy.incidentradar.model;

import academy.incidentradar.report.OutputFormat;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Проверенные настройки запуска анализа. */
public record AnalysisOptions(
        String path,
        OutputFormat outputFormat,
        @Nullable Path outputPath,
        DateRange dateRange,
        @Nullable DateRange comparisonDateRange) {

    public AnalysisOptions {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(outputFormat, "outputFormat");
        Objects.requireNonNull(dateRange, "dateRange");

        if (path.isBlank()) {
            throw new IllegalArgumentException("--path не должен быть пустым");
        }

        if (outputPath != null && !outputFormat.matchesExtension(outputPath)) {
            throw new IllegalArgumentException("--output-path должен иметь расширение " + outputFormat.extension());
        }
        if (comparisonDateRange != null) {
            if (comparisonDateRange.from() == null || comparisonDateRange.to() == null) {
                throw new IllegalArgumentException("Для сравнения укажите --compare-from и --compare-to");
            }
            if (dateRange.from() == null || dateRange.to() == null) {
                throw new IllegalArgumentException("Для сравнения укажите --from и --to");
            }
        }
    }

    public AnalysisOptions(String path, OutputFormat outputFormat, @Nullable Path outputPath, DateRange dateRange) {
        this(path, outputFormat, outputPath, dateRange, null);
    }
}
