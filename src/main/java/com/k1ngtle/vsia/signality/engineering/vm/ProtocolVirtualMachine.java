package com.k1ngtle.vsia.signality.engineering.vm;

import com.k1ngtle.vsia.signality.engineering.math.RfMath;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;

public final class ProtocolVirtualMachine {

    private static final int NUMERIC_REGISTER_COUNT = 64;
    private static final int BUFFER_REGISTER_COUNT = 16;

    private final ProtocolProgram program;
    private final ProtocolVmHost host;

    private final double[] numbers =
            new double[NUMERIC_REGISTER_COUNT];

    private final byte[][] buffers =
            new byte[BUFFER_REGISTER_COUNT][];

    private final Map<String, ProtocolVmTimer> timers =
            new HashMap<>();

    private final Random deterministicRandom;

    private byte[] hostInput =
            new byte[0];

    private ProtocolVmEnvironment environment =
            ProtocolVmEnvironment.empty();

    public ProtocolVirtualMachine(
            ProtocolProgram program,
            ProtocolVmHost host
    ) {
        if (program == null) {
            throw new IllegalArgumentException(
                    "program"
            );
        }

        if (host == null) {
            throw new IllegalArgumentException(
                    "host"
            );
        }

        this.program =
                program;

        this.host =
                host;

        this.deterministicRandom =
                new Random(
                        program.id()
                                .toString()
                                .hashCode()
                );

        for (int i = 0;
             i < buffers.length;
             i++) {
            buffers[i] =
                    new byte[0];
        }
    }

    public ProtocolProgram program() {
        return program;
    }

    public ProtocolVmRunResult run(
            String entrypoint,
            byte[] input,
            ProtocolVmEnvironment environment
    ) {
        int pc =
                program.entrypoint(
                        entrypoint
                );

        if (pc < 0) {
            return ProtocolVmRunResult.failed(
                    0,
                    "Unknown entrypoint: "
                            + entrypoint
            );
        }

        this.hostInput =
                input == null
                        ? new byte[0]
                        : input.clone();

        this.environment =
                environment == null
                        ? ProtocolVmEnvironment.empty()
                        : environment;

        return execute(
                pc
        );
    }

    public List<String> dueTimerEntrypoints() {
        long now =
                host.currentTick();

        List<ProtocolVmTimer> due =
                timers.values()
                        .stream()
                        .filter(
                                timer ->
                                        timer.dueTick()
                                                <= now
                        )
                        .sorted(
                                Comparator.comparingLong(
                                        ProtocolVmTimer::dueTick
                                )
                        )
                        .toList();

        List<String> entries =
                new ArrayList<>(
                        due.size()
                );

        for (ProtocolVmTimer timer : due) {
            timers.remove(
                    timer.id()
            );

            entries.add(
                    timer.entrypoint()
            );
        }

        return entries;
    }

    public double number(
            int register
    ) {
        return numbers[
                checkNumberRegister(
                        register
                )
                ];
    }

    public void setNumber(
            int register,
            double value
    ) {
        numbers[
                checkNumberRegister(
                        register
                )
                ] =
                finiteOrZero(
                        value
                );
    }

    public byte[] buffer(
            int register
    ) {
        return buffers[
                checkBufferRegister(
                        register
                )
                ].clone();
    }

    public void setBuffer(
            int register,
            byte[] value
    ) {
        byte[] data =
                value == null
                        ? new byte[0]
                        : value.clone();

        int oldLength =
                buffers[
                        checkBufferRegister(
                                register
                        )
                        ].length;

        long projected =
                totalBufferBytes()
                        - oldLength
                        + data.length;

        if (projected
                > program
                .limits()
                .maxBufferBytes()) {
            throw new IllegalStateException(
                    "VM buffer memory limit exceeded"
            );
        }

        buffers[register] =
                data;
    }

    private ProtocolVmRunResult execute(
            int startPc
    ) {
        int pc =
                startPc;

        int executed =
                0;

        int limit =
                program
                        .limits()
                        .maxInstructionsPerRun();

        try {
            while (pc >= 0
                    && pc
                    < program
                    .instructions()
                    .size()) {

                if (executed++ >= limit) {
                    return ProtocolVmRunResult.failed(
                            executed,
                            "Instruction budget exceeded"
                    );
                }

                ProtocolInstruction instruction =
                        program
                                .instructions()
                                .get(
                                        pc
                                );

                int next =
                        pc + 1;

                switch (instruction.opcode()) {
                    case NOP -> {
                    }

                    case HALT -> {
                        return ProtocolVmRunResult.ok(
                                true,
                                executed
                        );
                    }

                    case SET_NUM ->
                            setNumber(
                                    instruction.a(),
                                    instruction.value()
                            );

                    case MOVE_NUM ->
                            setNumber(
                                    instruction.a(),
                                    number(
                                            instruction.b()
                                    )
                            );

                    case ADD ->
                            binary(
                                    instruction,
                                    (a, b) ->
                                            a + b
                            );

                    case SUB ->
                            binary(
                                    instruction,
                                    (a, b) ->
                                            a - b
                            );

                    case MUL ->
                            binary(
                                    instruction,
                                    (a, b) ->
                                            a * b
                            );

                    case DIV -> {
                        double divisor =
                                number(
                                        instruction.c()
                                );

                        if (divisor == 0.0) {
                            throw new ArithmeticException(
                                    "Division by zero"
                            );
                        }

                        setNumber(
                                instruction.a(),
                                number(
                                        instruction.b()
                                ) / divisor
                        );
                    }

                    case MOD -> {
                        double divisor =
                                number(
                                        instruction.c()
                                );

                        if (divisor == 0.0) {
                            throw new ArithmeticException(
                                    "Modulo by zero"
                            );
                        }

                        setNumber(
                                instruction.a(),
                                number(
                                        instruction.b()
                                ) % divisor
                        );
                    }

                    case MIN ->
                            binary(
                                    instruction,
                                    Math::min
                            );

                    case MAX ->
                            binary(
                                    instruction,
                                    Math::max
                            );

                    case POW ->
                            binary(
                                    instruction,
                                    Math::pow
                            );

                    case LOG2 ->
                            setNumber(
                                    instruction.a(),
                                    Math.log(
                                            Math.max(
                                                    number(
                                                            instruction.b()
                                                    ),
                                                    Double.MIN_NORMAL
                                            )
                                    ) / Math.log(
                                            2.0
                                    )
                            );

                    case CMP_EQ ->
                            setNumber(
                                    instruction.a(),
                                    Double.compare(
                                            number(
                                                    instruction.b()
                                            ),
                                            number(
                                                    instruction.c()
                                            )
                                    ) == 0
                                            ? 1.0
                                            : 0.0
                            );

                    case CMP_GT ->
                            setNumber(
                                    instruction.a(),
                                    number(
                                            instruction.b()
                                    )
                                            > number(
                                            instruction.c()
                                    )
                                            ? 1.0
                                            : 0.0
                            );

                    case CMP_LT ->
                            setNumber(
                                    instruction.a(),
                                    number(
                                            instruction.b()
                                    )
                                            < number(
                                            instruction.c()
                                    )
                                            ? 1.0
                                            : 0.0
                            );

                    case JUMP ->
                            next =
                                    jumpTarget(
                                            instruction
                                    );

                    case JUMP_IF_ZERO -> {
                        if (number(
                                instruction.a()
                        ) == 0.0) {
                            next =
                                    jumpTarget(
                                            instruction
                                    );
                        }
                    }

                    case JUMP_IF_NONZERO -> {
                        if (number(
                                instruction.a()
                        ) != 0.0) {
                            next =
                                    jumpTarget(
                                            instruction
                                    );
                        }
                    }

                    case LOAD_ENV ->
                            setNumber(
                                    instruction.a(),
                                    environment.number(
                                            instruction.text()
                                    )
                            );

                    case BUFFER_CLEAR ->
                            setBuffer(
                                    instruction.a(),
                                    new byte[0]
                            );

                    case BUFFER_COPY_INPUT ->
                            setBuffer(
                                    instruction.a(),
                                    hostInput
                            );

                    case BUFFER_COPY ->
                            setBuffer(
                                    instruction.a(),
                                    buffer(
                                            instruction.b()
                                    )
                            );

                    case BUFFER_APPEND ->
                            setBuffer(
                                    instruction.a(),
                                    concat(
                                            buffer(
                                                    instruction.a()
                                            ),
                                            buffer(
                                                    instruction.b()
                                            )
                                    )
                            );

                    case BUFFER_LENGTH ->
                            setNumber(
                                    instruction.a(),
                                    buffer(
                                            instruction.b()
                                    ).length
                            );

                    case BUFFER_APPEND_U8 ->
                            appendInteger(
                                    instruction.a(),
                                    number(
                                            instruction.b()
                                    ),
                                    1
                            );

                    case BUFFER_APPEND_U16_BE ->
                            appendInteger(
                                    instruction.a(),
                                    number(
                                            instruction.b()
                                    ),
                                    2
                            );

                    case BUFFER_APPEND_U32_BE ->
                            appendInteger(
                                    instruction.a(),
                                    number(
                                            instruction.b()
                                    ),
                                    4
                            );

                    case BUFFER_READ_U8 ->
                            setNumber(
                                    instruction.a(),
                                    readUnsignedInteger(
                                            instruction.b(),
                                            instruction.c(),
                                            1
                                    )
                            );

                    case BUFFER_READ_U16_BE ->
                            setNumber(
                                    instruction.a(),
                                    readUnsignedInteger(
                                            instruction.b(),
                                            instruction.c(),
                                            2
                                    )
                            );

                    case BUFFER_READ_U32_BE ->
                            setNumber(
                                    instruction.a(),
                                    readUnsignedInteger(
                                            instruction.b(),
                                            instruction.c(),
                                            4
                                    )
                            );

                    case BUFFER_SLICE -> {
                        int offset =
                                checkedInt(
                                        number(
                                                instruction.c()
                                        )
                                );

                        int length =
                                checkedInt(
                                        instruction.value()
                                );

                        byte[] source =
                                buffer(
                                        instruction.b()
                                );

                        if (offset < 0
                                || length < 0
                                || offset + length
                                > source.length) {
                            throw new IndexOutOfBoundsException(
                                    "Invalid buffer slice"
                            );
                        }

                        setBuffer(
                                instruction.a(),
                                Arrays.copyOfRange(
                                        source,
                                        offset,
                                        offset + length
                                )
                        );
                    }

                    case BUFFER_DROP_PREFIX -> {
                        byte[] source =
                                buffer(
                                        instruction.b()
                                );

                        int count =
                                checkedInt(
                                        number(
                                                instruction.c()
                                        )
                                );

                        if (count < 0
                                || count > source.length) {
                            throw new IndexOutOfBoundsException(
                                    "Invalid prefix-drop length"
                            );
                        }

                        setBuffer(
                                instruction.a(),
                                Arrays.copyOfRange(
                                        source,
                                        count,
                                        source.length
                                )
                        );
                    }

                    case BUFFER_DROP_TAIL -> {
                        byte[] source =
                                buffer(
                                        instruction.b()
                                );

                        int count =
                                checkedInt(
                                        number(
                                                instruction.c()
                                        )
                                );

                        if (count < 0
                                || count > source.length) {
                            throw new IndexOutOfBoundsException(
                                    "Invalid tail-drop length"
                            );
                        }

                        setBuffer(
                                instruction.a(),
                                Arrays.copyOf(
                                        source,
                                        source.length - count
                                )
                        );
                    }

                    case BUFFER_CRC32 -> {
                        CRC32 crc =
                                new CRC32();

                        crc.update(
                                buffer(
                                        instruction.b()
                                )
                        );

                        setNumber(
                                instruction.a(),
                                crc.getValue()
                        );
                    }

                    case BUFFER_SHA256 ->
                            setBuffer(
                                    instruction.a(),
                                    sha256(
                                            buffer(
                                                    instruction.b()
                                            )
                                    )
                            );

                    case BUFFER_HMAC_SHA256 ->
                            setBuffer(
                                    instruction.a(),
                                    hmacSha256(
                                            buffer(
                                                    instruction.b()
                                            ),
                                            buffer(
                                                    instruction.c()
                                            )
                                    )
                            );

                    case PRIMITIVE ->
                            invokePrimitive(
                                    instruction
                            );

                    case SEND_FRAME -> {
                        byte[] frame =
                                buffer(
                                        instruction.a()
                                );

                        if (frame.length
                                > program
                                .limits()
                                .maxFrameBytes()) {
                            throw new IllegalStateException(
                                    "Frame exceeds VM frame-size limit"
                            );
                        }

                        host.sendFrame(
                                frame
                        );
                    }

                    case DELIVER_HOST ->
                            host.deliverToHost(
                                    buffer(
                                            instruction.a()
                                    )
                            );

                    case SET_TIMER ->
                            setTimer(
                                    instruction
                            );

                    case CANCEL_TIMER ->
                            timers.remove(
                                    instruction.text()
                            );
                }

                pc =
                        next;
            }

            return ProtocolVmRunResult.ok(
                    false,
                    executed
            );
        } catch (Throwable throwable) {
            return ProtocolVmRunResult.failed(
                    executed,
                    throwable.getClass()
                            .getSimpleName()
                            + ": "
                            + throwable.getMessage()
            );
        }
    }

    private void invokePrimitive(
            ProtocolInstruction instruction
    ) {
        switch (instruction.text()) {
            case "rf.watts_to_dbm" ->
                    setNumber(
                            instruction.a(),
                            RfMath.wattsToDbm(
                                    number(
                                            instruction.b()
                                    )
                            )
                    );

            case "rf.dbm_to_watts" ->
                    setNumber(
                            instruction.a(),
                            RfMath.dbmToWatts(
                                    number(
                                            instruction.b()
                                    )
                            )
                    );

            case "rf.fspl_db" ->
                    setNumber(
                            instruction.a(),
                            RfMath.freeSpacePathLossDb(
                                    number(
                                            instruction.b()
                                    ),
                                    number(
                                            instruction.c()
                                    )
                            )
                    );

            case "rf.noise_floor_dbm" ->
                    setNumber(
                            instruction.a(),
                            RfMath.noiseFloorDbm(
                                    number(
                                            instruction.b()
                                    ),
                                    RfMath.STANDARD_TEMPERATURE_K,
                                    number(
                                            instruction.c()
                                    )
                            )
                    );

            case "rf.shannon_bps" ->
                    setNumber(
                            instruction.a(),
                            RfMath.shannonCapacityBps(
                                    number(
                                            instruction.b()
                                    ),
                                    number(
                                            instruction.c()
                                    )
                            )
                    );

            case "random.uniform" ->
                    setNumber(
                            instruction.a(),
                            deterministicRandom.nextDouble()
                    );

            case "math.clamp01" ->
                    setNumber(
                            instruction.a(),
                            Math.max(
                                    0.0,
                                    Math.min(
                                            1.0,
                                            number(
                                                    instruction.b()
                                            )
                                    )
                            )
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unknown VM primitive: "
                                    + instruction.text()
                    );
        }
    }

    private void setTimer(
            ProtocolInstruction instruction
    ) {
        String[] parts =
                instruction.text()
                        .split(
                                "\\|",
                                2
                        );

        if (parts.length != 2
                || parts[0].isBlank()
                || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "SET_TIMER text must be timerId|entrypoint"
            );
        }

        if (!timers.containsKey(
                parts[0]
        )
                && timers.size()
                >= program
                .limits()
                .maxTimers()) {
            throw new IllegalStateException(
                    "VM timer limit exceeded"
            );
        }

        long delay =
                Math.round(
                        Math.max(
                                1.0,
                                number(
                                        instruction.a()
                                )
                        )
                );

        delay =
                Math.min(
                        delay,
                        program
                                .limits()
                                .maxTimerDelayTicks()
                );

        timers.put(
                parts[0],
                new ProtocolVmTimer(
                        parts[0],
                        parts[1],
                        host.currentTick()
                                + delay
                )
        );
    }

    private void binary(
            ProtocolInstruction instruction,
            DoubleBinary operation
    ) {
        setNumber(
                instruction.a(),
                operation.apply(
                        number(
                                instruction.b()
                        ),
                        number(
                                instruction.c()
                        )
                )
        );
    }

    private void appendInteger(
            int bufferRegister,
            double value,
            int bytes
    ) {
        long integer =
                Math.round(
                        value
                );

        ByteBuffer encoded =
                ByteBuffer
                        .allocate(
                                bytes
                        )
                        .order(
                                ByteOrder.BIG_ENDIAN
                        );

        if (bytes == 1) {
            encoded.put(
                    (byte) integer
            );
        } else if (bytes == 2) {
            encoded.putShort(
                    (short) integer
            );
        } else {
            encoded.putInt(
                    (int) integer
            );
        }

        setBuffer(
                bufferRegister,
                concat(
                        buffer(
                                bufferRegister
                        ),
                        encoded.array()
                )
        );
    }

    private long readUnsignedInteger(
            int bufferRegister,
            int offsetNumberRegister,
            int bytes
    ) {
        byte[] source =
                buffer(
                        bufferRegister
                );

        int offset =
                checkedInt(
                        number(
                                offsetNumberRegister
                        )
                );

        if (offset < 0
                || offset + bytes
                > source.length) {
            throw new IndexOutOfBoundsException(
                    "Buffer read outside bounds"
            );
        }

        long value =
                0L;

        for (int i = 0;
             i < bytes;
             i++) {
            value =
                    (value << 8)
                            | (
                            source[offset + i]
                                    & 0xFFL
                    );
        }

        return value;
    }

    private int jumpTarget(
            ProtocolInstruction instruction
    ) {
        if (instruction.target() < 0
                || instruction.target()
                >= program
                .instructions()
                .size()) {
            throw new IllegalStateException(
                    "Invalid jump target"
            );
        }

        return instruction.target();
    }

    private long totalBufferBytes() {
        long total =
                0L;

        for (byte[] buffer : buffers) {
            total +=
                    buffer.length;
        }

        return total;
    }

    private static int checkNumberRegister(
            int register
    ) {
        if (register < 0
                || register
                >= NUMERIC_REGISTER_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Invalid numeric register "
                            + register
            );
        }

        return register;
    }

    private static int checkBufferRegister(
            int register
    ) {
        if (register < 0
                || register
                >= BUFFER_REGISTER_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Invalid buffer register "
                            + register
            );
        }

        return register;
    }

    private static int checkedInt(
            double value
    ) {
        if (!Double.isFinite(
                value
        )
                || value < Integer.MIN_VALUE
                || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Value cannot be represented as int"
            );
        }

        return (int) Math.round(
                value
        );
    }

    private static double finiteOrZero(
            double value
    ) {
        return Double.isFinite(
                value
        )
                ? value
                : 0.0;
    }

    private static byte[] concat(
            byte[] first,
            byte[] second
    ) {
        byte[] result =
                Arrays.copyOf(
                        first,
                        first.length
                                + second.length
                );

        System.arraycopy(
                second,
                0,
                result,
                first.length,
                second.length
        );

        return result;
    }

    private static byte[] sha256(
            byte[] data
    ) {
        try {
            return MessageDigest
                    .getInstance(
                            "SHA-256"
                    )
                    .digest(
                            data
                    );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "SHA-256 unavailable",
                    exception
            );
        }
    }

    private static byte[] hmacSha256(
            byte[] key,
            byte[] data
    ) {
        try {
            Mac mac =
                    Mac.getInstance(
                            "HmacSHA256"
                    );

            mac.init(
                    new SecretKeySpec(
                            key.length == 0
                                    ? new byte[32]
                                    : key,
                            "HmacSHA256"
                    )
            );

            return mac.doFinal(
                    data
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "HMAC-SHA256 unavailable",
                    exception
            );
        }
    }

    @FunctionalInterface
    private interface DoubleBinary {
        double apply(
                double a,
                double b
        );
    }
}
