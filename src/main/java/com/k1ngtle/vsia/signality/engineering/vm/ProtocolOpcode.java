package com.k1ngtle.vsia.signality.engineering.vm;

public enum ProtocolOpcode {
    NOP,
    HALT,

    SET_NUM,
    MOVE_NUM,
    ADD,
    SUB,
    MUL,
    DIV,
    MOD,
    MIN,
    MAX,
    POW,
    LOG2,

    CMP_EQ,
    CMP_GT,
    CMP_LT,

    JUMP,
    JUMP_IF_ZERO,
    JUMP_IF_NONZERO,

    LOAD_ENV,

    BUFFER_CLEAR,
    BUFFER_COPY_INPUT,
    BUFFER_COPY,
    BUFFER_APPEND,
    BUFFER_LENGTH,
    BUFFER_APPEND_U8,
    BUFFER_APPEND_U16_BE,
    BUFFER_APPEND_U32_BE,
    BUFFER_READ_U8,
    BUFFER_READ_U16_BE,
    BUFFER_READ_U32_BE,
    BUFFER_SLICE,
    BUFFER_DROP_PREFIX,
    BUFFER_DROP_TAIL,
    BUFFER_CRC32,
    BUFFER_SHA256,
    BUFFER_HMAC_SHA256,

    PRIMITIVE,

    SEND_FRAME,
    DELIVER_HOST,
    SET_TIMER,
    CANCEL_TIMER
}
