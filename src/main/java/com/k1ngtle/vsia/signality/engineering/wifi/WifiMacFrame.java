package com.k1ngtle.vsia.signality.engineering.wifi;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.CRC32;
public record WifiMacFrame(int frameControl,int durationId,byte[] address1,byte[] address2,byte[] address3,int sequenceControl,byte[] payload){
    public WifiMacFrame{
        check(address1);check(address2);check(address3);
        address1=address1.clone();address2=address2.clone();address3=address3.clone();
        payload=payload==null?new byte[0]:payload.clone();
    }
    public byte[] encode(){
        ByteBuffer b=ByteBuffer.allocate(24+payload.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short)frameControl).putShort((short)durationId).put(address1).put(address2).put(address3).putShort((short)sequenceControl).put(payload);
        byte[] body=b.array(); CRC32 crc=new CRC32(); crc.update(body);
        ByteBuffer out=ByteBuffer.allocate(body.length+4).order(ByteOrder.LITTLE_ENDIAN);
        out.put(body).putInt((int)crc.getValue()); return out.array();
    }
    public static WifiMacFrame decode(byte[] encoded){
        if(encoded.length<28)throw new IllegalArgumentException("frame too short");
        byte[] body=Arrays.copyOf(encoded,encoded.length-4); CRC32 crc=new CRC32();crc.update(body);
        long expected=Integer.toUnsignedLong(ByteBuffer.wrap(encoded,encoded.length-4,4).order(ByteOrder.LITTLE_ENDIAN).getInt());
        if(expected!=crc.getValue())throw new IllegalArgumentException("FCS mismatch");
        ByteBuffer b=ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        int fc=Short.toUnsignedInt(b.getShort()),dur=Short.toUnsignedInt(b.getShort());
        byte[] a1=new byte[6],a2=new byte[6],a3=new byte[6];b.get(a1);b.get(a2);b.get(a3);
        int seq=Short.toUnsignedInt(b.getShort());byte[] p=new byte[b.remaining()];b.get(p);
        return new WifiMacFrame(fc,dur,a1,a2,a3,seq,p);
    }
    private static void check(byte[] a){if(a==null||a.length!=6)throw new IllegalArgumentException("MAC must be 6 bytes");}
}
