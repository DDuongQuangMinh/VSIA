package com.k1ngtle.vsia.signality.engineering.nr;
import com.k1ngtle.vsia.signality.engineering.phy.CodingProfile;
import com.k1ngtle.vsia.signality.engineering.phy.Modulation;
public final class NrThroughputModel{
    private NrThroughputModel(){}
    public static double codedBitsPerSlot(NrResourceGrid g,Modulation m,CodingProfile c,int layers){
        if(layers<1)throw new IllegalArgumentException("layers");
        return g.dataResourceElements()*m.bitsPerSymbol()*c.rate()*layers;
    }
    public static double estimatedBitsPerSecond(NrResourceGrid g,Modulation m,CodingProfile c,int layers){
        return codedBitsPerSlot(g,m,c,layers)/g.numerology().slotDurationSeconds();
    }
}
