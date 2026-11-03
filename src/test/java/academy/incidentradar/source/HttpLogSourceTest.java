package academy.incidentradar.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.incidentradar.support.TestHttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Дополнительные проверки HTTP-источника. */
@DisplayName("HTTP-источник логов")
class HttpLogSourceTest {

    @Test
    @DisplayName("Query-параметры URL не меняют расширение файла")
    void queryDoesNotChangeExtension() throws IOException {
        try (var server = new TestHttpServer(200, "entry\n")) {
            var uri = URI.create(server.uri() + "?download=true");
            var sources = new LogSourceResolver().resolve(uri.toString());
            var lines = new ArrayList<String>();

            var result = new LogReader().read(sources, lines::add);

            assertThat(result.files()).containsExactly(uri.toString());
            assertThat(result.linesRead()).isEqualTo(1);
            assertThat(lines).containsExactly("entry");
        }
    }

    @Test
    @DisplayName("Ошибка обработчика не превращается в ошибку HTTP-источника")
    void preservesActionFailure() throws IOException {
        try (var server = new TestHttpServer(200, "first\nsecond\n")) {
            var failure = new UncheckedIOException(new IOException("Тестовый сбой обработчика"));

            assertThatThrownBy(() -> new HttpLogSource(server.uri()).forEachLine(line -> {
                        throw failure;
                    }))
                    .isSameAs(failure);
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ftp://example.invalid/access.log",
                "http:/access.log",
                "http://example.invalid/access.csv",
                "http://example.invalid:70000/access.log"
            })
    @DisplayName("Недопустимый HTTP-источник отклоняется до сетевого обращения")
    void rejectsInvalidUri(String value) {
        var uri = URI.create(value);

        assertThatThrownBy(() -> new HttpLogSource(uri)).isInstanceOf(LogSourceException.class);
    }
}
