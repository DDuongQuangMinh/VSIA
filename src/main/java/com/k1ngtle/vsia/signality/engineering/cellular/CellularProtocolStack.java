package com.k1ngtle.vsia.signality.engineering.cellular;

import com.k1ngtle.vsia.signality.engineering.cellular.mac.HarqProcess;
import com.k1ngtle.vsia.signality.engineering.cellular.pdcp.PdcpPdu;
import com.k1ngtle.vsia.signality.engineering.cellular.rlc.RlcPdu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class CellularProtocolStack {
    private static final AtomicLong NEXT_TRANSPORT_BLOCK =
            new AtomicLong(1L);

    private static final int DEFAULT_RLC_SEGMENT_BYTES =
            768;

    @FunctionalInterface
    public interface Sender {
        void send(CompoundTag message);
    }

    public boolean transmit(
            CellularBearerContext bearer,
            CompoundTag payload,
            Sender sender
    ) {
        if (bearer == null
                || !bearer.secured()) {
            return false;
        }

        byte[] serialized =
                serialize(payload);

        PdcpPdu pdcp =
                bearer.pdcp()
                        .protect(
                                serialized,
                                bearer.security()
                        );

        byte[] encodedPdcp =
                encodePdcp(pdcp);

        List<RlcPdu> segments =
                bearer.rlc()
                        .segment(
                                encodedPdcp,
                                DEFAULT_RLC_SEGMENT_BYTES
                        );

        for (RlcPdu segment : segments) {
            long transportBlockId =
                    NEXT_TRANSPORT_BLOCK
                            .getAndIncrement();

            HarqProcess harq =
                    bearer.harq()
                            .allocate(
                                    transportBlockId
                            );

            if (harq == null) {
                return false;
            }

            CompoundTag transport =
                    new CompoundTag();

            transport.putString(
                    "cellular_protocol",
                    "USER_PLANE"
            );

            transport.putLong(
                    "transport_block_id",
                    transportBlockId
            );

            transport.putInt(
                    "harq_process_id",
                    harq.processId()
            );

            transport.putInt(
                    "rlc_sequence_number",
                    segment.sequenceNumber()
            );

            transport.putInt(
                    "rlc_segment_index",
                    segment.segmentIndex()
            );

            transport.putInt(
                    "rlc_segment_count",
                    segment.segmentCount()
            );

            transport.putByteArray(
                    "rlc_payload",
                    segment.payload()
            );

            sender.send(
                    transport
            );
        }

        return true;
    }

    public CompoundTag receive(
            CellularBearerContext bearer,
            CompoundTag transport
    ) {
        if (bearer == null
                || !bearer.secured()) {
            return null;
        }

        RlcPdu rlc =
                new RlcPdu(
                        transport.getInt(
                                "rlc_sequence_number"
                        ),
                        transport.getInt(
                                "rlc_segment_index"
                        ),
                        transport.getInt(
                                "rlc_segment_count"
                        ),
                        transport.getByteArray(
                                "rlc_payload"
                        )
                );

        byte[] complete =
                bearer.rlc()
                        .receive(
                                rlc
                        );

        if (complete == null) {
            return null;
        }

        PdcpPdu pdcp =
                decodePdcp(
                        complete
                );

        byte[] clear =
                bearer.pdcp()
                        .unprotect(
                                pdcp,
                                bearer.security()
                        );

        return deserialize(
                clear
        );
    }

    public void acknowledge(
            CellularBearerContext bearer,
            int harqProcessId,
            boolean success
    ) {
        HarqProcess process =
                bearer.harq()
                        .process(
                                harqProcessId
                        );

        if (process == null) {
            return;
        }

        if (success) {
            process.acknowledge();
        } else {
            process.negativeAcknowledge();
        }
    }

    private static byte[] encodePdcp(
            PdcpPdu pdu
    ) {
        CompoundTag tag =
                new CompoundTag();

        tag.putInt(
                "sn",
                pdu.sequenceNumber()
        );

        tag.putByteArray(
                "payload",
                pdu.protectedPayload()
        );

        tag.putByteArray(
                "integrity",
                pdu.integrityTag()
        );

        return serialize(
                tag
        );
    }

    private static PdcpPdu decodePdcp(
            byte[] encoded
    ) {
        CompoundTag tag =
                deserialize(
                        encoded
                );

        return new PdcpPdu(
                tag.getInt(
                        "sn"
                ),
                tag.getByteArray(
                        "payload"
                ),
                tag.getByteArray(
                        "integrity"
                )
        );
    }

    private static byte[] serialize(
            CompoundTag tag
    ) {
        try {
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream();

            NbtIo.write(
                    tag,
                    new DataOutputStream(
                            bytes
                    )
            );

            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to serialize cellular protocol payload",
                    exception
            );
        }
    }

    private static CompoundTag deserialize(
            byte[] bytes
    ) {
        try {
            CompoundTag tag =
                    NbtIo.read(
                            new DataInputStream(
                                    new ByteArrayInputStream(
                                            bytes
                                    )
                            )
                    );

            return tag == null
                    ? new CompoundTag()
                    : tag;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Unable to deserialize cellular protocol payload",
                    exception
            );
        }
    }
}
