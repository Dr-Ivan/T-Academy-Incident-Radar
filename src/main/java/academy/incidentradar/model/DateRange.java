package academy.incidentradar.model;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/** Период с необязательными включительными границами. */
public record DateRange(@Nullable LocalDate from, @Nullable LocalDate to) {

    public DateRange {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("--from должен быть раньше --to");
        }
    }
}
