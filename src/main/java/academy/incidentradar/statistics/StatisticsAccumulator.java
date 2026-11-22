package academy.incidentradar.statistics;

import academy.incidentradar.model.LogRecord;
import academy.incidentradar.model.LogStatistics;
import academy.incidentradar.model.ResourceStatistics;
import academy.incidentradar.model.ResponseSizeStatistics;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

/** Последовательное накопление статистики отобранных записей. */
public final class StatisticsAccumulator implements Consumer<LogRecord> {

    private static final int TOP_RESOURCES_LIMIT = 10;
    private static final int AVERAGE_SCALE = 2;
    private static final int P95_TAIL_DIVISOR = 20;

    private final NavigableMap<Long, Long> responseSizeCounts = new TreeMap<>();

    private static final Comparator<Map.Entry<String, Long>> RESOURCE_ORDER =
            Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey());

    private final Map<Integer, Long> responseCodes = new HashMap<>();
    private final Map<String, Long> resourceCounts = new HashMap<>();
    private final Map<DayOfWeek, Long> requestsByWeekday = new EnumMap<>(DayOfWeek.class);
    private final Set<String> uniqueProtocols = new HashSet<>();

    private long totalRequestsCount;

    @Override
    public void accept(LogRecord record) {
        Objects.requireNonNull(record, "record");

        totalRequestsCount = Math.incrementExact(totalRequestsCount);
        responseCodes.merge(record.status(), 1L, Math::addExact);
        resourceCounts.merge(record.resource(), 1L, Math::addExact);
        requestsByWeekday.merge(record.timestamp().getDayOfWeek(), 1L, Math::addExact);
        uniqueProtocols.add(record.protocol());
        responseSizeCounts.merge(record.bodyBytesSent(), 1L, Math::addExact);
    }

    public LogStatistics snapshot() {
        return new LogStatistics(
                totalRequestsCount,
                responseSizeStatistics(),
                responseCodes,
                topResources(),
                requestsByWeekday,
                List.copyOf(uniqueProtocols));
    }

    private ResponseSizeStatistics responseSizeStatistics() {
        if (totalRequestsCount == 0) {
            return new ResponseSizeStatistics(BigDecimal.ZERO.setScale(AVERAGE_SCALE), 0, 0);
        }

        var rank = totalRequestsCount - totalRequestsCount / P95_TAIL_DIVISOR;
        var cumulativeCount = 0L;
        var p95 = 0L;
        var totalBytes = BigInteger.ZERO;

        for (var entry : responseSizeCounts.entrySet()) {
            var size = entry.getKey();
            var count = entry.getValue();
            if (cumulativeCount < rank) {
                p95 = size;
            }
            cumulativeCount = Math.addExact(cumulativeCount, count);
            totalBytes = totalBytes.add(BigInteger.valueOf(size).multiply(BigInteger.valueOf(count)));
        }

        var average = new BigDecimal(totalBytes)
                .divide(BigDecimal.valueOf(totalRequestsCount), AVERAGE_SCALE, RoundingMode.HALF_UP);
        return new ResponseSizeStatistics(average, responseSizeCounts.lastKey(), p95);
    }

    private List<ResourceStatistics> topResources() {
        var top = new PriorityQueue<>(TOP_RESOURCES_LIMIT, RESOURCE_ORDER.reversed());

        for (var entry : resourceCounts.entrySet()) {
            if (top.size() < TOP_RESOURCES_LIMIT) {
                top.add(entry);
            } else if (RESOURCE_ORDER.compare(entry, top.element()) < 0) {
                top.remove();
                top.add(entry);
            }
        }

        return top.stream()
                .sorted(RESOURCE_ORDER)
                .map(entry -> new ResourceStatistics(entry.getKey(), entry.getValue()))
                .toList();
    }
}
