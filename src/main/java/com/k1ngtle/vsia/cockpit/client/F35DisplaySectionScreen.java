package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35SeatController;
import com.k1ngtle.vsia.cockpit.detection.F35DetectionContact;
import com.k1ngtle.vsia.cockpit.detection.F35DetectionType;
import com.k1ngtle.vsia.cockpit.display.F35DisplayState;
import com.k1ngtle.vsia.cockpit.display.F35DisplayStateFactory;
import com.k1ngtle.vsia.cockpit.display.F35RadarTrackView;
import com.k1ngtle.vsia.cockpit.display.F35TrackTrailCache;
import com.k1ngtle.vsia.cockpit.display.stores.F35StoresSnapshot;
import com.k1ngtle.vsia.cockpit.display.telemetry.AircraftTelemetry;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import com.k1ngtle.vsia.cockpit.iff.F35IffClientState;
import com.k1ngtle.vsia.cockpit.network.C2SF35IffActionPacket;
import com.k1ngtle.vsia.cockpit.network.C2SF35IffRequestPacket;
import com.k1ngtle.vsia.network.VsiaNetwork;
import java.time.Instant;

public final class F35DisplaySectionScreen extends Screen {
    private static final float VIRTUAL_WIDTH = 860.0F;
    private static final float VIRTUAL_HEIGHT = 400.0F;

    private static final int BLACK = 0xF4070A0A;
    private static final int PANEL = 0xEE111616;
    private static final int GRID = 0xFF66716E;
    private static final int GREEN = 0xFFA0E0A0;
    private static final int CYAN = 0xFF78C8C8;
    private static final int MAGENTA = 0xFFA830F8;
    private static final int WHITE = 0xFFF0F0F0;
    private static final int DIM = 0xFF59615F;
    private static final int RED = 0xFFE05858;
    private static final int AMBER = 0xFFE8C860;

    private int section;
    private UUID cockpitId;
    private float uiScale = 1.0F;
    private float uiLeft;
    private float uiTop;
    private F35DisplayState state;
    private final List<HitTarget> hitTargets = new ArrayList<>();

    public F35DisplaySectionScreen(int section) {
        super(Component.literal("F-35 COCKPIT DISPLAY"));
        this.section = clampSection(section);
    }

    @Override
    protected void init() {
        if (!bindCockpit()) return;
        F35DisplayClientConfig.ensureLoaded();
        updateGeometry();
        if (section == 1) requestIff();
    }

    private boolean bindCockpit() {
        F35CockpitSeatBlockEntity seated = F35CockpitClientContext.seated();
        if (cockpitId == null && seated != null) cockpitId = seated.cockpitId();
        return F35CockpitClientContext.bindSeat(cockpitId);
    }

    private void requestIff() {
        if (bindCockpit()) VsiaNetwork.sendToServer(new C2SF35IffRequestPacket(cockpitId));
    }

    private void setSection(int value) {
        section = value;
        if (section == 1) requestIff();
    }

    @Override
    public void tick() {
        if (!bindCockpit()) { onClose(); return; }
        if (section == 1 && minecraft.player.tickCount % 20 == 0) requestIff();
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (!bindCockpit()) {
            graphics.fill(0, 0, width, height, 0xB0000000);
            graphics.drawString(font, "Waiting for this cockpit. Reopen if you changed seats.", 20, 20, AMBER);
            return;
        }
        updateGeometry();
        state = captureState(partialTick);
        hitTargets.clear();

        if (state != null && state.ownship().shipDetected()) {
            F35TrackTrailCache.update(state);
        }

        graphics.fill(0, 0, width, height, 0xB0000000);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(uiLeft, uiTop, 0.0F);
        pose.scale(uiScale, uiScale, 1.0F);

        graphics.fill(0, 0, (int) VIRTUAL_WIDTH, (int) VIRTUAL_HEIGHT, BLACK);
        drawFrame(graphics);
        drawTabs(graphics);

        if (state == null) {
            drawNoCockpit(graphics);
        } else if (section == 1) {
            drawIffPage(graphics);
        } else if (!state.ownship().shipDetected()) {
            drawNoShip(graphics);
        } else {
            switch (section) {
                case 1 -> drawIffPage(graphics);
                case 2 -> drawSensorPage(graphics, state);
                case 3 -> drawTsdPage(graphics, state);
                default -> drawHsiPage(graphics, state);
            }
        }

        if(section>1&&state!=null){
            F35RadarTrackView locked=F35TargetLockClient.lockedRadarTrack(state.tracks());
            F35DetectionContact contact=locked==null?F35TargetLockClient.lockedDetection(state.detections()):null;
            String data=locked!=null&&locked.iffAuthenticated()?locked.iffTelemetry():(contact!=null&&contact.iffAuthenticated()?contact.iffTelemetry():"");
            if(!data.isBlank())graphics.drawWordWrap(font,Component.literal("VERIFIED "+data),28,369,800,GREEN);
        }
        pose.popPose();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (!bindCockpit()) return false;
        double x = (mouseX - uiLeft) / uiScale;
        double y = (mouseY - uiTop) / uiScale;

        if (!inside(x, y, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT)) {
            return false;
        }

        if (button == 1 && section >= 2) {
            F35TargetLockClient.clear();
            return true;
        }

        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (y >= 34.0 && y <= 64.0) {
            if (x >= 20.0 && x <= 215.0) {
                setSection(1);
                return true;
            }
            if (x >= 225.0 && x <= 420.0) {
                setSection(2);
                return true;
            }
            if (x >= 430.0 && x <= 625.0) {
                setSection(3);
                return true;
            }
            if (x >= 635.0 && x <= 830.0) {
                setSection(4);
                return true;
            }
        }

        if (section >= 2) {
            HitTarget hit = nearestHit((float) x, (float) y, 13.0F);
            if (hit != null) {
                if (hit.kind == TargetKind.RADAR) {
                    F35TargetLockClient.toggleRadar(hit.id);
                } else {
                    F35TargetLockClient.toggleDetection(hit.id);
                }
                return true;
            }
        }

        if (section == 1) {
            int action = iffActionAt(x, y);
            if(action==32){minecraft.setScreen(new F35IffKeyScreen(this,cockpitId));return true;}
            if (action >= 0 && !F35IffClientState.get(cockpitId).status().equals("WAIT")) {
                VsiaNetwork.sendToServer(new C2SF35IffActionPacket(cockpitId, action, "MISSION-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase())); return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (!bindCockpit()) return false;
        int requestedSection = sectionForKey(keyCode);

        if (requestedSection > 0) {
            setSection(requestedSection);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE
                || keyCode == GLFW.GLFW_KEY_DELETE) {
            F35TargetLockClient.clear();
            return true;
        }

        if (F35DisplayKeyMappings.CONFIGURE_DISPLAY.matches(keyCode, scanCode)) {
            if (minecraft != null) {
                minecraft.setScreen(new F35DisplayConfigScreen());
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static int sectionForKey(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_KP_1 -> 1;
            case GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_KP_2 -> 2;
            case GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_KP_3 -> 3;
            case GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_KP_4 -> 4;
            default -> 0;
        };
    }

    private F35DisplayState captureState(float partialTick) {
        if (minecraft == null
                || minecraft.player == null
                || minecraft.level == null
                || !minecraft.player.isPassenger()) {
            return null;
        }

        Entity vehicle = minecraft.player.getVehicle();

        if (!F35SeatController.isSeatRenderMarker(vehicle)) {
            return null;
        }

        BlockPos cockpitPos =
                F35SeatController.clientCockpitPos(vehicle);

        if (cockpitPos == null) {
            return null;
        }

        BlockEntity blockEntity =
                minecraft.level.getBlockEntity(cockpitPos);

        if (!(blockEntity instanceof F35CockpitSeatBlockEntity cockpit)) {
            return null;
        }

        return F35DisplayStateFactory.capture(
                cockpit,
                partialTick
        );
    }

    private void drawFrame(GuiGraphics graphics) {
        rect(graphics, 8, 8, 844, 384, GRID);
        lineH(graphics, 8, 852, 72, GRID);
        text(graphics, "F-35 COCKPIT " + F35CockpitClientContext.label(cockpitId), 20, 14, GREEN);
        text(graphics, "CLICK CONTACT = LOCK   RIGHT CLICK / DEL = UNLOCK   \\ = CONFIG", 430, 14, CYAN);
    }

    private void drawTabs(GuiGraphics graphics) {
        tab(graphics, 20, 34, 195, "1  IFF / XPDR", section == 1);
        tab(graphics, 225, 34, 195, "2  SENSOR / RADAR", section == 2);
        tab(graphics, 430, 34, 195, "3  TSD1", section == 3);
        tab(graphics, 635, 34, 195, "4  TSD2 / HSI", section == 4);
    }

    private void drawNoCockpit(GuiGraphics graphics) {
        text(graphics, "COCKPIT DATA LINK WAIT", 322, 190, AMBER);
        text(graphics, "If this is the first tick after updating, dismount and sit once again.", 255, 210, DIM);
    }

    private void drawNoShip(GuiGraphics graphics) {
        text(graphics, "NO VALKYRIEN SKIES AIRFRAME DETECTED", 285, 190, RED);
        text(graphics, "Display state is waiting for the cockpit ship transform.", 284, 210, DIM);
    }

    private void drawIffPage(GuiGraphics g) {
        F35IffClientState.Snapshot s = F35IffClientState.get(cockpitId);
        if (s.status().equals("WAIT")) {
            text(g, "WAITING FOR THIS COCKPIT'S IFF DATA", 34, 116, AMBER);
            return;
        }
        text(g, "IFF / TRANSPONDER CONTROL", 34, 88, CYAN);
        text(g, "MASTER  " + s.master(), 34, 116, GREEN); rect(g, 28, 104, 190, 30, GRID);
        String[] labels={"MODE 1  "+s.mode1(),"MODE 2  "+s.mode2(),"MODE 3/A  "+s.mode3a(),"MODE 4  SECURE","MODE 5  SECURE+DATA"};
        for(int i=0;i<5;i++){ int y=154+i*34; rect(g,28,y-8,280,28,GRID); text(g,labels[i],40,y,s.enabled(i+1)?GREEN:DIM); text(g,s.enabled(i+1)?"ON":"OFF",260,y,s.enabled(i+1)?GREEN:AMBER); if(i<3)text(g,"+",292,y,WHITE); }
        text(g,"MISSION KEY",350,88,CYAN); rect(g,340,104,220,96,GRID);
        text(g,"SLOT  [A] [B]  ACTIVE "+(s.activeSlot()==0?"A":"B"),354,118,WHITE); text(g,"KEY ID  "+s.keyId(),354,140,GREEN);
        long remain=Math.max(0,s.expiresAt()-Instant.now().getEpochSecond()); text(g,"VALID  "+remain/60+" MIN",354,162,remain>0?GREEN:AMBER); text(g,s.status(),354,184,remain>0?GREEN:AMBER);
        rect(g,580,104,245,112,GRID); text(g,"KEY OPERATIONS",594,118,CYAN); text(g,"[CREATE SHARED MISSION]",594,140,WHITE); text(g,"[DISTRIBUTE / LOAD / ROTATE]",594,164,CYAN); text(g,"[ZEROIZE THIS COCKPIT]",594,192,RED);
        text(g,"MODE 5 AUTHENTICATED DATA",350,226,CYAN); String[] fields={"POSITION","VELOCITY","HEADING","MISSION"};
        for(int i=0;i<4;i++){int x=350+(i%2)*230,y=254+(i/2)*38;rect(g,x,y-8,210,28,GRID);text(g,fields[i]+"  "+(s.telemetry(i)?"SEND":"HOLD"),x+12,y,s.telemetry(i)?GREEN:DIM);}
        text(g,"Server owns keys and authentication. Secret material is never sent to clients.",350,340,DIM);
        text(g,"Public HKDF/HMAC-SHA-256 + AES-256-GCM analogue; not classified military crypto.",350,358,AMBER);
    }

    private static int iffActionAt(double x,double y){
        if(x>=28&&x<=218&&y>=104&&y<=134)return 0;
        for(int i=0;i<5;i++)if(x>=28&&x<=308&&y>=146+i*34&&y<=174+i*34)return (i<3&&x>=282)?41+i:i+1;
        if(x>=390&&x<=420&&y>=104&&y<=134)return 20; if(x>=421&&x<=451&&y>=104&&y<=134)return 21;
        if(x>=580&&x<=825&&y>=124&&y<=152)return 30; if(x>=580&&x<=825&&y>152&&y<=178)return 32; if(x>=580&&x<=825&&y>178&&y<=216)return 31;
        for(int i=0;i<4;i++){int xx=350+(i%2)*230,yy=246+(i/2)*38;if(x>=xx&&x<=xx+210&&y>=yy&&y<=yy+28)return 10+i;} return -1;
    }

    private void drawStoresPage(
            GuiGraphics graphics,
            F35DisplayState state
    ) {
        F35StoresSnapshot stores = state.stores();

        text(graphics, "FUEL", 40, 92, MAGENTA);
        text(graphics, format0(stores.fuelPercent()) + "%", 40, 116, GREEN);
        text(graphics, format0(stores.fuelKg()) + " KG", 40, 136, WHITE);
        text(graphics, "GUN " + stores.gunLabel(), 40, 170, CYAN);
        text(graphics, stores.gunRounds() + " RDS", 40, 190, WHITE);

        rect(graphics, 180, 92, 620, 244, GRID);
        text(graphics, "SMS / WEAPON STATIONS", 200, 108, CYAN);

        int row = 0;
        for (F35StoresSnapshot.Station station : stores.stations()) {
            int col = row % 2;
            int line = row / 2;
            int x = 210 + col * 280;
            int y = 142 + line * 34;
            int color = station.selected() ? GREEN : WHITE;

            rect(graphics, x, y, 250, 26, station.selected() ? GREEN : DIM);
            text(
                    graphics,
                    "STA " + station.index() + "  " + station.label() + "  X" + station.count(),
                    x + 10,
                    y + 9,
                    color
            );
            row++;
            if (row >= 10) {
                break;
            }
        }

        text(graphics, "SECTION 1 MIRRORS THE LEFT SMS / STORES DISPLAY", 40, 360, DIM);
    }

    private void drawSensorPage(
            GuiGraphics graphics,
            F35DisplayState state
    ) {
        float cx = 430.0F;
        float cy = 224.0F;
        float radius = 136.0F;
        double range = state.radarRangeMeters() > 0.0
                ? state.radarRangeMeters()
                : 500.0;

        text(graphics, "SENSOR / RADAR", 28, 86, CYAN);
        text(graphics, "RNG " + rangeLabel(range), 28, 104, WHITE);
        text(graphics, "MODE TWS", 28, 122, GREEN);

        circle(graphics, cx, cy, radius, MAGENTA);
        circle(graphics, cx, cy, radius * 0.66F, GRID);
        circle(graphics, cx, cy, radius * 0.33F, GRID);
        cross(graphics, cx, cy, 18.0F, WHITE);
        aircraft(graphics, cx, cy, 8.0F, CYAN);

        renderBearingLine(graphics, state, cx, cy, radius, true, range);
        renderContacts(graphics, state, cx, cy, radius, true, range, true);
        drawLockBox(graphics, state, 650, 110, 170, 120);

        text(graphics, "FULL SENSOR PICTURE", 28, 350, MAGENTA);
        text(graphics, "Friendly = green aircraft   Hostile = red triangle   Unknown = amber diamond", 230, 350, DIM);
    }

    private void drawTsdPage(
            GuiGraphics graphics,
            F35DisplayState state
    ) {
        float cx = 430.0F;
        float cy = 388.0F;
        float radius = 292.0F;
        double range = state.radarRangeMeters() > 0.0
                ? state.radarRangeMeters()
                : 500.0;

        text(graphics, "TSD1", 28, 86, CYAN);
        text(graphics, "A-S   TWS", 28, 104, GREEN);
        text(graphics, "RNG " + rangeLabel(range), 28, 122, WHITE);

        arc(graphics, cx, cy, radius * 0.33F, 205.0, 335.0, 32, WHITE);
        arc(graphics, cx, cy, radius * 0.66F, 205.0, 335.0, 40, WHITE);
        arc(graphics, cx, cy, radius, 205.0, 335.0, 48, GRID);

        line(graphics, cx, cy, 160.0F, 148.0F, GRID);
        line(graphics, cx, cy, 700.0F, 148.0F, GRID);
        aircraft(graphics, cx, cy - 18.0F, 10.0F, CYAN);

        renderBearingLine(graphics, state, cx, cy - 18.0F, radius, false, range);
        renderContacts(graphics, state, cx, cy, radius, false, range, true);
        drawLockBox(graphics, state, 650, 92, 170, 120);

        text(graphics, "TSD1 / FORWARD SECTOR", 28, 350, MAGENTA);
        text(graphics, "Click any visible track to command the radar lock.", 600, 350, DIM);
    }

    private void drawHsiPage(
            GuiGraphics graphics,
            F35DisplayState state
    ) {
        float cx = 430.0F;
        float cy = 224.0F;
        float radius = 142.0F;
        double range = state.radarRangeMeters() > 0.0
                ? state.radarRangeMeters()
                : 500.0;

        text(graphics, "TSD2 / HSI", 28, 86, CYAN);
        text(graphics, "HDG " + threeDigits(state.ownship().headingDeg()), 28, 104, WHITE);
        text(graphics, "RNG " + rangeLabel(range), 28, 122, GREEN);

        circle(graphics, cx, cy, radius, WHITE);
        circle(graphics, cx, cy, radius * 0.66F, GRID);
        circle(graphics, cx, cy, radius * 0.33F, GRID);

        for (int deg = 0; deg < 360; deg += 30) {
            float[] a = polar(cx, cy, radius, deg - 90.0);
            float[] b = polar(cx, cy, radius - 9.0F, deg - 90.0);
            line(graphics, a[0], a[1], b[0], b[1], WHITE);
            if (deg % 60 == 0) {
                float[] t = polar(cx, cy, radius - 20.0F, deg - 90.0);
                text(graphics, Integer.toString(deg / 10), (int) t[0] - 4, (int) t[1] - 4, DIM);
            }
        }

        aircraft(graphics, cx, cy, 10.0F, CYAN);
        renderBearingLine(graphics, state, cx, cy, radius, true, range);
        renderContacts(graphics, state, cx, cy, radius, true, range, true);
        drawLockBox(graphics, state, 650, 110, 170, 120);

        text(graphics, "TSD2 / 360 DEGREE HSI", 28, 350, MAGENTA);
        text(graphics, "Locked target is boxed in white and mirrored on the physical display.", 460, 350, DIM);
    }

    private void renderContacts(
            GuiGraphics graphics,
            F35DisplayState state,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle,
            double rangeMeters,
            boolean interactive
    ) {
        AircraftTelemetry ownship = state.ownship();

        for (F35RadarTrackView track : state.tracks()) {
            if (!radarTrackVisible(track)) {
                continue;
            }

            float[] point = scopePoint(
                    ownship,
                    track.position(),
                    rangeMeters,
                    centerX,
                    centerY,
                    radius,
                    fullCircle
            );

            if (point == null) {
                continue;
            }

            int color = trackColor(track);

            if (F35DisplayClientConfig.trackTrails()) {
                renderTrail(
                        graphics,
                        ownship,
                        F35TrackTrailCache.radarTrail(track.trackId()),
                        rangeMeters,
                        centerX,
                        centerY,
                        radius,
                        fullCircle,
                        color
                );
            }

            if (F35DisplayClientConfig.velocityVectors()) {
                renderVelocityVector(
                        graphics,
                        ownship,
                        track.position(),
                        track.velocity(),
                        6.0,
                        rangeMeters,
                        centerX,
                        centerY,
                        radius,
                        fullCircle,
                        point[0],
                        point[1],
                        color
                );
            }

            boolean locked = F35TargetLockClient.isRadarLocked(track.trackId());
            drawRadarTrack(graphics, track, point[0], point[1], locked);

            if (interactive) {
                hitTargets.add(new HitTarget(TargetKind.RADAR, track.trackId(), point[0], point[1]));
            }
        }

        for (F35DetectionContact contact : state.detections()) {
            if (!detectionContactVisible(contact)) {
                continue;
            }

            float[] point = scopePoint(
                    ownship,
                    contact.position(),
                    rangeMeters,
                    centerX,
                    centerY,
                    radius,
                    fullCircle
            );

            if (point == null) {
                continue;
            }

            int color = detectionColor(contact);
            boolean trailEnabled = contact.type() == F35DetectionType.MISSILE
                    ? F35DisplayClientConfig.missileTrails()
                    : F35DisplayClientConfig.trackTrails();

            if (trailEnabled) {
                renderTrail(
                        graphics,
                        ownship,
                        F35TrackTrailCache.detectionTrail(contact.contactId()),
                        rangeMeters,
                        centerX,
                        centerY,
                        radius,
                        fullCircle,
                        color
                );
            }

            if (F35DisplayClientConfig.velocityVectors()) {
                renderVelocityVector(
                        graphics,
                        ownship,
                        contact.position(),
                        contact.velocity(),
                        contact.type() == F35DetectionType.MISSILE ? 3.0 : 5.0,
                        rangeMeters,
                        centerX,
                        centerY,
                        radius,
                        fullCircle,
                        point[0],
                        point[1],
                        color
                );
            }

            boolean locked = F35TargetLockClient.isDetectionLocked(contact.contactId());
            drawDetectionContact(graphics, contact, point[0], point[1], locked);

            if (interactive) {
                hitTargets.add(new HitTarget(TargetKind.DETECTION, contact.contactId(), point[0], point[1]));
            }
        }
    }

    private void renderBearingLine(
            GuiGraphics graphics,
            F35DisplayState state,
            float startX,
            float startY,
            float radius,
            boolean fullCircle,
            double rangeMeters
    ) {
        Vec3 targetPosition = null;

        F35RadarTrackView radar =
                F35TargetLockClient.lockedRadarTrack(state.tracks());

        if (radar != null) {
            targetPosition = radar.position();
        } else {
            F35DetectionContact detection =
                    F35TargetLockClient.lockedDetection(state.detections());
            if (detection != null) {
                targetPosition = detection.position();
            }
        }

        if (targetPosition == null) {
            return;
        }

        float[] point = scopePoint(
                state.ownship(),
                targetPosition,
                rangeMeters,
                startX,
                section == 3 ? 388.0F : 224.0F,
                radius,
                fullCircle
        );

        if (point == null) {
            return;
        }

        dashedLine(graphics, startX, startY, point[0], point[1], WHITE);
    }

    private void drawLockBox(
            GuiGraphics graphics,
            F35DisplayState state,
            int x,
            int y,
            int width,
            int height
    ) {
        rect(graphics, x, y, width, height, CYAN);
        text(graphics, "TARGET LOCK", x + 10, y + 10, CYAN);

        if (F35TargetLockClient.targetCoasting()) {
            text(graphics, "COAST", x + width - 48, y + 10, AMBER);
        }

        F35RadarTrackView radar =
                F35TargetLockClient.lockedRadarTrack(state.tracks());

        if (radar != null) {
            double distance = radar.position().distanceTo(state.ownship().position());
            text(graphics, radar.shortId() + " " + shortAffiliation(radar.iffAffiliation()), x + 10, y + 34, trackColor(radar));
            text(graphics, "R " + format0(distance) + " M", x + 10, y + 54, WHITE);
            text(graphics, "V " + format0(radar.speedMps()) + " M/S", x + 10, y + 72, WHITE);
            text(graphics, "Q " + format1(radar.quality()), x + 10, y + 90, GREEN);
            text(graphics, radar.iffAuthenticated() ? "AUTH " + (radar.iffTelemetry().startsWith("M5")?"M5":"M4") : "IFF " + radar.iffReplyStatus(), x + 10, y + 106, trackColor(radar));
            return;
        }

        F35DetectionContact contact =
                F35TargetLockClient.lockedDetection(state.detections());

        if (contact != null) {
            double distance = contact.position().distanceTo(state.ownship().position());
            text(graphics, contact.type().name() + " " + contact.shortId(), x + 10, y + 34, detectionColor(contact));
            text(graphics, "R " + format0(distance) + " M", x + 10, y + 54, WHITE);
            text(graphics, "V " + format0(contact.speedMps()) + " M/S", x + 10, y + 72, WHITE);
            text(graphics, contact.label(), x + 10, y + 90, DIM);
            text(graphics, contact.iffAuthenticated() ? "AUTH " + (contact.iffTelemetry().startsWith("M5")?"M5":"M4") : "IFF UNKNOWN", x + 10, y + 106, detectionColor(contact));
            return;
        }

        text(graphics, "NO LOCK", x + 10, y + 44, DIM);
        text(graphics, "CLICK A CONTACT", x + 10, y + 66, DIM);
    }

    private void drawRadarTrack(
            GuiGraphics graphics,
            F35RadarTrackView track,
            float x,
            float y,
            boolean locked
    ) {
        int color = trackColor(track);
        String affiliation = track.iffAffiliation() == null ? "" : track.iffAffiliation();

        if (track.iffAuthenticated() && affiliation.contains("FRIENDLY")) {
            aircraft(graphics, x, y, 7.0F, GREEN);
        } else if (affiliation.contains("HOSTILE")) {
            triangle(graphics, x, y, 7.0F, RED);
        } else {
            diamond(graphics, x, y, 6.0F, AMBER);
        }

        if (locked) {
            rect(graphics, (int) x - 10, (int) y - 10, 20, 20, WHITE);
            cross(graphics, x, y, 13.0F, WHITE);
        }

        if (F35DisplayClientConfig.trackLabels()) {
            text(graphics, track.shortId(), (int) x + 9, (int) y - 5, color);
        }
    }

    private void drawDetectionContact(
            GuiGraphics graphics,
            F35DetectionContact contact,
            float x,
            float y,
            boolean locked
    ) {
        int color = detectionColor(contact);

        switch (contact.type()) {
            case SHIP -> aircraft(graphics, x, y, 7.0F, color);
            case PLAYER -> rect(graphics, (int) x - 5, (int) y - 5, 10, 10, color);
            case MISSILE -> {
                triangle(graphics, x, y, 7.0F, color);
                line(graphics, x, y + 7.0F, x, y + 14.0F, color);
            }
            case MOB -> diamond(graphics, x, y, 5.0F, color);
        }

        if (locked) {
            rect(graphics, (int) x - 10, (int) y - 10, 20, 20, WHITE);
            cross(graphics, x, y, 13.0F, WHITE);
        }

        if (F35DisplayClientConfig.trackLabels()) {
            String label = contact.type() == F35DetectionType.MISSILE
                    ? "MSL " + contact.shortId()
                    : contact.type().name().substring(0, 1) + contact.shortId();
            text(graphics, label, (int) x + 9, (int) y - 5, color);
        }
    }

    private void renderTrail(
            GuiGraphics graphics,
            AircraftTelemetry ownship,
            List<F35TrackTrailCache.TrailPoint> trail,
            double rangeMeters,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle,
            int color
    ) {
        float[] previous = null;

        for (F35TrackTrailCache.TrailPoint trailPoint : trail) {
            float[] point = scopePoint(
                    ownship,
                    trailPoint.position(),
                    rangeMeters,
                    centerX,
                    centerY,
                    radius,
                    fullCircle
            );

            if (point == null) {
                previous = null;
                continue;
            }

            if (previous != null) {
                line(graphics, previous[0], previous[1], point[0], point[1], dim(color));
            }

            previous = point;
        }
    }

    private void renderVelocityVector(
            GuiGraphics graphics,
            AircraftTelemetry ownship,
            Vec3 position,
            Vec3 velocity,
            double seconds,
            double rangeMeters,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle,
            float startX,
            float startY,
            int color
    ) {
        Vec3 future = position.add(velocity.scale(seconds));
        float[] point = scopePoint(
                ownship,
                future,
                rangeMeters,
                centerX,
                centerY,
                radius,
                fullCircle
        );

        if (point != null) {
            line(graphics, startX, startY, point[0], point[1], dim(color));
        }
    }

    private static float[] scopePoint(
            AircraftTelemetry ownship,
            Vec3 target,
            double rangeMeters,
            float centerX,
            float centerY,
            float radius,
            boolean fullCircle
    ) {
        if (rangeMeters <= 0.0) {
            return null;
        }

        Relative relative = relativeToOwnship(ownship, target);
        double normalizedRight = relative.right() / rangeMeters;
        double normalizedForward = relative.forward() / rangeMeters;

        if (!com.k1ngtle.vsia.cockpit.network.F35RadarContactProjection.forwardVisible(fullCircle,normalizedForward)) {
            return null;
        }

        double radial = Math.sqrt(
                normalizedRight * normalizedRight
                        + normalizedForward * normalizedForward
        );

        if (radial > 1.0) {
            return null;
        }

        return new float[]{
                centerX + (float) normalizedRight * radius,
                centerY - (float) normalizedForward * radius
        };
    }

    private static Relative relativeToOwnship(
            AircraftTelemetry ownship,
            Vec3 target
    ) {
        double heading = Math.toRadians(ownship.headingDeg());
        double forwardX = -Math.sin(heading);
        double forwardZ = Math.cos(heading);
        double rightX = -Math.cos(heading);
        double rightZ = -Math.sin(heading);
        double dx = target.x - ownship.position().x;
        double dz = target.z - ownship.position().z;
        double forward = dx * forwardX + dz * forwardZ;
        double right = dx * rightX + dz * rightZ;
        return new Relative(forward, right);
    }

    private static boolean radarTrackVisible(F35RadarTrackView track) {
        return F35DisplayClientConfig.radarTrackVisible(track.iffAffiliation());
    }

    private static boolean detectionContactVisible(F35DetectionContact contact) {
        return switch (contact.type()) {
            case MOB -> F35DisplayClientConfig.detectMobs();
            case PLAYER -> F35DisplayClientConfig.detectPlayers();
            case SHIP -> F35DisplayClientConfig.detectShips();
            case MISSILE -> F35DisplayClientConfig.showMissiles();
        };
    }

    private static int trackColor(F35RadarTrackView track) {
        String affiliation = track.iffAffiliation() == null ? "" : track.iffAffiliation();
        if (track.iffAuthenticated() && affiliation.contains("FRIENDLY")) {
            return GREEN;
        }
        if (affiliation.contains("HOSTILE")) {
            return RED;
        }
        return AMBER;
    }

    private static int detectionColor(F35DetectionContact contact) {
        if(contact.iffAuthenticated()&&contact.iffStatus().startsWith("FRIENDLY"))return GREEN;
        return switch (contact.type()) {
            case SHIP -> AMBER;
            case PLAYER -> AMBER;
            case MISSILE -> RED;
            case MOB -> AMBER;
        };
    }

    private HitTarget nearestHit(float x, float y, float maxDistance) {
        HitTarget best = null;
        float bestDistanceSqr = maxDistance * maxDistance;

        for (HitTarget hit : hitTargets) {
            float dx = hit.x - x;
            float dy = hit.y - y;
            float distanceSqr = dx * dx + dy * dy;

            if (distanceSqr < bestDistanceSqr) {
                bestDistanceSqr = distanceSqr;
                best = hit;
            }
        }

        return best;
    }

    private void tab(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            String label,
            boolean active
    ) {
        int color = active ? GREEN : GRID;
        rect(graphics, x, y, width, 30, color);
        text(graphics, label, x + 12, y + 11, active ? GREEN : WHITE);
    }

    private void updateGeometry() {
        uiScale = Math.min(
                (width - 20.0F) / VIRTUAL_WIDTH,
                (height - 20.0F) / VIRTUAL_HEIGHT
        );
        uiScale = Math.max(0.25F, uiScale);
        uiLeft = (width - VIRTUAL_WIDTH * uiScale) / 2.0F;
        uiTop = (height - VIRTUAL_HEIGHT * uiScale) / 2.0F;
    }

    private static int clampSection(int value) {
        return Math.max(1, Math.min(4, value));
    }

    private static boolean inside(
            double x,
            double y,
            double left,
            double top,
            double width,
            double height
    ) {
        return x >= left
                && y >= top
                && x <= left + width
                && y <= top + height;
    }

    private void text(
            GuiGraphics graphics,
            String value,
            int x,
            int y,
            int color
    ) {
        graphics.drawString(font, value, x, y, color, false);
    }

    private void rect(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        lineH(graphics, x, x + width, y, color);
        lineH(graphics, x, x + width, y + height, color);
        lineV(graphics, x, y, y + height, color);
        lineV(graphics, x + width, y, y + height, color);
    }

    private void lineH(
            GuiGraphics graphics,
            int x1,
            int x2,
            int y,
            int color
    ) {
        graphics.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, color);
    }

    private void lineV(
            GuiGraphics graphics,
            int x,
            int y1,
            int y2,
            int color
    ) {
        graphics.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, color);
    }

    private void line(
            GuiGraphics graphics,
            float x1,
            float y1,
            float x2,
            float y2,
            int color
    ) {
        int ix1 = Math.round(x1);
        int iy1 = Math.round(y1);
        int ix2 = Math.round(x2);
        int iy2 = Math.round(y2);
        int dx = Math.abs(ix2 - ix1);
        int sx = ix1 < ix2 ? 1 : -1;
        int dy = -Math.abs(iy2 - iy1);
        int sy = iy1 < iy2 ? 1 : -1;
        int error = dx + dy;

        while (true) {
            graphics.fill(ix1, iy1, ix1 + 1, iy1 + 1, color);
            if (ix1 == ix2 && iy1 == iy2) {
                break;
            }
            int e2 = error * 2;
            if (e2 >= dy) {
                error += dy;
                ix1 += sx;
            }
            if (e2 <= dx) {
                error += dx;
                iy1 += sy;
            }
        }
    }

    private void dashedLine(
            GuiGraphics graphics,
            float x1,
            float y1,
            float x2,
            float y2,
            int color
    ) {
        int segments = 18;
        for (int i = 0; i < segments; i += 2) {
            float a = (float) i / segments;
            float b = (float) (i + 1) / segments;
            line(
                    graphics,
                    x1 + (x2 - x1) * a,
                    y1 + (y2 - y1) * a,
                    x1 + (x2 - x1) * b,
                    y1 + (y2 - y1) * b,
                    color
            );
        }
    }

    private void circle(
            GuiGraphics graphics,
            float cx,
            float cy,
            float radius,
            int color
    ) {
        arc(graphics, cx, cy, radius, 0.0, 360.0, 72, color);
    }

    private void arc(
            GuiGraphics graphics,
            float cx,
            float cy,
            float radius,
            double startDegrees,
            double endDegrees,
            int segments,
            int color
    ) {
        float[] previous = polar(cx, cy, radius, startDegrees);
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double degrees = startDegrees + (endDegrees - startDegrees) * t;
            float[] point = polar(cx, cy, radius, degrees);
            line(graphics, previous[0], previous[1], point[0], point[1], color);
            previous = point;
        }
    }

    private static float[] polar(
            float cx,
            float cy,
            float radius,
            double degrees
    ) {
        double radians = Math.toRadians(degrees);
        return new float[]{
                cx + (float) Math.cos(radians) * radius,
                cy + (float) Math.sin(radians) * radius
        };
    }

    private void cross(
            GuiGraphics graphics,
            float x,
            float y,
            float size,
            int color
    ) {
        line(graphics, x - size, y, x + size, y, color);
        line(graphics, x, y - size, x, y + size, color);
    }

    private void aircraft(
            GuiGraphics graphics,
            float x,
            float y,
            float size,
            int color
    ) {
        line(graphics, x, y - size, x, y + size, color);
        line(graphics, x - size, y, x + size, y, color);
        line(graphics, x - size * 0.55F, y + size * 0.55F, x, y + size * 0.28F, color);
        line(graphics, x + size * 0.55F, y + size * 0.55F, x, y + size * 0.28F, color);
    }

    private void triangle(
            GuiGraphics graphics,
            float x,
            float y,
            float size,
            int color
    ) {
        line(graphics, x, y - size, x - size, y + size, color);
        line(graphics, x - size, y + size, x + size, y + size, color);
        line(graphics, x + size, y + size, x, y - size, color);
    }

    private void diamond(
            GuiGraphics graphics,
            float x,
            float y,
            float size,
            int color
    ) {
        line(graphics, x, y - size, x + size, y, color);
        line(graphics, x + size, y, x, y + size, color);
        line(graphics, x, y + size, x - size, y, color);
        line(graphics, x - size, y, x, y - size, color);
    }

    private static String shortAffiliation(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        if (value.contains("FRIENDLY")) {
            return "FRIEND";
        }
        if (value.contains("HOSTILE")) {
            return "HOSTILE";
        }
        return "UNKNOWN";
    }

    private static String rangeLabel(double meters) {
        if (meters >= 1000.0) {
            return format1(meters / 1000.0) + " KM";
        }
        return format0(meters) + " M";
    }

    private static String threeDigits(double value) {
        int normalized = ((int) Math.round(value) % 360 + 360) % 360;
        return String.format("%03d", normalized);
    }

    private static String format0(double value) {
        return String.format("%.0f", value);
    }

    private static String format1(double value) {
        return String.format("%.1f", value);
    }

    private static int dim(int color) {
        int a = color >>> 24;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        r = (int) (r * 0.55F);
        g = (int) (g * 0.55F);
        b = (int) (b * 0.55F);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private enum TargetKind {
        RADAR,
        DETECTION
    }

    private record HitTarget(
            TargetKind kind,
            UUID id,
            float x,
            float y
    ) {
    }

    private record Relative(
            double forward,
            double right
    ) {
    }
}
