package com.k1ngtle.vsia.phone.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

public final class PhonePersonalAppsState {
    public record Note(
            long id,
            String title,
            String body,
            long modifiedAt
    ) {
    }

    public record CalendarEvent(
            long id,
            long epochDay,
            int hour,
            int minute,
            String title
    ) {
    }

    public record Reminder(
            long id,
            String title,
            boolean completed
    ) {
    }

    public record Photo(
            long id,
            long capturedAt,
            String dimension,
            int x,
            int y,
            int z,
            float yaw,
            boolean frontCamera,
            int zoom
    ) {
    }

    private static final List<Note> NOTES =
            new ArrayList<>();

    private static final List<CalendarEvent> EVENTS =
            new ArrayList<>();

    private static final List<Reminder> REMINDERS =
            new ArrayList<>();

    private static final List<Photo> PHOTOS =
            new ArrayList<>();

    private static boolean loaded;
    private static long nextId = 1L;

    private PhonePersonalAppsState() {
    }

    public static synchronized List<Note> notes() {
        ensureLoaded();

        return NOTES.stream()
                .sorted(
                        Comparator.comparingLong(
                                Note::modifiedAt
                        ).reversed()
                )
                .toList();
    }

    public static synchronized Note note(long id) {
        ensureLoaded();

        for (Note note : NOTES) {
            if (note.id() == id) {
                return note;
            }
        }

        return null;
    }

    public static synchronized long saveNote(
            long id,
            String title,
            String body
    ) {
        ensureLoaded();

        String safeTitle =
                sanitizeLine(
                        title == null || title.isBlank()
                                ? PhoneI18n.translate("New Note")
                                : title.trim()
                );

        String safeBody =
                body == null
                        ? ""
                        : body;

        long now =
                System.currentTimeMillis();

        if (id <= 0L) {
            id = nextId++;
        }

        long finalId = id;
        NOTES.removeIf(
                note -> note.id() == finalId
        );

        NOTES.add(
                new Note(
                        id,
                        safeTitle,
                        safeBody,
                        now
                )
        );

        save();
        return id;
    }

    public static synchronized void deleteNote(long id) {
        ensureLoaded();

        NOTES.removeIf(
                note -> note.id() == id
        );

        save();
    }

    public static synchronized List<CalendarEvent> eventsFor(
            LocalDate date
    ) {
        ensureLoaded();

        long day =
                (date == null
                        ? PhoneLocaleSettings.currentDate()
                        : date)
                        .toEpochDay();

        return EVENTS.stream()
                .filter(
                        event -> event.epochDay() == day
                )
                .sorted(
                        Comparator
                                .comparingInt(
                                        CalendarEvent::hour
                                )
                                .thenComparingInt(
                                        CalendarEvent::minute
                                )
                )
                .toList();
    }

    public static synchronized CalendarEvent event(long id) {
        ensureLoaded();

        for (CalendarEvent event : EVENTS) {
            if (event.id() == id) {
                return event;
            }
        }

        return null;
    }

    public static synchronized long saveEvent(
            long id,
            LocalDate date,
            int hour,
            int minute,
            String title
    ) {
        ensureLoaded();

        LocalDate safeDate =
                date == null
                        ? PhoneLocaleSettings.currentDate()
                        : date;

        int safeHour =
                Math.max(
                        0,
                        Math.min(
                                23,
                                hour
                        )
                );

        int safeMinute =
                Math.max(
                        0,
                        Math.min(
                                59,
                                minute
                        )
                );

        String safeTitle =
                sanitizeLine(
                        title == null || title.isBlank()
                                ? PhoneI18n.translate("New Event")
                                : title.trim()
                );

        if (id <= 0L) {
            id = nextId++;
        }

        long finalId = id;
        EVENTS.removeIf(
                event -> event.id() == finalId
        );

        EVENTS.add(
                new CalendarEvent(
                        id,
                        safeDate.toEpochDay(),
                        safeHour,
                        safeMinute,
                        safeTitle
                )
        );

        save();
        return id;
    }

    public static synchronized void deleteEvent(long id) {
        ensureLoaded();

        EVENTS.removeIf(
                event -> event.id() == id
        );

        save();
    }

    public static synchronized List<Reminder> reminders() {
        ensureLoaded();
        return List.copyOf(REMINDERS);
    }

    public static synchronized long addReminder(String title) {
        ensureLoaded();

        String safeTitle =
                sanitizeLine(
                        title == null || title.isBlank()
                                ? PhoneI18n.translate("New Reminder")
                                : title.trim()
                );

        long id = nextId++;

        REMINDERS.add(
                new Reminder(
                        id,
                        safeTitle,
                        false
                )
        );

        save();
        return id;
    }

    public static synchronized void toggleReminder(long id) {
        ensureLoaded();

        for (int i = 0; i < REMINDERS.size(); i++) {
            Reminder current =
                    REMINDERS.get(i);

            if (current.id() == id) {
                REMINDERS.set(
                        i,
                        new Reminder(
                                current.id(),
                                current.title(),
                                !current.completed()
                        )
                );

                save();
                return;
            }
        }
    }

    public static synchronized void deleteReminder(long id) {
        ensureLoaded();

        REMINDERS.removeIf(
                reminder -> reminder.id() == id
        );

        save();
    }

    public static synchronized void clearCompletedReminders() {
        ensureLoaded();

        REMINDERS.removeIf(
                Reminder::completed
        );

        save();
    }

    public static synchronized List<Photo> photos() {
        ensureLoaded();

        return PHOTOS.stream()
                .sorted(
                        Comparator.comparingLong(
                                Photo::capturedAt
                        ).reversed()
                )
                .toList();
    }

    public static synchronized Photo capturePhoto(
            boolean frontCamera,
            int zoom
    ) {
        ensureLoaded();

        Minecraft minecraft =
                Minecraft.getInstance();

        String dimension = "unknown";
        int x = 0;
        int y = 0;
        int z = 0;
        float yaw = 0.0F;

        if (minecraft.level != null) {
            dimension =
                    minecraft.level
                            .dimension()
                            .location()
                            .toString();
        }

        if (minecraft.player != null) {
            BlockPos pos =
                    minecraft.player
                            .blockPosition();

            x = pos.getX();
            y = pos.getY();
            z = pos.getZ();
            yaw = minecraft.player.getYRot();
        }

        Photo photo =
                new Photo(
                        nextId++,
                        System.currentTimeMillis(),
                        dimension,
                        x,
                        y,
                        z,
                        yaw,
                        frontCamera,
                        Math.max(
                                1,
                                Math.min(
                                        4,
                                        zoom
                                )
                        )
                );

        PHOTOS.add(photo);
        save();

        return photo;
    }

    public static synchronized void deletePhoto(long id) {
        ensureLoaded();

        PHOTOS.removeIf(
                photo -> photo.id() == id
        );

        save();
    }

    public static synchronized void clearAll() {
        ensureLoaded();

        NOTES.clear();
        EVENTS.clear();
        REMINDERS.clear();
        PHOTOS.clear();

        save();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;

        Path path =
                storagePath();

        if (!Files.exists(path)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(
                    path,
                    StandardCharsets.UTF_8
            )) {
                parseLine(line);
            }
        } catch (IOException ignored) {
        }
    }

    private static void parseLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }

        try {
            String[] parts =
                    line.split(
                            "\\|",
                            -1
                    );

            switch (parts[0]) {
                case "N" -> {
                    long id =
                            Long.parseLong(parts[1]);

                    NOTES.add(
                            new Note(
                                    id,
                                    decode(parts[3]),
                                    decode(parts[4]),
                                    Long.parseLong(parts[2])
                            )
                    );

                    nextId =
                            Math.max(
                                    nextId,
                                    id + 1L
                            );
                }

                case "E" -> {
                    long id =
                            Long.parseLong(parts[1]);

                    EVENTS.add(
                            new CalendarEvent(
                                    id,
                                    Long.parseLong(parts[2]),
                                    Integer.parseInt(parts[3]),
                                    Integer.parseInt(parts[4]),
                                    decode(parts[5])
                            )
                    );

                    nextId =
                            Math.max(
                                    nextId,
                                    id + 1L
                            );
                }

                case "R" -> {
                    long id =
                            Long.parseLong(parts[1]);

                    REMINDERS.add(
                            new Reminder(
                                    id,
                                    decode(parts[3]),
                                    Boolean.parseBoolean(parts[2])
                            )
                    );

                    nextId =
                            Math.max(
                                    nextId,
                                    id + 1L
                            );
                }

                case "P" -> {
                    long id =
                            Long.parseLong(parts[1]);

                    PHOTOS.add(
                            new Photo(
                                    id,
                                    Long.parseLong(parts[2]),
                                    decode(parts[3]),
                                    Integer.parseInt(parts[4]),
                                    Integer.parseInt(parts[5]),
                                    Integer.parseInt(parts[6]),
                                    Float.parseFloat(parts[7]),
                                    Boolean.parseBoolean(parts[8]),
                                    Integer.parseInt(parts[9])
                            )
                    );

                    nextId =
                            Math.max(
                                    nextId,
                                    id + 1L
                            );
                }

                default -> {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        Path path =
                storagePath();

        try {
            Files.createDirectories(
                    path.getParent()
            );

            List<String> lines =
                    new ArrayList<>();

            for (Note note : NOTES) {
                lines.add(
                        "N|"
                                + note.id()
                                + "|"
                                + note.modifiedAt()
                                + "|"
                                + encode(note.title())
                                + "|"
                                + encode(note.body())
                );
            }

            for (CalendarEvent event : EVENTS) {
                lines.add(
                        "E|"
                                + event.id()
                                + "|"
                                + event.epochDay()
                                + "|"
                                + event.hour()
                                + "|"
                                + event.minute()
                                + "|"
                                + encode(event.title())
                );
            }

            for (Reminder reminder : REMINDERS) {
                lines.add(
                        "R|"
                                + reminder.id()
                                + "|"
                                + reminder.completed()
                                + "|"
                                + encode(reminder.title())
                );
            }

            for (Photo photo : PHOTOS) {
                lines.add(
                        "P|"
                                + photo.id()
                                + "|"
                                + photo.capturedAt()
                                + "|"
                                + encode(photo.dimension())
                                + "|"
                                + photo.x()
                                + "|"
                                + photo.y()
                                + "|"
                                + photo.z()
                                + "|"
                                + photo.yaw()
                                + "|"
                                + photo.frontCamera()
                                + "|"
                                + photo.zoom()
                );
            }

            Files.write(
                    path,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ignored) {
        }
    }

    private static Path storagePath() {
        Minecraft minecraft =
                Minecraft.getInstance();

        return minecraft.gameDirectory
                .toPath()
                .resolve("config")
                .resolve("vsia")
                .resolve("phone_personal_apps.dat");
    }

    private static String encode(String value) {
        return Base64
                .getEncoder()
                .encodeToString(
                        (value == null
                                ? ""
                                : value)
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );
    }

    private static String decode(String value) {
        return new String(
                Base64
                        .getDecoder()
                        .decode(value),
                StandardCharsets.UTF_8
        );
    }

    private static String sanitizeLine(String value) {
        return value
                .replace(
                        '\n',
                        ' '
                )
                .replace(
                        '\r',
                        ' '
                );
    }
}
