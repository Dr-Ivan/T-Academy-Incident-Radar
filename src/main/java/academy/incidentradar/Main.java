package academy.incidentradar;

import academy.incidentradar.cli.IncidentRadarCommand;
import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.source.LogReader;
import academy.incidentradar.source.LogSourceResolver;
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
        return IncidentRadarCommand.createCommandLine(Main::readSources);
    }

    private static void readSources(AnalysisOptions options) {
        var sources = new LogSourceResolver().resolve(options.path());
        var result = new LogReader().read(sources, ignored -> {});

        LOG.info(
                "Прочитано источников: {} (\n{}\n), строк: {}",
                result.files().size(),
                String.join(",\n", result.files()),
                result.linesRead());
    }
}
