package academy.incidentradar.cli;

import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.report.OutputFormat;
import academy.incidentradar.source.LogSourceException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.concurrent.Callable;
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
        version = "incident-radar MR1")
public final class IncidentRadarCommand implements Callable<Integer> {

    private static final Pattern DATE_PATTERN = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private final Consumer<AnalysisOptions> action;

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

    @Spec
    private @Nullable CommandSpec spec;

    private IncidentRadarCommand(Consumer<AnalysisOptions> action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    /** Создаёт независимый экземпляр CLI для одного запуска. */
    public static CommandLine createCommandLine(Consumer<AnalysisOptions> action) {
        return new CommandLine(new IncidentRadarCommand(action))
                .setCaseInsensitiveEnumValuesAllowed(true)
                .registerConverter(LocalDate.class, IncidentRadarCommand::parseDate)
                .setExecutionExceptionHandler((exception, commandLine, ignored) -> {
                    if (exception instanceof LogSourceException) {
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
        action.accept(options);
        return ExitCode.OK;
    }

    private AnalysisOptions validatedOptions() {
        try {
            return new AnalysisOptions(path, outputFormat, outputPath, new DateRange(from, to));
        } catch (IllegalArgumentException exception) {
            var commandSpec = Objects.requireNonNull(spec, "Команда не инициализирована picocli");
            var message = Objects.requireNonNullElse(exception.getMessage(), "Некорректные параметры запуска");
            throw new ParameterException(commandSpec.commandLine(), message, exception);
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
