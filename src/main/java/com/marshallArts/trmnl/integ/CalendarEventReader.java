package com.marshallArts.trmnl.integ;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.stream.Stream;

public final class CalendarEventReader {
    public static Stream<Event> readCalendar(final String icsUrl) throws IOException, ParserException {
        final URL url = new URL(icsUrl);
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        final Calendar calendar = new CalendarBuilder().build(con.getInputStream());

        return calendar.getComponents(Component.VEVENT).stream()
                .map(VEvent.class::cast)
                .map(event -> new Event(
                        toInstant(event.getDateTimeStart().getDate(), Duration.ZERO),
                        toInstant(event.getDateTimeEnd().getDate(), Duration.ofHours(16)),
                        event.getSummary().getValue()
                ))
                .sorted();
    }

    private static Instant toInstant(final Temporal val, TemporalAmount offset) {
        if (val instanceof LocalDate) {
            return ((LocalDate) val).atStartOfDay().plus(offset).toInstant(ZoneOffset.UTC);
        } else if (val instanceof ZonedDateTime) {
            return ((ZonedDateTime) val).toInstant();
        }

        throw new IllegalStateException(val.getClass().getName());
    }

    public record Event(Instant start, Instant end, String summary) implements Comparable<Event> {
        @Override
        public int compareTo(final Event other) {
            return start.compareTo(other.start());
        }
    }
}
