package academy.incidentradar.model;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/** Неизменяемый результат подсчёта запросов. */
public record LogStatistics(
        long totalRequestsCount,
        ResponseSizeStatistics responseSizeInBytes,
        Map<Integer, Long> responseCodes,
        List<ResourceStatistics> resources,
        Map<DayOfWeek, Long> requestsByWeekday,
        List<String> uniqueProtocols) {

    public LogStatistics {
        Objects.requireNonNull(responseSizeInBytes, "responseSizeInBytes");
        if (totalRequestsCount < 0) {
            throw new IllegalArgumentException("Число запросов не может быть отрицательным");
        }

        responseCodes = Collections.unmodifiableMap(new TreeMap<>(Map.copyOf(responseCodes)));
        resources = List.copyOf(resources);

        var weekdays = new EnumMap<DayOfWeek, Long>(DayOfWeek.class);
        weekdays.putAll(Map.copyOf(requestsByWeekday));
        for (var day : DayOfWeek.values()) {
            weekdays.putIfAbsent(day, 0L);
        }
        requestsByWeekday = Collections.unmodifiableMap(weekdays);
        uniqueProtocols = List.copyOf(new TreeSet<>(uniqueProtocols));
    }
}
