package academy.incidentradar.report;

import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.LogStatistics;
import academy.incidentradar.model.PeriodComparison.MetricChange;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class AsciiDocReportRenderer implements ReportRenderer {

    private static final Locale RUSSIAN = Locale.forLanguageTag("ru");

    @Override
    public String render(ReportData report) {
        Objects.requireNonNull(report, "report");
        var output = new StringBuilder("= Отчёт Incident Radar\n\n");

        var sources = report.files().stream().map(List::of).toList();
        appendTable(output, "== Источники", List.of("Источник"), sources);
        appendPeriod(output, "Основной период", report.period(), report.statistics());

        var comparison = report.comparison();
        if (comparison != null) {
            appendPeriod(
                    output,
                    "Период сравнения",
                    Objects.requireNonNull(report.comparisonPeriod()),
                    comparison.referenceStatistics());
            appendTable(
                    output,
                    "== Изменения относительно периода сравнения",
                    List.of("Метрика", "Разница", "Изменение"),
                    List.of(
                            changeRow("Количество запросов", comparison.requestsChange()),
                            changeRow("Средний размер ответа, байт", comparison.averageSizeChange()),
                            changeRow("Максимальный размер ответа, байт", comparison.maxSizeChange()),
                            changeRow("p95, байт", comparison.p95SizeChange())));
        }

        return output.toString();
    }

    private static void appendPeriod(StringBuilder output, String title, DateRange period, LogStatistics statistics) {
        output.append("== ").append(title).append("\n\n");
        appendTable(
                output,
                "=== Общая статистика",
                List.of("Метрика", "Значение"),
                List.of(
                        List.of("Начальная дата", date(period.from())),
                        List.of("Конечная дата", date(period.to())),
                        List.of("Количество запросов", Long.toString(statistics.totalRequestsCount())),
                        List.of(
                                "Средний размер ответа, байт",
                                statistics.responseSizeInBytes().average().toPlainString()),
                        List.of(
                                "Максимальный размер ответа, байт",
                                Long.toString(statistics.responseSizeInBytes().max())),
                        List.of(
                                "p95, байт",
                                Long.toString(statistics.responseSizeInBytes().p95()))));

        var resources = statistics.resources().stream()
                .map(resource -> List.of(resource.resource(), Long.toString(resource.totalRequestsCount())))
                .toList();
        appendTable(output, "=== Топ-10 ресурсов", List.of("Ресурс", "Запросов"), resources);

        var codes = statistics.responseCodes().entrySet().stream()
                .map(entry -> List.of(
                        HttpStatusNames.label(entry.getKey()), entry.getValue().toString()))
                .toList();
        appendTable(output, "=== Коды ответа", List.of("Код", "Запросов"), codes);

        var weekdays = statistics.requestsByWeekday().entrySet().stream()
                .map(entry -> List.of(
                        entry.getKey().getDisplayName(TextStyle.FULL, RUSSIAN),
                        entry.getValue().toString()))
                .toList();
        appendTable(output, "=== Дни недели", List.of("День", "Запросов"), weekdays);

        var protocols = statistics.uniqueProtocols().stream().map(List::of).toList();
        appendTable(output, "=== Уникальные протоколы", List.of("Протокол"), protocols);
    }

    private static List<String> changeRow(String metric, MetricChange change) {
        var percentage = change.percentage();
        return List.of(
                metric,
                change.difference().toPlainString(),
                percentage == null ? "н/д" : percentage.toPlainString() + "%");
    }

    private static String date(@Nullable LocalDate value) {
        return value == null ? "Не ограничено" : value.toString();
    }

    private static void appendTable(
            StringBuilder output, String heading, List<String> columns, List<List<String>> rows) {
        output.append(heading).append("\n\n");
        output.append("[cols=\"").append(columns.size()).append("*\",options=\"header\"]\n|===\n");
        appendRow(output, columns);
        output.append('\n');
        if (rows.isEmpty()) {
            appendRow(output, Collections.nCopies(columns.size(), "—"));
        }
        rows.forEach(row -> appendRow(output, row));
        output.append("|===\n\n");
    }

    private static void appendRow(StringBuilder output, List<String> cells) {
        var encoded = cells.stream().map(cell -> "|" + escape(cell)).toList();
        output.append(String.join(" ", encoded)).append('\n');
    }

    private static String escape(String value) {
        var escaped = value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("+", "&#43;")
                .replace("|", "&#124;")
                .replace("\r", "&#13;")
                .replace("\n", "&#10;")
                .replace("\t", "&#9;");
        return escaped.isEmpty() ? "" : "+++" + escaped + "+++";
    }
}
