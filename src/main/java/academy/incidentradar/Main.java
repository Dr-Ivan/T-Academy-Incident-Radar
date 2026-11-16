package academy.incidentradar;

import academy.incidentradar.analysis.LogAnalysisService;
import academy.incidentradar.cli.IncidentRadarCommand;
import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.report.ReportData;
import academy.incidentradar.report.ReportWriter;
import academy.incidentradar.source.LogSourceResolver;
import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** Точка входа в Incident Radar. */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {}

    public static void main(String[] args) {
        System.exit(createCommandLine().execute(args));
    }

    public static CommandLine createCommandLine() {
        return IncidentRadarCommand.createCommandLineWithOutput(Main::analyze);
    }

    private static void analyze(AnalysisOptions options, PrintWriter stdout) {
        var sources = new LogSourceResolver().resolve(options.path());
        var result = new LogAnalysisService().analyze(sources, options);
        var readResult = result.readResult();

        LOG.info(
                "Прочитано источников: {} (\n{}\n), строк: {}",
                readResult.files().size(),
                String.join(",\n", readResult.files()),
                readResult.linesRead());
        LOG.info(
                "Некорректных строк: {}, запросов в основном периоде: {}",
                result.malformedLines(),
                result.statistics().totalRequestsCount());

        var comparison = result.comparison();
        if (comparison != null) {
            var change = comparison.requestsChange();
            var percentage = change.percentage();
            var relative = percentage == null ? "н/д" : percentage.toPlainString() + "%";
            LOG.info(
                    "Запросов в периоде сравнения: {}, изменение: {} ({})",
                    comparison.referenceStatistics().totalRequestsCount(),
                    change.difference().toPlainString(),
                    relative);
        }

        var data = ReportData.from(options, result);
        var report = options.outputFormat().createRenderer().render(data);
        new ReportWriter().write(report, options.outputPath(), stdout);
    }
}
