package academy.incidentradar.source;

import academy.incidentradar.model.LogReadResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Последовательное чтение источников со сбором сведений о загрузке. */
public final class LogReader {

    public LogReadResult read(List<? extends LogSource> sources, Consumer<String> action) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(action, "action");

        var files = new ArrayList<String>();
        var linesRead = new AtomicLong();

        for (var source : sources) {
            source.forEachLine(line -> {
                action.accept(line);
                linesRead.incrementAndGet();
            });
            files.add(source.name());
        }

        return new LogReadResult(files, linesRead.get());
    }
}
