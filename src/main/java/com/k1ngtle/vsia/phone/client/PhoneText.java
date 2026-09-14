package com.k1ngtle.vsia.phone.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class PhoneText {
    public static final ResourceLocation UI_FONT =
            new ResourceLocation("minecraft", "uniform");

    private PhoneText() {
    }

    public static Component component(String text) {
        String localized =
                PhoneI18n.translate(
                        text == null
                                ? ""
                                : text
                );

        return Component.literal(localized)
                .withStyle(
                        style -> style
                                .withFont(UI_FONT)
                                .withBold(
                                        PhoneAccessibilityClientPreferences.boldText()
                                )
                );
    }

    public static FormattedCharSequence sequence(String text) {
        return component(text).getVisualOrderText();
    }

    public static int width(Font font, String text) {
        return font.width(component(text));
    }

    public static void draw(
            GuiGraphics graphics,
            Font font,
            String text,
            int x,
            int y,
            int color
    ) {
        graphics.drawString(
                font,
                component(text),
                x,
                y,
                color,
                false
        );
    }

    public static void drawCentered(
            GuiGraphics graphics,
            Font font,
            String text,
            int centerX,
            int y,
            int color
    ) {
        Component component = component(text);
        int width = font.width(component);

        graphics.drawString(
                font,
                component,
                centerX - width / 2,
                y,
                color,
                false
        );
    }

    public static List<FormattedCharSequence> split(
            Font font,
            String text,
            int width
    ) {
        return font.split(
                component(text),
                width
        );
    }
}
