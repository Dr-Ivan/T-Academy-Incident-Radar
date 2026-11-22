package academy.incidentradar.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Проверки неизменяемости и инвариантов результата чтения. */
@DisplayName("Результат чтения логов")
class LogReadResultTest {

    @Test
    @DisplayName("Список источников является неизменяемым независимым снимком")
    void protectsFileList() {
        var files = new ArrayList<>(List.of("first.log", "second.log"));
        var result = new LogReadResult(files, 2);

        files.clear();
        assertThat(result.files()).containsExactly("first.log", "second.log");
        assertThatThrownBy(() -> result.files().add("third.log")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Количество прочитанных строк не может быть отрицательным")
    void rejectsNegativeLineCount() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LogReadResult(List.of("access.log"), -1));
    }
}
