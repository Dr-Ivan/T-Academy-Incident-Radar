package academy.incidentradar.analysis;

import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.LogRecord;
import java.util.Objects;
import java.util.function.Predicate;

/** Отбор записей по календарной дате. */
public final class DateFilter implements Predicate<LogRecord> {

    private final DateRange dateRange;

    public DateFilter(DateRange dateRange) {
        this.dateRange = Objects.requireNonNull(dateRange, "dateRange");
    }

    @Override
    public boolean test(LogRecord record) {
        Objects.requireNonNull(record, "record");

        var date = record.timestamp().toLocalDate();
        var from = dateRange.from();
        var to = dateRange.to();

        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }
}
