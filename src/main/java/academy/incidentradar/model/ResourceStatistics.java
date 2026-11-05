package academy.incidentradar.model;

import java.util.Objects;

/** Число обращений к одному ресурсу. */
public record ResourceStatistics(String resource, long totalRequestsCount) {

    public ResourceStatistics {
        Objects.requireNonNull(resource, "resource");
        if (resource.isBlank()) {
            throw new IllegalArgumentException("Ресурс не должен быть пустым");
        }
        if (totalRequestsCount <= 0) {
            throw new IllegalArgumentException("Число запросов к ресурсу должно быть положительным");
        }
    }
}
