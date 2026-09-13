package com.k1ngtle.vsia.phone.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class PhoneNotificationManager {
    private static final PhoneNotificationManager INSTANCE =
            new PhoneNotificationManager();

    private static final long BANNER_LIFETIME_MS = 4500L;
    private static final int HISTORY_LIMIT = 32;

    private final Deque<PhoneNotification> history =
            new ArrayDeque<>();

    private volatile PhoneNotification banner;
    private volatile long bannerStartedAt;

    private PhoneNotificationManager() {
    }

    public static PhoneNotificationManager get() {
        return INSTANCE;
    }

    public void postMessage(
            String sender,
            String body
    ) {
        if (!PhoneNotificationSettings.messagesNotificationsEnabled()) {
            return;
        }

        String safeSender = sender == null || sender.isBlank()
                ? "Unknown"
                : sender;

        String safeBody = body == null
                ? ""
                : body;

        PhoneNotification notification =
                new PhoneNotification(
                        "Messages",
                        safeSender,
                        safeBody,
                        System.currentTimeMillis()
                );

        addHistory(notification);

        if (PhoneNotificationSettings.messagesSilencedByFocus()) {
            return;
        }

        banner = notification;
        bannerStartedAt = System.currentTimeMillis();

        playAlert();
    }

    public void previewAlert() {
        playAlert();
    }

    public List<PhoneNotification> history() {
        return List.copyOf(history);
    }

    public int historyCount() {
        return history.size();
    }

    public void clearHistory() {
        history.clear();
    }

    public void renderBanner(
            GuiGraphics graphics,
            Font font,
            int phoneX,
            int phoneY,
            int phoneWidth
    ) {
        PhoneNotification current = banner;
        if (current == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long age = now - bannerStartedAt;

        if (age < 0L || age > BANNER_LIFETIME_MS) {
            banner = null;
            return;
        }

        int nudge = 0;
        if (PhoneNotificationSettings.hapticsEnabled()
                && age < 180L) {
            nudge = ((age / 45L) % 2L == 0L) ? -1 : 1;
        }

        int x = phoneX + 15 + nudge;
        int y = phoneY + 34;
        int width = phoneWidth - 30;
        int height = 49;

        roundedRect(
                graphics,
                x,
                y,
                width,
                height,
                13,
                0xF229292D
        );

        PhoneText.draw(
                graphics,
                font,
                current.app(),
                x + 12,
                y + 8,
                0xFFFFFFFF
        );

        String preview = PhoneNotificationSettings.showPreviews()
                ? current.sender() + ": " + current.body()
                : "New Message";

        PhoneText.draw(
                graphics,
                font,
                fit(font, preview, width - 24),
                x + 12,
                y + 26,
                0xFFCFD0D4
        );
    }

    private void addHistory(
            PhoneNotification notification
    ) {
        history.addFirst(notification);

        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    private void playAlert() {
        if (!PhoneNotificationSettings.alertSoundEnabled()) {
            return;
        }

        float volume = PhoneNotificationSettings.alertVolume();
        if (volume <= 0.0F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null
                || minecraft.getSoundManager() == null) {
            return;
        }

        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(
                        SoundEvents.NOTE_BLOCK_BELL.value(),
                        1.35F,
                        volume
                )
        );
    }

    private static String fit(
            Font font,
            String value,
            int maxWidth
    ) {
        String text = value == null
                ? ""
                : value;

        if (font.width(PhoneText.component(text)) <= maxWidth) {
            return text;
        }

        while (!text.isEmpty()
                && font.width(
                PhoneText.component(text + "...")
        ) > maxWidth) {
            text = text.substring(
                    0,
                    text.length() - 1
            );
        }

        return text.isEmpty()
                ? "..."
                : text + "...";
    }

    private static void roundedRect(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        graphics.fill(
                x + radius,
                y,
                x + width - radius,
                y + height,
                color
        );

        graphics.fill(
                x,
                y + radius,
                x + width,
                y + height - radius,
                color
        );

        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int inset = (int) Math.ceil(
                    radius - Math.sqrt(
                            Math.max(
                                    0,
                                    radius * radius
                                            - dy * dy
                            )
                    )
            );

            graphics.fill(
                    x + inset,
                    y + i,
                    x + width - inset,
                    y + i + 1,
                    color
            );

            graphics.fill(
                    x + inset,
                    y + height - i - 1,
                    x + width - inset,
                    y + height - i,
                    color
            );
        }
    }

    public record PhoneNotification(
            String app,
            String sender,
            String body,
            long timestampMillis
    ) {
    }
}
