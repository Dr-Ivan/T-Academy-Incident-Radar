package academy.incidentradar.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Метрики размера тела ответа в байтах. */
public record ResponseSizeStatistics(BigDecimal average, long max, long p95) {

    public ResponseSizeStatistics {
        Objects.requireNonNull(average, "average");
        if (average.signum() < 0 || max < 0 || p95 < 0) {
            throw new IllegalArgumentException("Метрики размера ответа не могут быть отрицательными");
        }
        if (p95 > max || average.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new IllegalArgumentException("Среднее и p95 не могут превышать максимальный размер");
        }
    }
}
