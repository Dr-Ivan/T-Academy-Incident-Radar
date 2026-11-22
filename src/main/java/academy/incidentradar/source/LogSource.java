package academy.incidentradar.source;

import java.util.function.Consumer;

/** Источник логов, управляющий ресурсами своего чтения. */
public interface LogSource {

    String name();

    /** Последовательно передаёт строки обработчику и закрывает ресурсы при завершении. */
    void forEachLine(Consumer<String> action);
}
