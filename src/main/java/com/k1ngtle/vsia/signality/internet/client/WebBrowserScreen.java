package com.k1ngtle.vsia.signality.internet.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Client UI where players can browse the internet, or open the developer
 * tools to code their own HTML, CSS, and JS servers.
 *
 * NOTE: For actual execution and rendering of complex HTML/CSS/JS, this screen
 * acts as a wrapper for MCEF (Minecraft Chromium Embedded Framework).
 * MCEF handles creating a browser instance over the OpenGL context.
 */
public class WebBrowserScreen extends Screen {

    private EditBox urlBar;
    private EditBox codeEditor; // For server owners coding their sites
    private boolean developerMode = false;

    // The downloaded HTML data from the Signality Network
    private String currentHtml = "";

    public WebBrowserScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        // URL Bar
        this.urlBar = new EditBox(this.font, 20, 10, this.width - 100, 20, Component.literal("URL"));
        this.urlBar.setMaxLength(256);
        this.addRenderableWidget(this.urlBar);

        // Go Button (Sends L7 DNS Request -> L7 HTTP Request via Signality)
        this.addRenderableWidget(Button.builder(Component.literal("GO"), button -> {
            navigate(this.urlBar.getValue());
        }).bounds(this.width - 75, 10, 55, 20).build());

        // Toggle Developer / Coding Mode
        this.addRenderableWidget(Button.builder(Component.literal("DEV"), button -> {
            this.developerMode = !this.developerMode;
            this.codeEditor.visible = this.developerMode;
        }).bounds(this.width - 75, this.height - 30, 55, 20).build());

        // Code Editor for writing HTML/CSS/JS to the server
        this.codeEditor = new EditBox(this.font, 20, 40, this.width - 40, this.height - 80, Component.literal("Code"));
        this.codeEditor.setMaxLength(10000);
        this.codeEditor.visible = false;
        this.addRenderableWidget(this.codeEditor);
    }

    private void navigate(String url) {
        // 1. Player requests a URL.
        // 2. Client sends OSINetworkPacket (Port 53) to DNS Server via Signality.
        // 3. DNS Server replies with Target IP.
        // 4. Client sends OSINetworkPacket (Port 80) to the Target IP.
        // 5. Web Server replies with HTML/CSS/JS Payload.
        // 6. MCEF (Chromium) parses the string and renders the WebGL texture!

        this.currentHtml = "<h1>Loading...</h1>";

        // Example Hook for MCEF Integration:
        // MCEFBrowser browser = MCEF.createBrowser(url);
        // browser.loadHtml(this.currentHtml);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (developerMode) {
            guiGraphics.drawString(this.font, "Server Code Editor (HTML/CSS/JS)", 20, 30, 0x00FF00, false);
        } else {
            // Placeholder text renderer if MCEF is not installed
            guiGraphics.fill(20, 40, this.width - 20, this.height - 40, 0xFFFFFFFF);

            // Draw parsed HTML (Simplified)
            if (!currentHtml.isEmpty()) {
                guiGraphics.drawString(this.font, currentHtml, 25, 45, 0x000000, false);
            }
        }
    }
}