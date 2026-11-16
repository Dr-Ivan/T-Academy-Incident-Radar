package academy.incidentradar.cli;

import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.report.OutputFormat;
import academy.incidentradar.report.ReportOutputException;
import academy.incidentradar.source.LogSourceException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

/** Разбор аргументов и передача настроек обработчику анализа. */
@Command(
        name = "incident-radar",
        description = "Анализ NGINX-логов.",
        mixinStandardHelpOptions = true,
        version = "incident-radar MR3")
public final class IncidentRadarCommand implements Callable<Integer> {

    private static final Pattern DATE_PATTERN = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private final BiConsumer<AnalysisOptions, PrintWriter> action;

    @Option(
            names = {"--path", "-p"},
            required = true,
            paramLabel = "SOURCE",
            description = "Локальный файл, glob-маска или URL.")
    private String path = "";

    @Option(
            names = {"--output-format", "-f"},
            paramLabel = "FORMAT",
            description = "Формат отчёта: json, markdown или adoc.",
            showDefaultValue = Visibility.ALWAYS)
    private OutputFormat outputFormat = OutputFormat.MARKDOWN;

    @Option(
            names = {"--output-path", "-o"},
            paramLabel = "FILE",
            description = "Файл отчёта; если не указан, используется stdout.")
    private @Nullable Path outputPath;

    @Option(names = "--from", paramLabel = "YYYY-MM-DD", description = "Начальная дата включительно.")
    private @Nullable LocalDate from;

    @Option(names = "--to", paramLabel = "YYYY-MM-DD", description = "Конечная дата включительно.")
    private @Nullable LocalDate to;

    @Option(names = "--compare-from", paramLabel = "YYYY-MM-DD", description = "Начало периода сравнения включительно.")
    private @Nullable LocalDate compareFrom;

    @Option(names = "--compare-to", paramLabel = "YYYY-MM-DD", description = "Конец периода сравнения включительно.")
    private @Nullable LocalDate compareTo;

    @Spec
    private @Nullable CommandSpec spec;

    private IncidentRadarCommand(BiConsumer<AnalysisOptions, PrintWriter> action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    /** Создаёт независимый экземпляр CLI для одного запуска. */
    public static CommandLine createCommandLine(Consumer<AnalysisOptions> action) {
        Objects.requireNonNull(action, "action");
        return createCommandLineWithOutput((options, ignoredOutput) -> action.accept(options));
    }

    /** Передаёт обработчику текущий stdout команды. */
    public static CommandLine createCommandLineWithOutput(BiConsumer<AnalysisOptions, PrintWriter> action) {
        return new CommandLine(new IncidentRadarCommand(action))
                .setCaseInsensitiveEnumValuesAllowed(true)
                .registerConverter(LocalDate.class, IncidentRadarCommand::parseDate)
                .setExecutionExceptionHandler((exception, commandLine, ignored) -> {
                    if (exception instanceof LogSourceException || exception instanceof ReportOutputException) {
                        commandLine.getErr().println("Ошибка: " + exception.getMessage());
                        commandLine.getErr().flush();
                        return ExitCode.USAGE;
                    }

                    throw exception;
                });
    }

    @Override
    public Integer call() {
        var options = validatedOptions();
        action.accept(options, commandLine().getOut());
        return ExitCode.OK;
    }

    private CommandLine commandLine() {
        return Objects.requireNonNull(spec, "Команда не инициализирована picocli")
                .commandLine();
    }

    private AnalysisOptions validatedOptions() {
        try {
            return new AnalysisOptions(path, outputFormat, outputPath, new DateRange(from, to), comparisonDateRange());
        } catch (IllegalArgumentException exception) {
            var message = Objects.requireNonNullElse(exception.getMessage(), "Некорректные параметры запуска");
            throw new ParameterException(commandLine(), message, exception);
        }
    }

    private @Nullable DateRange comparisonDateRange() {
        if (compareFrom == null && compareTo == null) {
            return null;
        }
        try {
            return new DateRange(compareFrom, compareTo);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("--compare-from должен быть раньше --compare-to", exception);
        }
    }

    private static LocalDate parseDate(String value) {
        if (!DATE_PATTERN.matcher(value).matches()) {
            throw new TypeConversionException("Ожидается дата в формате YYYY-MM-DD");
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            throw new TypeConversionException("Некорректная календарная дата: " + value);
        }
    }
}
