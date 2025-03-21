package com.marshallArts.trmnl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.collect.ImmutableList;
import com.marshallArts.trmnl.integ.CalendarEventReader;
import com.marshallArts.trmnl.integ.KeeeyClient;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import static java.time.ZoneOffset.UTC;
import static java.util.function.Predicate.not;

@AllArgsConstructor
public final class LambdaActual implements RequestHandler<APIGatewayV2HTTPEvent, String> {
    private final ObjectMapper objectMapper;
    private final HandlerDelegate<GetEvent> getDelegate;

    public interface HandlerDelegate<IN> {
        String handle(final IN event, final APIGatewayV2HTTPEvent rawEvent, final Context context) throws Exception;
    }

    public record GetEvent() { }

    @Override
    @SneakyThrows
    public String handleRequest(final APIGatewayV2HTTPEvent event, final Context context) {
        context.getLogger().log(String.valueOf(event));
        return switch (event.getRequestContext().getHttp().getMethod()) {
            case "GET" -> getDelegate.handle(
                    objectMapper.readValue(event.getBody(), GetEvent.class),
                    event,
                    context
            );
            default -> throw new IllegalArgumentException("Unrecognised Event");
        };
    }

    @AllArgsConstructor
    public static final class GetActivityHandler implements HandlerDelegate<GetEvent> {
        record Target(String goal, String actual, Double completion) {
            private static Target from(com.marshallArts.trmnl.LambdaActual.Target from) {
                final String goal = from == null ? "0" : from.goal();
                final String actual = from == null ? "0" : from.actual();
                return new Target(
                        goal,
                        actual,
                        100 * Double.parseDouble(actual) / Double.parseDouble(goal)
                );
            }
        }
        record Mission(String name, Target measures) {
            public static Mission from(List<Conqueror> missions) {
                return missions.stream()
                        .max((o1, o2) -> o1.accept_date().compareTo(o2.accept_date()))
                        .map(c -> new Mission(
                                c.name(),
                                new Target(
                                        String.valueOf(c.target()),
                                        String.valueOf(c.travelled),
                                        100 * c.travelled() / c.target()
                                )
                        ))
                        .orElseThrow();
            }
        }
        record Rings(Target exercise, Target move, Target stand, Target steps, Mission mission) {}
        record Response(Rings daniel, Rings nicolle) {}

        private final KeeeyClient client;

        private final ObjectMapper objectMapper;

        @Override
        public String handle(
                final GetEvent event,
                final APIGatewayV2HTTPEvent rawEvent,
                final Context context)
                throws Exception {

            final TrmnlActivityEntry danielTrmnlActivity = client.getKey(
                    "daniel_trmnl_activity",
                    new TypeReference<TrmnlActivityEntry>() { }
            );

            final TrmnlActivityEntry nicolleTrmnlActivity = client.getKey(
                    "nicolle_trmnl_activity",
                    new TypeReference<TrmnlActivityEntry>() { }
            );

            final Response response = new Response(
                    new Rings(
                            Target.from(danielTrmnlActivity.exercise()),
                            Target.from(danielTrmnlActivity.move()),
                            Target.from(danielTrmnlActivity.stand()),
                            Target.from(danielTrmnlActivity.steps()),
                            Mission.from(danielTrmnlActivity.missions())
                    ),
                    new Rings(
                            Target.from(nicolleTrmnlActivity.exercise()),
                            Target.from(nicolleTrmnlActivity.move()),
                            Target.from(nicolleTrmnlActivity.stand()),
                            Target.from(nicolleTrmnlActivity.steps()),
                            Mission.from(nicolleTrmnlActivity.missions())
                    )
            );

            return objectMapper.writeValueAsString(response);
        }
    }

    record TrmnlActivityEntry(Target exercise, Target move, Target stand, Target steps, List<Conqueror> missions) {}
    record Target(String goal, String actual) {}
    record Conqueror(String name, String unit, Double travelled, Double target, @JsonDeserialize(converter = DateConverter.class) LocalDateTime complete_date, @JsonDeserialize(converter = DateConverter.class) LocalDateTime accept_date) {}
    private static final class DateConverter extends StdConverter<String, LocalDateTime> {

        @Override
        public LocalDateTime convert(final String value) {
            if (Objects.equals(value, "")) {
                return null;
            } else {
                return LocalDateTime.parse(value.replace(" ", "T"));
            }
        }
    }

    @AllArgsConstructor
    public static final class GetListsHandler implements HandlerDelegate<GetEvent> {

        record LeftRight(String left, String right) { }
        record Response(CalendarEvent school, DurationPretty year, LeftRight notes) { }

        private final KeeeyClient client;

        private final ObjectMapper objectMapper;

        @Override
        public String handle(
                final GetEvent event,
                final APIGatewayV2HTTPEvent rawEvent,
                final Context context)
                throws Exception {

            final JsonNode notesTrmnl = client.getKey(
                    "trmnl_notes",
                    new TypeReference<JsonNode>() { }
            );

            final List<String> note = Arrays.stream(
                        notesTrmnl.get("note")
                            .asText(null)
                            .split("<trmnl-break>")[0]
                            .split("\\\\n")
                    )
                    .map(s -> s.replace("\\t", " "))
                    .map(String::trim)
                    .filter(not(String::isEmpty))
                    .map(s -> {
                        if (s.startsWith("◦ ")) {
                            return "<div>" + s.replace("◦ ", "◦&nbsp;&nbsp;") + "</div>";
                        } else if (s.startsWith("✓ ")) {
                            return "<div>" + s.replace("✓ ", "✓&nbsp;") + "</div>";
                        } else {
                            return "<h4>" + s + "</h4>";
                        }
                    })
                    .toList();

            final Instant now = Instant.now();
            final CalendarEvent schoolTermDuration = CalendarEventReader.readCalendar("https://outlook.live.com/owa/calendar/00000000-0000-0000-0000-000000000000/2c99e8a4-0e69-4f05-b992-81fcecb26cf1/cid-78423E9DB633FF88/calendar.ics")
                    .filter(entry -> entry.end().compareTo(now) > 0)
                    .findFirst()
                    .map(entry -> {
                        if (entry.start().compareTo(now) < 0) {
                            final DurationPretty durationPretty = getDurationPretty(entry.start(), entry.end(), now);
                            return new CalendarEvent(
                                    durationPretty.label(),
                                    true,
                                    durationPretty.completionPercentage()
                            );
                        }

                        return new CalendarEvent("On Holidays", false, 0.0);
                    })
                    .orElse(new CalendarEvent("No Terms", false, 0.0));

            final LocalDateTime yearStart = LocalDateTime.of(
                    now.atOffset(UTC).getYear(),
                    1,
                    1,
                    0,
                    0
            );

            final DurationPretty yearDuration = getDurationPretty(
                    yearStart.toInstant(UTC),
                    yearStart.plusYears(1L).toInstant(UTC),
                    now
            );

            final Response response = new Response(
                    schoolTermDuration,
                    yearDuration,
                    new LeftRight(
                            String.join("\n", note.subList(0, 6)),
                            String.join("\n", note.subList(6, note.size()))
                    )
            );

            return objectMapper.writeValueAsString(response);
        }

        record DurationPretty(String label, double completionPercentage) { }
        private static DurationPretty getDurationPretty(final Instant start, final Instant end, final Instant now) {
            final Duration termDuration = Duration.between(start, end);
            final Duration remainingDuration = Duration.between(now, end);
            final double completion = 100 - ((double) 100 * remainingDuration.toDays() / termDuration.toDays());

            final ImmutableList.Builder<String> result = ImmutableList.builder();
            if (remainingDuration.toDays() > 7) {
                final long weeks = remainingDuration.toDays() / 7
                        + (remainingDuration.toDays() % 7 == 0 ? 0 : 1);
                result.add(String.valueOf(weeks));
                result.add("Week" + (weeks > 1 ? "s" : ""));
            } else {
                final long days = remainingDuration.toDays();
                result.add(String.valueOf(days));
                result.add("Day" + (days > 1 ? "s" : ""));
            }

            result.add("To Go");
            final String label = String.join(" ", result.build());
            return new DurationPretty(label, completion);
        }
    }

    record CalendarEvent(String label, Boolean active, Double completionPercentage) { }
}