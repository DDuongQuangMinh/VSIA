package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

public final class IcmpErrorModel {
    public static final int DESTINATION_UNREACHABLE=3;
    public static final int TIME_EXCEEDED=11;

    private IcmpErrorModel(){}

    public static int internetChecksum(byte[] data){
        long sum=0;
        for(int i=0;i<data.length;i+=2){
            int word=(data[i]&255)<<8;
            if(i+1<data.length) word|=data[i+1]&255;
            sum+=word;
            while((sum>>>16)!=0) sum=(sum&0xffff)+(sum>>>16);
        }
        return (int)(~sum)&0xffff;
    }

    public static byte[] encode(int type,int code,byte[] quoted){
        byte[] q=quoted==null?new byte[0]:quoted;
        byte[] out=new byte[8+q.length];
        out[0]=(byte)type;
        out[1]=(byte)code;
        System.arraycopy(q,0,out,8,q.length);
        int c=internetChecksum(out);
        out[2]=(byte)(c>>>8);
        out[3]=(byte)c;
        return out;
    }
}
