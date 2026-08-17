package com.k1ngtle.vsia.signality.engineering.phy;
import com.k1ngtle.vsia.signality.engineering.math.Complex;
import com.k1ngtle.vsia.signality.engineering.math.Fft;
import java.util.Arrays;
public final class OfdmEngine {
    private OfdmEngine(){}
    public static Complex[] modulate(Complex[] data,int fftSize,int cp){
        if(Integer.bitCount(fftSize)!=1) throw new IllegalArgumentException("fftSize");
        if(data.length>fftSize) throw new IllegalArgumentException("subcarriers");
        Complex[] f=new Complex[fftSize]; Arrays.fill(f,Complex.ZERO);
        int start=(fftSize-data.length)/2;
        for(int i=0;i<data.length;i++){ int bin=start+i; if(bin!=fftSize/2) f[bin]=data[i]; }
        Complex[] t=Fft.ifft(f);
        if(cp<=0)return t;
        if(cp>fftSize)throw new IllegalArgumentException("cp");
        Complex[] out=new Complex[fftSize+cp];
        System.arraycopy(t,fftSize-cp,out,0,cp);
        System.arraycopy(t,0,out,cp,fftSize);
        return out;
    }
}
