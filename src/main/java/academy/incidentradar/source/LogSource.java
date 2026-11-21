package academy.incidentradar.source;

import java.util.Objects;
import java.util.function.Consumer;

/** Источник логов, управляющий ресурсами своего чтения. */
public interface LogSource {

    String name();

    /** Последовательно передаёт строки обработчику и закрывает ресурсы при завершении. */
    void forEachLine(Consumer<String> action);

    /** Позволяет источнику отдельно сообщить об ошибке декодирования строки. */
    default void forEachLine(Consumer<String> action, Runnable onDecodingError) {
        Objects.requireNonNull(onDecodingError, "onDecodingError");
        forEachLine(action);
    }
}
