package academy.incidentradar.source;

import academy.incidentradar.model.LogReadResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Последовательное чтение источников со сбором сведений о загрузке. */
public final class LogReader {

    private static final Logger LOG = LoggerFactory.getLogger(LogReader.class);

    public LogReadResult read(List<? extends LogSource> sources, Consumer<String> action) {
        return read(sources, action, () -> LOG.warn("skipped malformed log line"));
    }

    public LogReadResult read(List<? extends LogSource> sources, Consumer<String> action, Runnable onDecodingError) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(onDecodingError, "onDecodingError");

        var files = new ArrayList<String>();
        var linesRead = new AtomicLong();

        for (var source : sources) {
            source.forEachLine(
                    line -> {
                        action.accept(line);
                        linesRead.incrementAndGet();
                    },
                    () -> {
                        onDecodingError.run();
                        linesRead.incrementAndGet();
                    });
            files.add(source.name());
        }

        return new LogReadResult(files, linesRead.get());
    }
}
