package academy.incidentradar.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Objects;

public final class JsonReportRenderer implements ReportRenderer {

    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
            .build();

    private final ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter()
            .withObjectIndenter(new DefaultIndenter("  ", "\n"))
            .withArrayIndenter(new DefaultIndenter("  ", "\n")));

    @Override
    public String render(ReportData report) {
        Objects.requireNonNull(report, "report");

        var root = mapper.createObjectNode();
        root.set("files", mapper.valueToTree(report.files()));
        root.set("period", mapper.valueToTree(report.period()));

        ObjectNode statistics = mapper.valueToTree(report.statistics());
        root.setAll(statistics);

        var comparison = report.comparison();
        if (comparison != null) {
            ObjectNode comparisonNode = mapper.valueToTree(comparison);
            comparisonNode.set("period", mapper.valueToTree(report.comparisonPeriod()));
            root.set("comparison", comparisonNode);
        }

        try {
            return writer.writeValueAsString(root) + "\n";
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось сформировать JSON-отчёт", exception);
        }
    }
}
