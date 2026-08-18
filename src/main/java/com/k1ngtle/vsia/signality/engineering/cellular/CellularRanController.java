package com.k1ngtle.vsia.signality.engineering.cellular;

import net.minecraft.nbt.CompoundTag;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CellularRanController {

    @FunctionalInterface
    public interface Sender {
        void send(CompoundTag message);
    }

    private final Map<UUID, CellRecord> discoveredCells =
            new LinkedHashMap<>();

    private final Map<UUID, UeContext> connectedUes =
            new LinkedHashMap<>();

    private final ProportionalFairScheduler scheduler =
            new ProportionalFairScheduler();

    private final HandoverEngine handoverEngine =
            new HandoverEngine(3.0);

    private CellularMode mode =
            CellularMode.LEGACY_DIRECT;

    private UeRanState ueState =
            UeRanState.DETACHED;

    private int physicalCellId;
    private long cellIdentity;
    private String plmn = "00101";

    private UUID servingCellId;
    private int servingRnti;

    private double servingRsrpDbm =
            Double.NEGATIVE_INFINITY;

    private double servingSnrDb =
            Double.NEGATIVE_INFINITY;

    public CellularMode mode() {
        return mode;
    }

    public UeRanState ueState() {
        return ueState;
    }

    public UUID servingCellId() {
        return servingCellId;
    }

    public int servingRnti() {
        return servingRnti;
    }

    public double servingRsrpDbm() {
        return servingRsrpDbm;
    }

    public double servingSnrDb() {
        return servingSnrDb;
    }

    public Collection<CellRecord> discoveredCells() {
        return Collections.unmodifiableCollection(
                discoveredCells.values()
        );
    }

    public Collection<UeContext> connectedUes() {
        return Collections.unmodifiableCollection(
                connectedUes.values()
        );
    }

    public void useLegacyDirectMode() {
        mode = CellularMode.LEGACY_DIRECT;
        resetUe();
        connectedUes.clear();
    }

    public void configureUe() {
        mode = CellularMode.UE;
        resetUe();
    }

    public void configureBaseStation(
            int physicalCellId,
            long cellIdentity,
            String plmn
    ) {
        if (physicalCellId < 0 || physicalCellId > 1007) {
            throw new IllegalArgumentException(
                    "physicalCellId must be in [0,1007]"
            );
        }

        if (plmn == null || plmn.isBlank()) {
            throw new IllegalArgumentException("plmn");
        }

        mode = CellularMode.BASE_STATION;
        this.physicalCellId = physicalCellId;
        this.cellIdentity = cellIdentity;
        this.plmn = plmn;

        resetUe();
        connectedUes.clear();
    }

    public void startCellSearch() {
        requireUe();
        discoveredCells.clear();
        ueState = UeRanState.CELL_SEARCH;
    }

    public boolean selectStrongestCell(
            UUID ueId,
            Sender sender
    ) {
        requireUe();

        CellRecord best =
                discoveredCells.values()
                        .stream()
                        .max((a, b) ->
                                Double.compare(
                                        a.rsrpDbm(),
                                        b.rsrpDbm()
                                ))
                        .orElse(null);

        if (best == null) {
            return false;
        }

        servingCellId = best.baseStationId();
        servingRsrpDbm = best.rsrpDbm();
        servingSnrDb = best.snrDb();
        ueState = UeRanState.CAMPED;

        beginRandomAccess(ueId, sender);
        return true;
    }

    public void sendSystemInformation(
            UUID baseStationId,
            double frequencyHz,
            String profileId,
            Sender sender
    ) {
        if (mode != CellularMode.BASE_STATION) {
            return;
        }

        CompoundTag message =
                baseMessage(CellularMessageType.SSB);

        message.putUUID(
                "base_station_id",
                baseStationId
        );
        message.putInt(
                "physical_cell_id",
                physicalCellId
        );
        message.putLong(
                "cell_identity",
                cellIdentity
        );
        message.putString(
                "plmn",
                plmn
        );
        message.putDouble(
                "frequency_hz",
                frequencyHz
        );
        message.putString(
                "network_profile",
                profileId
        );

        sender.send(message);
    }

    public CompoundTag receive(
            UUID ownId,
            CompoundTag message,
            double receivedPowerDbm,
            double snrDb,
            Sender sender
    ) {
        if (!message.contains("cellular_message_type")) {
            return null;
        }

        if (message.contains("target_id")
                && !message.getUUID("target_id").equals(ownId)) {
            return null;
        }

        CellularMessageType type;

        try {
            type = CellularMessageType.valueOf(
                    message.getString("cellular_message_type")
            );
        } catch (Exception ignored) {
            return null;
        }

        if (type == CellularMessageType.SSB) {
            handleSsb(
                    message,
                    receivedPowerDbm,
                    snrDb
            );
            return null;
        }

        if (mode == CellularMode.BASE_STATION) {
            return receiveAsBaseStation(
                    ownId,
                    type,
                    message,
                    sender
            );
        }

        if (mode == CellularMode.UE) {
            return receiveAsUe(
                    ownId,
                    type,
                    message,
                    sender
            );
        }

        return null;
    }

    public void sendMeasurementReport(
            UUID ueId,
            Sender sender
    ) {
        if (mode != CellularMode.UE
                || servingCellId == null
                || ueState != UeRanState.REGISTERED) {
            return;
        }

        HandoverDecision decision =
                handoverEngine.evaluate(
                        servingCellId,
                        servingRsrpDbm,
                        discoveredCells.values()
                );

        CompoundTag message =
                baseMessage(
                        CellularMessageType.MEASUREMENT_REPORT
                );

        message.putUUID(
                "target_id",
                servingCellId
        );
        message.putUUID(
                "ue_id",
                ueId
        );
        message.putDouble(
                "serving_rsrp_dbm",
                servingRsrpDbm
        );
        message.putDouble(
                "serving_snr_db",
                servingSnrDb
        );
        message.putInt(
                "cqi",
                CellularCqiModel.fromSnrDb(
                        servingSnrDb
                )
        );

        if (decision.shouldHandover()) {
            message.putBoolean(
                    "handover_candidate",
                    true
            );
            message.putUUID(
                    "candidate_id",
                    decision.target().baseStationId()
            );
            message.putDouble(
                    "candidate_rsrp_dbm",
                    decision.target().rsrpDbm()
            );
            message.putDouble(
                    "handover_margin_db",
                    decision.marginDb()
            );
        }

        sender.send(message);
    }

    public Collection<ResourceBlockAllocation> schedule(
            int totalResourceBlocks
    ) {
        if (mode != CellularMode.BASE_STATION) {
            return Collections.emptyList();
        }

        return scheduler.schedule(
                connectedUes.values(),
                totalResourceBlocks
        );
    }

    public boolean sendDataFromUe(
            UUID ueId,
            CompoundTag payload,
            long payloadBits,
            Sender sender
    ) {
        if (mode != CellularMode.UE
                || ueState != UeRanState.REGISTERED
                || servingCellId == null) {
            return false;
        }

        CompoundTag message =
                baseMessage(CellularMessageType.DATA);

        message.putUUID(
                "target_id",
                servingCellId
        );
        message.putUUID(
                "ue_id",
                ueId
        );
        message.putLong(
                "payload_bits",
                Math.max(1L, payloadBits)
        );
        message.put(
                "payload",
                payload
        );

        sender.send(message);
        return true;
    }

    public boolean sendDataFromBaseStation(
            UUID baseStationId,
            UUID ueId,
            CompoundTag payload,
            long payloadBits,
            Sender sender
    ) {
        if (mode != CellularMode.BASE_STATION
                || !connectedUes.containsKey(ueId)) {
            return false;
        }

        CompoundTag message =
                baseMessage(CellularMessageType.DATA);

        message.putUUID(
                "target_id",
                ueId
        );
        message.putUUID(
                "base_station_id",
                baseStationId
        );
        message.putLong(
                "payload_bits",
                Math.max(1L, payloadBits)
        );
        message.put(
                "payload",
                payload
        );

        sender.send(message);
        return true;
    }

    private CompoundTag receiveAsBaseStation(
            UUID ownId,
            CellularMessageType type,
            CompoundTag message,
            Sender sender
    ) {
        switch (type) {
            case RACH_PREAMBLE ->
                    handleRachPreamble(
                            ownId,
                            message,
                            sender
                    );

            case RRC_SETUP_REQUEST ->
                    handleRrcSetupRequest(
                            ownId,
                            message,
                            sender
                    );

            case REGISTRATION_REQUEST ->
                    handleRegistrationRequest(
                            ownId,
                            message,
                            sender
                    );

            case MEASUREMENT_REPORT ->
                    handleMeasurementReport(
                            ownId,
                            message,
                            sender
                    );

            case HANDOVER_COMPLETE -> {
                UUID ueId =
                        message.getUUID("ue_id");

                connectedUes.computeIfAbsent(
                        ueId,
                        ignored -> new UeContext(
                                ueId,
                                allocateRnti()
                        )
                );
            }

            case DATA -> {
                UUID ueId =
                        message.getUUID("ue_id");

                UeContext context =
                        connectedUes.get(ueId);

                if (context == null) {
                    return null;
                }

                context.recordDelivery(
                        message.getLong("payload_bits")
                );

                return message.getCompound("payload");
            }

            default -> {
            }
        }

        return null;
    }

    private CompoundTag receiveAsUe(
            UUID ownId,
            CellularMessageType type,
            CompoundTag message,
            Sender sender
    ) {
        switch (type) {
            case RANDOM_ACCESS_RESPONSE -> {
                servingRnti =
                        message.getInt("temporary_rnti");

                ueState =
                        UeRanState.RRC_CONNECTING;

                CompoundTag request =
                        baseMessage(
                                CellularMessageType.RRC_SETUP_REQUEST
                        );

                request.putUUID(
                        "target_id",
                        servingCellId
                );
                request.putUUID(
                        "ue_id",
                        ownId
                );
                request.putInt(
                        "temporary_rnti",
                        servingRnti
                );

                sender.send(request);
            }

            case RRC_SETUP -> {
                servingRnti =
                        message.getInt("rnti");

                ueState =
                        UeRanState.RRC_CONNECTED;

                beginRegistration(
                        ownId,
                        sender
                );
            }

            case REGISTRATION_ACCEPT ->
                    ueState = UeRanState.REGISTERED;

            case HANDOVER_COMMAND -> {
                UUID target =
                        message.getUUID(
                                "new_serving_cell_id"
                        );

                CellRecord targetCell =
                        discoveredCells.get(target);

                if (targetCell == null) {
                    return null;
                }

                ueState = UeRanState.HANDOVER;
                servingCellId = target;
                servingRsrpDbm =
                        targetCell.rsrpDbm();
                servingSnrDb =
                        targetCell.snrDb();

                CompoundTag complete =
                        baseMessage(
                                CellularMessageType.HANDOVER_COMPLETE
                        );

                complete.putUUID(
                        "target_id",
                        target
                );
                complete.putUUID(
                        "ue_id",
                        ownId
                );

                sender.send(complete);
                ueState = UeRanState.REGISTERED;
            }

            case DATA -> {
                return message.getCompound("payload");
            }

            default -> {
            }
        }

        return null;
    }

    private void handleSsb(
            CompoundTag message,
            double receivedPowerDbm,
            double snrDb
    ) {
        if (mode != CellularMode.UE) {
            return;
        }

        UUID baseStationId =
                message.getUUID("base_station_id");

        CellRecord record =
                new CellRecord(
                        baseStationId,
                        message.getInt(
                                "physical_cell_id"
                        ),
                        message.getString(
                                "plmn"
                        ),
                        message.getLong(
                                "cell_identity"
                        ),
                        message.getDouble(
                                "frequency_hz"
                        ),
                        receivedPowerDbm,
                        snrDb,
                        System.nanoTime()
                );

        discoveredCells.put(
                baseStationId,
                record
        );

        if (baseStationId.equals(servingCellId)) {
            servingRsrpDbm =
                    receivedPowerDbm;
            servingSnrDb =
                    snrDb;
        }
    }

    private void beginRandomAccess(
            UUID ueId,
            Sender sender
    ) {
        ueState =
                UeRanState.RANDOM_ACCESS;

        CompoundTag message =
                baseMessage(
                        CellularMessageType.RACH_PREAMBLE
                );

        message.putUUID(
                "target_id",
                servingCellId
        );
        message.putUUID(
                "ue_id",
                ueId
        );
        message.putInt(
                "preamble_index",
                ThreadLocalRandom.current()
                        .nextInt(64)
        );

        sender.send(message);
    }

    private void handleRachPreamble(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        UUID ueId =
                message.getUUID("ue_id");

        int temporaryRnti =
                allocateRnti();

        CompoundTag response =
                baseMessage(
                        CellularMessageType.RANDOM_ACCESS_RESPONSE
                );

        response.putUUID(
                "target_id",
                ueId
        );
        response.putUUID(
                "base_station_id",
                ownId
        );
        response.putInt(
                "temporary_rnti",
                temporaryRnti
        );
        response.putInt(
                "timing_advance",
                0
        );
        response.putInt(
                "uplink_grant_resource_blocks",
                4
        );

        sender.send(response);
    }

    private void handleRrcSetupRequest(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        UUID ueId =
                message.getUUID("ue_id");

        int rnti =
                message.getInt("temporary_rnti");

        connectedUes.put(
                ueId,
                new UeContext(
                        ueId,
                        rnti
                )
        );

        CompoundTag response =
                baseMessage(
                        CellularMessageType.RRC_SETUP
                );

        response.putUUID(
                "target_id",
                ueId
        );
        response.putUUID(
                "base_station_id",
                ownId
        );
        response.putInt(
                "rnti",
                rnti
        );

        sender.send(response);
    }

    private void beginRegistration(
            UUID ueId,
            Sender sender
    ) {
        ueState =
                UeRanState.REGISTERING;

        CompoundTag request =
                baseMessage(
                        CellularMessageType.REGISTRATION_REQUEST
                );

        request.putUUID(
                "target_id",
                servingCellId
        );
        request.putUUID(
                "ue_id",
                ueId
        );

        sender.send(request);
    }

    private void handleRegistrationRequest(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        UUID ueId =
                message.getUUID("ue_id");

        if (!connectedUes.containsKey(ueId)) {
            return;
        }

        CompoundTag response =
                baseMessage(
                        CellularMessageType.REGISTRATION_ACCEPT
                );

        response.putUUID(
                "target_id",
                ueId
        );
        response.putUUID(
                "base_station_id",
                ownId
        );
        response.putString(
                "plmn",
                plmn
        );
        response.putLong(
                "temporary_subscriber_id",
                Math.abs(
                        ThreadLocalRandom.current()
                                .nextLong()
                )
        );

        sender.send(response);
    }

    private void handleMeasurementReport(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        UUID ueId =
                message.getUUID("ue_id");

        UeContext context =
                connectedUes.get(ueId);

        if (context == null) {
            return;
        }

        context.setCqi(
                message.getInt("cqi")
        );

        if (!message.getBoolean(
                "handover_candidate"
        )) {
            return;
        }

        UUID target =
                message.getUUID("candidate_id");

        CompoundTag command =
                baseMessage(
                        CellularMessageType.HANDOVER_COMMAND
                );

        command.putUUID(
                "target_id",
                ueId
        );
        command.putUUID(
                "source_cell_id",
                ownId
        );
        command.putUUID(
                "new_serving_cell_id",
                target
        );

        sender.send(command);
    }

    private CompoundTag baseMessage(
            CellularMessageType type
    ) {
        CompoundTag message =
                new CompoundTag();

        message.putString(
                "cellular_message_type",
                type.name()
        );

        return message;
    }

    private int allocateRnti() {
        return ThreadLocalRandom.current()
                .nextInt(
                        1,
                        65536
                );
    }

    private void requireUe() {
        if (mode != CellularMode.UE) {
            throw new IllegalStateException(
                    "Cellular interface is not in UE mode"
            );
        }
    }


    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putString(
                "Mode",
                mode.name()
        );

        tag.putString(
                "UeState",
                ueState.name()
        );

        tag.putInt(
                "PhysicalCellId",
                physicalCellId
        );

        tag.putLong(
                "CellIdentity",
                cellIdentity
        );

        tag.putString(
                "Plmn",
                plmn
        );

        if (servingCellId != null) {
            tag.putUUID(
                    "ServingCellId",
                    servingCellId
            );
        }

        tag.putInt(
                "ServingRnti",
                servingRnti
        );

        tag.putDouble(
                "ServingRsrpDbm",
                servingRsrpDbm
        );

        tag.putDouble(
                "ServingSnrDb",
                servingSnrDb
        );

        return tag;
    }

    public void load(
            CompoundTag tag
    ) {
        try {
            mode =
                    CellularMode.valueOf(
                            tag.getString(
                                    "Mode"
                            )
                    );
        } catch (Exception ignored) {
            mode =
                    CellularMode.LEGACY_DIRECT;
        }

        try {
            ueState =
                    UeRanState.valueOf(
                            tag.getString(
                                    "UeState"
                            )
                    );
        } catch (Exception ignored) {
            ueState =
                    UeRanState.DETACHED;
        }

        physicalCellId =
                tag.getInt(
                        "PhysicalCellId"
                );

        cellIdentity =
                tag.getLong(
                        "CellIdentity"
                );

        String loadedPlmn =
                tag.getString(
                        "Plmn"
                );

        if (!loadedPlmn.isBlank()) {
            plmn =
                    loadedPlmn;
        }

        servingCellId =
                tag.hasUUID(
                        "ServingCellId"
                )
                        ? tag.getUUID(
                        "ServingCellId"
                )
                        : null;

        servingRnti =
                tag.getInt(
                        "ServingRnti"
                );

        servingRsrpDbm =
                tag.getDouble(
                        "ServingRsrpDbm"
                );

        servingSnrDb =
                tag.getDouble(
                        "ServingSnrDb"
                );

        discoveredCells.clear();
        connectedUes.clear();

        if (mode == CellularMode.UE
                && ueState == UeRanState.REGISTERED) {
            ueState =
                    UeRanState.DETACHED;

            servingCellId =
                    null;

            servingRnti =
                    0;
        }
    }

    private void resetUe() {
        ueState = UeRanState.DETACHED;
        servingCellId = null;
        servingRnti = 0;
        servingRsrpDbm =
                Double.NEGATIVE_INFINITY;
        servingSnrDb =
                Double.NEGATIVE_INFINITY;
        discoveredCells.clear();
    }
}
