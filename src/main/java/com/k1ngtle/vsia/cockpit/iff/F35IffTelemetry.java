package com.k1ngtle.vsia.cockpit.iff;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.F35VsShipHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/** Versioned Mode-5-style payload generated from server telemetry, not client submissions. */
public final class F35IffTelemetry {
    private F35IffTelemetry() { }
    public static byte[] encode(F35CockpitSeatBlockEntity cockpit, UUID aircraft) throws IOException {
        F35IffConfig i=cockpit.iff(); F35VsShipHelper.ShipSnapshot s=F35VsShipHelper.shipSnapshot(cockpit);
        return encode(i,aircraft,cockpit.cockpitId(),s,F35VsShipHelper.cockpitHeadingDeg(cockpit));
    }
    public static byte[] encode(F35IffConfig i,UUID aircraft,UUID cockpitId,F35VsShipHelper.ShipSnapshot s,double heading)throws IOException{
        int mask=(i.sendPosition()?1:0)|(i.sendVelocity()?2:0)|(i.sendHeading()?4:0)|(i.sendMission()?8:0);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(1);uuid(out,aircraft);uuid(out,cockpitId);out.writeBoolean(i.master()==F35IffConfig.Master.EMER);
        out.writeByte(mask);out.writeUTF(i.mode3aEnabled()?i.mode3a():"");
        if((mask&1)!=0){out.writeDouble(s.worldCenter().x);out.writeDouble(s.worldCenter().y);out.writeDouble(s.worldCenter().z);}
        if((mask&2)!=0){out.writeDouble(s.velocity().x);out.writeDouble(s.velocity().y);out.writeDouble(s.velocity().z);out.writeDouble(s.velocity().length());}
        if((mask&4)!=0)out.writeDouble(heading);
        if((mask&8)!=0){uuid(out,i.activeKey().mission());out.writeUTF(i.mode1Enabled()?i.mode1():"");out.writeUTF(i.mode2Enabled()?i.mode2():"");}
        out.flush();return bytes.toByteArray();
    }
    public static Verified decode(byte[] bytes, UUID expectedAircraft, UUID expectedCockpit, UUID expectedMission) throws IOException {
        if(bytes.length>512)throw new IOException("Telemetry length");DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readInt()!=1||!uuid(in).equals(expectedAircraft)||!uuid(in).equals(expectedCockpit))throw new IOException("Telemetry identity/version");
        boolean emergency=in.readBoolean();int mask=in.readUnsignedByte();if((mask&~15)!=0)throw new IOException("Telemetry mask");
        String squawk=in.readUTF();if(!squawk.isEmpty()&&!squawk.matches("[0-7]{4}"))throw new IOException("Squawk");
        StringBuilder summary=new StringBuilder("M5");
        if((mask&1)!=0)summary.append(String.format(Locale.ROOT," POS %.0f/%.0f/%.0f",finite(in),finite(in),finite(in)));
        if((mask&2)!=0){double vx=finite(in),vy=finite(in),vz=finite(in),speed=finite(in);summary.append(String.format(Locale.ROOT," V %.1f/%.1f/%.1f SPD %.1f",vx,vy,vz,speed));}
        if((mask&4)!=0)summary.append(String.format(Locale.ROOT," HDG %.0f",finite(in)));
        if((mask&8)!=0){if(!uuid(in).equals(expectedMission))throw new IOException("Mission binding");String m1=in.readUTF(),m2=in.readUTF();if(!m1.matches("(?:[0-9]{2})?")||!m2.matches("(?:[0-9]{4})?"))throw new IOException("Mission codes");summary.append(" M1 ").append(m1).append(" M2 ").append(m2);}
        if(in.available()!=0)throw new IOException("Trailing telemetry");return new Verified(emergency,squawk,summary.toString());
    }
    private static double finite(DataInputStream in)throws IOException{double v=in.readDouble();if(!Double.isFinite(v))throw new IOException("Non-finite telemetry");return v;}
    private static void uuid(DataOutputStream out,UUID id)throws IOException{out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());}
    private static UUID uuid(DataInputStream in)throws IOException{return new UUID(in.readLong(),in.readLong());}
    public record Verified(boolean emergency,String squawk,String summary) { }
}
