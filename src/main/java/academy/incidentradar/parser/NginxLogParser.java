package academy.incidentradar.parser;

import academy.incidentradar.model.LogRecord;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Разбор строки NGINX-лога в проверенную модель. */
public final class NginxLogParser {

    private static final Pattern LOG_LINE = Pattern.compile("^(?<remoteAddress>\\S+) - (?<remoteUser>\\S+) "
            + "\\[(?<timestamp>[^]]+)] "
            + "\"(?<method>[^\\s\"]+) (?<resource>[^\\s\"]+) (?<protocol>[^\\s\"]+)\" "
            + "(?<status>[0-9]{3}) (?<bodyBytesSent>[0-9]+) "
            + "\"(?<referer>[^\"\\r\\n]*)\" \"(?<userAgent>[^\"\\r\\n]*)\"$");

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(
                    "dd/MMM/uuuu:HH:mm:ss xx", Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);

    public Optional<LogRecord> parse(String line) {
        Objects.requireNonNull(line, "line");
        var matcher = LOG_LINE.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        var remoteAddress = requiredGroup(matcher, "remoteAddress");
        var remoteUser = requiredGroup(matcher, "remoteUser");
        var rawTimestamp = requiredGroup(matcher, "timestamp");
        var method = requiredGroup(matcher, "method");
        var resource = requiredGroup(matcher, "resource");
        var protocol = requiredGroup(matcher, "protocol");
        var rawStatus = requiredGroup(matcher, "status");
        var rawSize = requiredGroup(matcher, "bodyBytesSent");
        var referer = requiredGroup(matcher, "referer");
        var userAgent = requiredGroup(matcher, "userAgent");

        try {
            return Optional.of(new LogRecord(
                    remoteAddress,
                    remoteUser,
                    OffsetDateTime.parse(rawTimestamp, TIMESTAMP_FORMAT),
                    method,
                    resource,
                    protocol,
                    Integer.parseInt(rawStatus),
                    Long.parseLong(rawSize),
                    referer,
                    userAgent));
        } catch (DateTimeParseException | IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static String requiredGroup(Matcher matcher, String name) {
        return Objects.requireNonNull(matcher.group(name), name);
    }
}
