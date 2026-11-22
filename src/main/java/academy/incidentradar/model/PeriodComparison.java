package academy.incidentradar.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Статистика периода сравнения и изменения показателей основного периода. */
public record PeriodComparison(
        LogStatistics referenceStatistics,
        MetricChange requestsChange,
        MetricChange averageSizeChange,
        MetricChange maxSizeChange,
        MetricChange p95SizeChange) {

    private static final int PERCENTAGE_SCALE = 2;

    public PeriodComparison {
        Objects.requireNonNull(referenceStatistics, "referenceStatistics");
        Objects.requireNonNull(requestsChange, "requestsChange");
        Objects.requireNonNull(averageSizeChange, "averageSizeChange");
        Objects.requireNonNull(maxSizeChange, "maxSizeChange");
        Objects.requireNonNull(p95SizeChange, "p95SizeChange");
    }

    public static PeriodComparison between(LogStatistics primary, LogStatistics reference) {
        var primarySizes = primary.responseSizeInBytes();
        var referenceSizes = reference.responseSizeInBytes();
        return new PeriodComparison(
                reference,
                change(primary.totalRequestsCount(), reference.totalRequestsCount()),
                change(primarySizes.average(), referenceSizes.average()),
                change(primarySizes.max(), referenceSizes.max()),
                change(primarySizes.p95(), referenceSizes.p95()));
    }

    private static MetricChange change(long primary, long reference) {
        return change(BigDecimal.valueOf(primary), BigDecimal.valueOf(reference));
    }

    private static MetricChange change(BigDecimal primary, BigDecimal reference) {
        var difference = primary.subtract(reference);
        var percentage = reference.signum() == 0
                ? null
                : difference.movePointRight(2).divide(reference, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
        return new MetricChange(difference, percentage);
    }

    /** Разность значений и её процент при ненулевом значении периода сравнения. */
    public record MetricChange(
            BigDecimal difference, @Nullable BigDecimal percentage) {

        public MetricChange {
            Objects.requireNonNull(difference, "difference");
        }
    }
}
