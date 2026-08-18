package com.k1ngtle.vsia.signality.engineering.cellular;

import com.k1ngtle.vsia.signality.engineering.cellular.core.FiveGCore;
import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;
import com.k1ngtle.vsia.signality.engineering.cellular.nas.FiveGAkaEngine;
import com.k1ngtle.vsia.signality.engineering.cellular.nas.NasMessageType;
import com.k1ngtle.vsia.signality.engineering.cellular.nas.NasState;
import com.k1ngtle.vsia.signality.engineering.cellular.pdcp.PdcpSecurityContext;
import net.minecraft.nbt.CompoundTag;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

    private final Map<UUID, CellularBearerContext> ueBearers =
            new HashMap<>();

    private final Map<UUID, byte[]> pendingChallenges =
            new HashMap<>();

    private final Map<UUID, String> pendingSupi =
            new HashMap<>();

    private final ProportionalFairScheduler scheduler =
            new ProportionalFairScheduler();

    private final HandoverEngine handoverEngine =
            new HandoverEngine(3.0);

    private final CellularProtocolStack protocolStack =
            new CellularProtocolStack();

    private final FiveGCore core =
            new FiveGCore();

    private CellularMode mode =
            CellularMode.LEGACY_DIRECT;

    private UeRanState ueState =
            UeRanState.DETACHED;

    private NasState nasState =
            NasState.DEREGISTERED;

    private int physicalCellId;
    private long cellIdentity;
    private String plmn = "00101";

    private UUID servingCellId;
    private int servingRnti;

    private double servingRsrpDbm =
            Double.NEGATIVE_INFINITY;

    private double servingSnrDb =
            Double.NEGATIVE_INFINITY;

    private String ueSupi = "";
    private byte[] ueSubscriberKey =
            new byte[0];

    private final CellularBearerContext ueBearer =
            new CellularBearerContext();

    private PduSession uePduSession;

    public CellularMode mode() {
        return mode;
    }

    public UeRanState ueState() {
        return ueState;
    }

    public NasState nasState() {
        return nasState;
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

    public PduSession pduSession() {
        return uePduSession;
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
        mode =
                CellularMode.LEGACY_DIRECT;

        resetUe();

        connectedUes.clear();
        ueBearers.clear();
        pendingChallenges.clear();
        pendingSupi.clear();
    }

    public void configureUe() {
        configureUe(
                "",
                new byte[0]
        );
    }

    public void configureUe(
            String supi,
            byte[] subscriberKey
    ) {
        mode =
                CellularMode.UE;

        ueSupi =
                supi == null
                        ? ""
                        : supi;

        ueSubscriberKey =
                subscriberKey == null
                        ? new byte[0]
                        : subscriberKey.clone();

        resetUe();
    }

    public void configureBaseStation(
            int physicalCellId,
            long cellIdentity,
            String plmn
    ) {
        if (physicalCellId < 0
                || physicalCellId > 1007) {
            throw new IllegalArgumentException(
                    "physicalCellId must be in [0,1007]"
            );
        }

        if (plmn == null
                || plmn.isBlank()) {
            throw new IllegalArgumentException(
                    "plmn"
            );
        }

        mode =
                CellularMode.BASE_STATION;

        this.physicalCellId =
                physicalCellId;

        this.cellIdentity =
                cellIdentity;

        this.plmn =
                plmn;

        resetUe();

        connectedUes.clear();
        ueBearers.clear();
        pendingChallenges.clear();
        pendingSupi.clear();
    }

    public void provisionSubscriber(
            String supi,
            byte[] subscriberKey
    ) {
        if (mode
                != CellularMode.BASE_STATION) {
            throw new IllegalStateException(
                    "Only a base station/core can provision subscribers"
            );
        }

        core.provisionSubscriber(
                supi,
                subscriberKey
        );
    }

    public void startCellSearch() {
        requireUe();

        discoveredCells.clear();

        ueState =
                UeRanState.CELL_SEARCH;
    }

    public boolean selectStrongestCell(
            UUID ueId,
            Sender sender
    ) {
        requireUe();

        CellRecord best =
                discoveredCells
                        .values()
                        .stream()
                        .max(
                                (a, b) ->
                                        Double.compare(
                                                a.rsrpDbm(),
                                                b.rsrpDbm()
                                        )
                        )
                        .orElse(
                                null
                        );

        if (best == null) {
            return false;
        }

        servingCellId =
                best.baseStationId();

        servingRsrpDbm =
                best.rsrpDbm();

        servingSnrDb =
                best.snrDb();

        ueState =
                UeRanState.CAMPED;

        beginRandomAccess(
                ueId,
                sender
        );

        return true;
    }

    public void sendSystemInformation(
            UUID baseStationId,
            double frequencyHz,
            String profileId,
            Sender sender
    ) {
        if (mode
                != CellularMode.BASE_STATION) {
            return;
        }

        CompoundTag message =
                baseMessage(
                        CellularMessageType.SSB
                );

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

        sender.send(
                message
        );
    }

    public CompoundTag receive(
            UUID ownId,
            CompoundTag message,
            double receivedPowerDbm,
            double snrDb,
            Sender sender
    ) {
        if (!message.contains(
                "cellular_message_type"
        )) {
            return null;
        }

        if (message.contains(
                "target_id"
        )
                && !message
                .getUUID(
                        "target_id"
                )
                .equals(
                        ownId
                )) {
            return null;
        }

        CellularMessageType type;

        try {
            type =
                    CellularMessageType.valueOf(
                            message.getString(
                                    "cellular_message_type"
                            )
                    );
        } catch (Exception ignored) {
            return null;
        }

        if (type
                == CellularMessageType.SSB) {
            handleSsb(
                    message,
                    receivedPowerDbm,
                    snrDb
            );

            return null;
        }

        if (mode
                == CellularMode.BASE_STATION) {
            return receiveAsBaseStation(
                    ownId,
                    type,
                    message,
                    sender
            );
        }

        if (mode
                == CellularMode.UE) {
            return receiveAsUe(
                    ownId,
                    type,
                    message,
                    sender
            );
        }

        return null;
    }

    public boolean requestPduSession(
            UUID ueId,
            String dnn,
            int fiveQi,
            Sender sender
    ) {
        if (mode
                != CellularMode.UE
                || nasState
                != NasState.REGISTERED
                || servingCellId == null) {
            return false;
        }

        CompoundTag nas =
                nasMessage(
                        NasMessageType.PDU_SESSION_ESTABLISHMENT_REQUEST
                );

        nas.putString(
                "dnn",
                dnn == null
                        || dnn.isBlank()
                        ? "internet"
                        : dnn
        );

        nas.putInt(
                "five_qi",
                Math.max(
                        1,
                        fiveQi
                )
        );

        sendNas(
                servingCellId,
                ueId,
                nas,
                sender
        );

        return true;
    }

    public void sendMeasurementReport(
            UUID ueId,
            Sender sender
    ) {
        if (mode
                != CellularMode.UE
                || servingCellId == null
                || ueState
                != UeRanState.REGISTERED) {
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
                    decision
                            .target()
                            .baseStationId()
            );

            message.putDouble(
                    "candidate_rsrp_dbm",
                    decision
                            .target()
                            .rsrpDbm()
            );

            message.putDouble(
                    "handover_margin_db",
                    decision.marginDb()
            );
        }

        sender.send(
                message
        );
    }

    public Collection<ResourceBlockAllocation> schedule(
            int totalResourceBlocks
    ) {
        if (mode
                != CellularMode.BASE_STATION) {
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
        if (mode
                != CellularMode.UE
                || ueState
                != UeRanState.REGISTERED
                || nasState
                != NasState.PDU_SESSION_ACTIVE
                || servingCellId == null
                || !ueBearer.secured()) {
            return false;
        }

        return protocolStack.transmit(
                ueBearer,
                payload,
                transport ->
                        sendUserPlane(
                                servingCellId,
                                ueId,
                                transport,
                                payloadBits,
                                sender
                        )
        );
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

            case NAS ->
                    handleNasAtBaseStation(
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
                        message.getUUID(
                                "ue_id"
                        );

                connectedUes.computeIfAbsent(
                        ueId,
                        ignored ->
                                new UeContext(
                                        ueId,
                                        allocateRnti()
                                )
                );
            }

            case USER_PLANE ->
            {
                UUID ueId =
                        message.getUUID(
                                "ue_id"
                        );

                CellularBearerContext bearer =
                        ueBearers.get(
                                ueId
                        );

                if (bearer == null) {
                    return null;
                }

                CompoundTag transport =
                        message.getCompound(
                                "transport"
                        );

                CompoundTag clear =
                        protocolStack.receive(
                                bearer,
                                transport
                        );

                sendHarqFeedback(
                        ueId,
                        ueId,
                        message.getInt(
                                "harq_process_id"
                        ),
                        true,
                        sender
                );

                UeContext context =
                        connectedUes.get(
                                ueId
                        );

                if (context != null) {
                    context.recordDelivery(
                            message.getLong(
                                    "payload_bits"
                            )
                    );
                }

                return clear;
            }

            case HARQ_FEEDBACK -> {
                UUID ueId =
                        message.getUUID(
                                "ue_id"
                        );

                CellularBearerContext bearer =
                        ueBearers.get(
                                ueId
                        );

                if (bearer != null) {
                    protocolStack.acknowledge(
                            bearer,
                            message.getInt(
                                    "harq_process_id"
                            ),
                            message.getBoolean(
                                    "ack"
                            )
                    );
                }
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
                        message.getInt(
                                "temporary_rnti"
                        );

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

                sender.send(
                        request
                );
            }

            case RRC_SETUP -> {
                servingRnti =
                        message.getInt(
                                "rnti"
                        );

                ueState =
                        UeRanState.RRC_CONNECTED;

                beginNasRegistration(
                        ownId,
                        sender
                );
            }

            case NAS ->
                    handleNasAtUe(
                            ownId,
                            message,
                            sender
                    );

            case HANDOVER_COMMAND -> {
                UUID target =
                        message.getUUID(
                                "new_serving_cell_id"
                        );

                CellRecord targetCell =
                        discoveredCells.get(
                                target
                        );

                if (targetCell == null) {
                    return null;
                }

                ueState =
                        UeRanState.HANDOVER;

                servingCellId =
                        target;

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

                sender.send(
                        complete
                );

                ueState =
                        UeRanState.REGISTERED;
            }

            case USER_PLANE -> {
                CompoundTag clear =
                        protocolStack.receive(
                                ueBearer,
                                message.getCompound(
                                        "transport"
                                )
                        );

                sendHarqFeedback(
                        servingCellId,
                        ownId,
                        message.getInt(
                                "harq_process_id"
                        ),
                        true,
                        sender
                );

                return clear;
            }

            case HARQ_FEEDBACK ->
                    protocolStack.acknowledge(
                            ueBearer,
                            message.getInt(
                                    "harq_process_id"
                            ),
                            message.getBoolean(
                                    "ack"
                            )
                    );

            default -> {
            }
        }

        return null;
    }

    private void beginNasRegistration(
            UUID ueId,
            Sender sender
    ) {
        ueState =
                UeRanState.REGISTERING;

        nasState =
                NasState.REGISTERING;

        CompoundTag nas =
                nasMessage(
                        NasMessageType.REGISTRATION_REQUEST
                );

        nas.putString(
                "supi",
                ueSupi
        );

        sendNas(
                servingCellId,
                ueId,
                nas,
                sender
        );
    }

    private void handleNasAtBaseStation(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        UUID ueId =
                message.getUUID(
                        "ue_id"
                );

        CompoundTag nas =
                message.getCompound(
                        "nas"
                );

        NasMessageType type =
                parseNasType(
                        nas
                );

        if (type == null) {
            return;
        }

        switch (type) {
            case REGISTRATION_REQUEST -> {
                String supi =
                        nas.getString(
                                "supi"
                        );

                if (!core.hasSubscriber(
                        supi
                )) {
                    return;
                }

                byte[] challenge =
                        FiveGAkaEngine.randomChallenge();

                pendingChallenges.put(
                        ueId,
                        challenge
                );

                pendingSupi.put(
                        ueId,
                        supi
                );

                CompoundTag response =
                        nasMessage(
                                NasMessageType.AUTHENTICATION_REQUEST
                        );

                response.putByteArray(
                        "challenge",
                        challenge
                );

                sendNas(
                        ueId,
                        ueId,
                        response,
                        sender
                );
            }

            case AUTHENTICATION_RESPONSE -> {
                String supi =
                        pendingSupi.get(
                                ueId
                        );

                byte[] challenge =
                        pendingChallenges.get(
                                ueId
                        );

                if (supi == null
                        || challenge == null
                        || !core.authenticate(
                        supi,
                        challenge,
                        nas.getByteArray(
                                "response"
                        )
                )) {
                    return;
                }

                byte[] subscriberKey =
                        core.subscriberKey(
                                supi
                        );

                byte[] anchorKey =
                        FiveGAkaEngine.deriveAnchorKey(
                                subscriberKey,
                                supi,
                                challenge
                        );

                CellularBearerContext bearer =
                        ueBearers.computeIfAbsent(
                                ueId,
                                ignored ->
                                        new CellularBearerContext()
                        );

                bearer.setSecurity(
                        new PdcpSecurityContext(
                                FiveGAkaEngine.deriveKey(
                                        anchorKey,
                                        "UP-CIPHER"
                                ),
                                FiveGAkaEngine.deriveKey(
                                        anchorKey,
                                        "UP-INTEGRITY"
                                )
                        )
                );

                CompoundTag command =
                        nasMessage(
                                NasMessageType.SECURITY_MODE_COMMAND
                        );

                command.putString(
                        "cipher_algorithm",
                        "AES_GCM_SIM"
                );

                command.putString(
                        "integrity_algorithm",
                        "HMAC_SHA256_SIM"
                );

                sendNas(
                        ueId,
                        ueId,
                        command,
                        sender
                );
            }

            case SECURITY_MODE_COMPLETE -> {
                String supi =
                        pendingSupi.get(
                                ueId
                        );

                if (supi == null) {
                    return;
                }

                core.register(
                        ueId,
                        supi
                );

                CompoundTag accept =
                        nasMessage(
                                NasMessageType.REGISTRATION_ACCEPT
                        );

                accept.putString(
                        "plmn",
                        plmn
                );

                sendNas(
                        ueId,
                        ueId,
                        accept,
                        sender
                );

                pendingChallenges.remove(
                        ueId
                );

                pendingSupi.remove(
                        ueId
                );
            }

            case PDU_SESSION_ESTABLISHMENT_REQUEST -> {
                PduSession session =
                        core.establishSession(
                                ueId,
                                nas.getString(
                                        "dnn"
                                ),
                                nas.getInt(
                                        "five_qi"
                                )
                        );

                if (session == null) {
                    return;
                }

                CompoundTag accept =
                        nasMessage(
                                NasMessageType.PDU_SESSION_ESTABLISHMENT_ACCEPT
                        );

                accept.putInt(
                        "session_id",
                        session.sessionId()
                );

                accept.putString(
                        "dnn",
                        session.dnn()
                );

                accept.putString(
                        "ip_address",
                        session.ipAddress()
                );

                accept.putInt(
                        "five_qi",
                        session.fiveQi()
                );

                sendNas(
                        ueId,
                        ueId,
                        accept,
                        sender
                );
            }

            default -> {
            }
        }
    }

    private void handleNasAtUe(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        CompoundTag nas =
                message.getCompound(
                        "nas"
                );

        NasMessageType type =
                parseNasType(
                        nas
                );

        if (type == null) {
            return;
        }

        switch (type) {
            case AUTHENTICATION_REQUEST -> {
                nasState =
                        NasState.AUTHENTICATING;

                byte[] challenge =
                        nas.getByteArray(
                                "challenge"
                        );

                byte[] response =
                        FiveGAkaEngine.calculateResponse(
                                ueSubscriberKey,
                                ueSupi,
                                challenge
                        );

                byte[] anchorKey =
                        FiveGAkaEngine.deriveAnchorKey(
                                ueSubscriberKey,
                                ueSupi,
                                challenge
                        );

                ueBearer.setSecurity(
                        new PdcpSecurityContext(
                                FiveGAkaEngine.deriveKey(
                                        anchorKey,
                                        "UP-CIPHER"
                                ),
                                FiveGAkaEngine.deriveKey(
                                        anchorKey,
                                        "UP-INTEGRITY"
                                )
                        )
                );

                CompoundTag reply =
                        nasMessage(
                                NasMessageType.AUTHENTICATION_RESPONSE
                        );

                reply.putByteArray(
                        "response",
                        response
                );

                sendNas(
                        servingCellId,
                        ownId,
                        reply,
                        sender
                );
            }

            case SECURITY_MODE_COMMAND -> {
                nasState =
                        NasState.SECURITY_MODE;

                CompoundTag complete =
                        nasMessage(
                                NasMessageType.SECURITY_MODE_COMPLETE
                        );

                sendNas(
                        servingCellId,
                        ownId,
                        complete,
                        sender
                );
            }

            case REGISTRATION_ACCEPT -> {
                nasState =
                        NasState.REGISTERED;

                ueState =
                        UeRanState.REGISTERED;
            }

            case PDU_SESSION_ESTABLISHMENT_ACCEPT -> {
                uePduSession =
                        new PduSession(
                                nas.getInt(
                                        "session_id"
                                ),
                                ownId,
                                nas.getString(
                                        "dnn"
                                ),
                                nas.getString(
                                        "ip_address"
                                ),
                                nas.getInt(
                                        "five_qi"
                                ),
                                true
                        );

                nasState =
                        NasState.PDU_SESSION_ACTIVE;
            }

            default -> {
            }
        }
    }

    private void handleSsb(
            CompoundTag message,
            double receivedPowerDbm,
            double snrDb
    ) {
        if (mode
                != CellularMode.UE) {
            return;
        }

        UUID baseStationId =
                message.getUUID(
                        "base_station_id"
                );

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

        if (baseStationId.equals(
                servingCellId
        )) {
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
                ThreadLocalRandom
                        .current()
                        .nextInt(
                                64
                        )
        );

        sender.send(
                message
        );
    }

    private void handleRachPreamble(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        UUID ueId =
                message.getUUID(
                        "ue_id"
                );

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

        sender.send(
                response
        );
    }

    private void handleRrcSetupRequest(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        UUID ueId =
                message.getUUID(
                        "ue_id"
                );

        int rnti =
                message.getInt(
                        "temporary_rnti"
                );

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

        sender.send(
                response
        );
    }

    private void handleMeasurementReport(
            UUID ownId,
            CompoundTag message,
            Sender sender
    ) {
        UUID ueId =
                message.getUUID(
                        "ue_id"
                );

        UeContext context =
                connectedUes.get(
                        ueId
                );

        if (context == null) {
            return;
        }

        context.setCqi(
                message.getInt(
                        "cqi"
                )
        );

        if (!message.getBoolean(
                "handover_candidate"
        )) {
            return;
        }

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
                message.getUUID(
                        "candidate_id"
                )
        );

        sender.send(
                command
        );
    }

    private void sendNas(
            UUID target,
            UUID ueId,
            CompoundTag nas,
            Sender sender
    ) {
        CompoundTag message =
                baseMessage(
                        CellularMessageType.NAS
                );

        message.putUUID(
                "target_id",
                target
        );

        message.putUUID(
                "ue_id",
                ueId
        );

        message.put(
                "nas",
                nas
        );

        sender.send(
                message
        );
    }

    private void sendUserPlane(
            UUID target,
            UUID ueId,
            CompoundTag transport,
            long payloadBits,
            Sender sender
    ) {
        CompoundTag message =
                baseMessage(
                        CellularMessageType.USER_PLANE
                );

        message.putUUID(
                "target_id",
                target
        );

        message.putUUID(
                "ue_id",
                ueId
        );

        message.putLong(
                "payload_bits",
                Math.max(
                        1L,
                        payloadBits
                )
        );

        message.putInt(
                "harq_process_id",
                transport.getInt(
                        "harq_process_id"
                )
        );

        message.put(
                "transport",
                transport
        );

        sender.send(
                message
        );
    }

    private void sendHarqFeedback(
            UUID target,
            UUID ueId,
            int processId,
            boolean ack,
            Sender sender
    ) {
        if (target == null) {
            return;
        }

        CompoundTag message =
                baseMessage(
                        CellularMessageType.HARQ_FEEDBACK
                );

        message.putUUID(
                "target_id",
                target
        );

        message.putUUID(
                "ue_id",
                ueId
        );

        message.putInt(
                "harq_process_id",
                processId
        );

        message.putBoolean(
                "ack",
                ack
        );

        sender.send(
                message
        );
    }

    private static CompoundTag nasMessage(
            NasMessageType type
    ) {
        CompoundTag nas =
                new CompoundTag();

        nas.putString(
                "nas_message_type",
                type.name()
        );

        return nas;
    }

    private static NasMessageType parseNasType(
            CompoundTag nas
    ) {
        try {
            return NasMessageType.valueOf(
                    nas.getString(
                            "nas_message_type"
                    )
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static CompoundTag baseMessage(
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
        return ThreadLocalRandom
                .current()
                .nextInt(
                        1,
                        65536
                );
    }

    private void requireUe() {
        if (mode
                != CellularMode.UE) {
            throw new IllegalStateException(
                    "Cellular interface is not in UE mode"
            );
        }
    }

    private void resetUe() {
        ueState =
                UeRanState.DETACHED;

        nasState =
                NasState.DEREGISTERED;

        servingCellId =
                null;

        servingRnti =
                0;

        servingRsrpDbm =
                Double.NEGATIVE_INFINITY;

        servingSnrDb =
                Double.NEGATIVE_INFINITY;

        uePduSession =
                null;

        discoveredCells.clear();
    }

    public CompoundTag save() {
        CompoundTag tag =
                new CompoundTag();

        tag.putString(
                "Mode",
                mode.name()
        );

        tag.putString(
                "UeState",
                ueState.name()
        );

        tag.putString(
                "NasState",
                nasState.name()
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

        tag.putString(
                "UeSupi",
                ueSupi
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

        if (uePduSession != null) {
            CompoundTag session =
                    new CompoundTag();

            session.putInt(
                    "SessionId",
                    uePduSession.sessionId()
            );

            session.putString(
                    "Dnn",
                    uePduSession.dnn()
            );

            session.putString(
                    "IpAddress",
                    uePduSession.ipAddress()
            );

            session.putInt(
                    "FiveQi",
                    uePduSession.fiveQi()
            );

            tag.put(
                    "PduSession",
                    session
            );
        }

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

        try {
            nasState =
                    NasState.valueOf(
                            tag.getString(
                                    "NasState"
                            )
                    );
        } catch (Exception ignored) {
            nasState =
                    NasState.DEREGISTERED;
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

        ueSupi =
                tag.getString(
                        "UeSupi"
                );

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
        ueBearers.clear();
        pendingChallenges.clear();
        pendingSupi.clear();

        ueSubscriberKey =
                new byte[0];

        uePduSession =
                null;

        if (mode
                == CellularMode.UE) {
            ueState =
                    UeRanState.DETACHED;

            nasState =
                    NasState.DEREGISTERED;

            servingCellId =
                    null;

            servingRnti =
                    0;
        }
    }
}
