package academy.incidentradar.model;

import java.util.List;

public record LogReadResult(List<String> files, long linesRead) {

    public LogReadResult {
        files = List.copyOf(files);
        if (linesRead < 0) {
            throw new IllegalArgumentException("Количество прочитанных строк не может быть отрицательным");
        }
    }
}
