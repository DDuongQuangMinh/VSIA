package com.k1ngtle.vsia.network.web;

import com.k1ngtle.vsia.client.web.W128IdeClient;
import com.k1ngtle.vsia.signality.internet.web.W128WebFile;
import com.k1ngtle.vsia.signality.internet.web.W128WebProject;
import com.k1ngtle.vsia.signality.internet.web.W128WebRegistrySavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public final class W128IdeSnapshotPacket {
    private static final int MAX_MESSAGE = 2048;
    private static final int MAX_HOST = 253;
    private static final int MAX_MODE = 32;
    private static final int MAX_IP = 45;
    private static final int MAX_PATH = 512;
    private static final int MAX_CONTENT_TYPE = 128;

    private final boolean success;
    private final String message;
    private final String host;
    private final String mode;
    private final String boundServerIp;
    private final boolean published;
    private final long buildRevision;
    private final String activePath;
    private final String activeContent;
    private final boolean activeGenerated;
    private final List<FileEntry> files;

    public W128IdeSnapshotPacket(
            boolean success,
            String message,
            String host,
            String mode,
            String boundServerIp,
            boolean published,
            long buildRevision,
            String activePath,
            String activeContent,
            boolean activeGenerated,
            List<FileEntry> files
    ) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.host = host == null ? "" : host;
        this.mode = mode == null ? "" : mode;
        this.boundServerIp = boundServerIp == null ? "" : boundServerIp;
        this.published = published;
        this.buildRevision = buildRevision;
        this.activePath = activePath == null ? "" : activePath;
        this.activeContent = activeContent == null ? "" : activeContent;
        this.activeGenerated = activeGenerated;
        this.files = files == null ? List.of() : List.copyOf(files);
    }

    public W128IdeSnapshotPacket(FriendlyByteBuf buf) {
        success = buf.readBoolean();
        message = buf.readUtf(MAX_MESSAGE);
        host = buf.readUtf(MAX_HOST);
        mode = buf.readUtf(MAX_MODE);
        boundServerIp = buf.readUtf(MAX_IP);
        published = buf.readBoolean();
        buildRevision = buf.readLong();
        activePath = buf.readUtf(MAX_PATH);
        activeContent = buf.readUtf(W128WebRegistrySavedData.MAX_FILE_CHARACTERS);
        activeGenerated = buf.readBoolean();

        int count = Math.min(
                W128WebRegistrySavedData.MAX_FILES_PER_PROJECT,
                Math.max(0, buf.readVarInt())
        );

        List<FileEntry> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            decoded.add(new FileEntry(
                    buf.readUtf(MAX_PATH),
                    buf.readUtf(MAX_CONTENT_TYPE),
                    buf.readBoolean()
            ));
        }

        files = List.copyOf(decoded);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeUtf(message, MAX_MESSAGE);
        buf.writeUtf(host, MAX_HOST);
        buf.writeUtf(mode, MAX_MODE);
        buf.writeUtf(boundServerIp, MAX_IP);
        buf.writeBoolean(published);
        buf.writeLong(buildRevision);
        buf.writeUtf(activePath, MAX_PATH);
        buf.writeUtf(activeContent, W128WebRegistrySavedData.MAX_FILE_CHARACTERS);
        buf.writeBoolean(activeGenerated);
        buf.writeVarInt(files.size());

        for (FileEntry file : files) {
            buf.writeUtf(file.path(), MAX_PATH);
            buf.writeUtf(file.contentType(), MAX_CONTENT_TYPE);
            buf.writeBoolean(file.generated());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(
                () -> DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> W128IdeClient.handleSnapshot(this)
                )
        );

        context.setPacketHandled(true);
    }

    public static W128IdeSnapshotPacket fromProject(
            W128WebProject project,
            String selectedPath,
            String message,
            boolean success
    ) {
        List<FileEntry> fileEntries = project.files()
                .values()
                .stream()
                .sorted(Comparator.comparing(W128WebFile::path))
                .map(file -> new FileEntry(
                        file.path(),
                        file.contentType(),
                        file.generated()
                ))
                .toList();

        W128WebFile selected = project.file(selectedPath);

        return new W128IdeSnapshotPacket(
                success,
                message,
                project.host(),
                project.mode().name(),
                project.boundServerIp(),
                project.published(),
                project.buildRevision(),
                selected == null ? "" : selected.path(),
                selected == null ? "" : selected.content(),
                selected != null && selected.generated(),
                fileEntries
        );
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public String host() {
        return host;
    }

    public String mode() {
        return mode;
    }

    public String boundServerIp() {
        return boundServerIp;
    }

    public boolean published() {
        return published;
    }

    public long buildRevision() {
        return buildRevision;
    }

    public String activePath() {
        return activePath;
    }

    public String activeContent() {
        return activeContent;
    }

    public boolean activeGenerated() {
        return activeGenerated;
    }

    public List<FileEntry> files() {
        return files;
    }

    public record FileEntry(String path, String contentType, boolean generated) {
    }
}
