package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

public record TcpFlags(
        boolean fin,
        boolean syn,
        boolean rst,
        boolean psh,
        boolean ack
) {
    public static TcpFlags synOnly() {
        return new TcpFlags(
                false,
                true,
                false,
                false,
                false
        );
    }

    public static TcpFlags synAck() {
        return new TcpFlags(
                false,
                true,
                false,
                false,
                true
        );
    }

    public static TcpFlags ackOnly() {
        return new TcpFlags(
                false,
                false,
                false,
                false,
                true
        );
    }

    public static TcpFlags data() {
        return new TcpFlags(
                false,
                false,
                false,
                true,
                true
        );
    }

    public static TcpFlags finAck() {
        return new TcpFlags(
                true,
                false,
                false,
                false,
                true
        );
    }

    public static TcpFlags rstOnly() {
        return new TcpFlags(
                false,
                false,
                true,
                false,
                false
        );
    }
}
