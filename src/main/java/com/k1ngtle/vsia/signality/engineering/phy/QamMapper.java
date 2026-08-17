package com.k1ngtle.vsia.signality.engineering.phy;
import com.k1ngtle.vsia.signality.engineering.math.Complex;
public final class QamMapper {
    private QamMapper(){}
    public static Complex[] map(boolean[] bits,Modulation m){
        int bps=m.bitsPerSymbol();
        if(bits.length%bps!=0) throw new IllegalArgumentException("bit count");
        if(m==Modulation.BPSK){
            Complex[] out=new Complex[bits.length];
            for(int i=0;i<bits.length;i++) out[i]=new Complex(bits[i]?1:-1,0);
            return out;
        }
        if(!m.isSquareQam()) throw new IllegalArgumentException("square QAM required");
        int side=(int)Math.sqrt(m.order()),axisBits=bps/2;
        double norm=Math.sqrt((2.0/3.0)*(m.order()-1));
        Complex[] out=new Complex[bits.length/bps];
        for(int s=0;s<out.length;s++){
            int off=s*bps;
            int ig=read(bits,off,axisBits), qg=read(bits,off+axisBits,axisBits);
            int ib=grayToBinary(ig), qb=grayToBinary(qg);
            out[s]=new Complex((2.0*ib-(side-1))/norm,(2.0*qb-(side-1))/norm);
        }
        return out;
    }
    private static int read(boolean[] b,int off,int n){ int v=0; for(int i=0;i<n;i++) v=(v<<1)|(b[off+i]?1:0); return v; }
    private static int grayToBinary(int g){ int b=g; for(int s=g>>1;s!=0;s>>=1)b^=s; return b; }
}
