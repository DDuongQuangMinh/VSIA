package com.k1ngtle.vsia.signality.engineering.nr;
public record NrNumerology(int mu,boolean normalCyclicPrefix){
    public NrNumerology{if(mu<0||mu>6)throw new IllegalArgumentException("mu");}
    public double subcarrierSpacingHz(){return 15_000.0*(1<<mu);}
    public int slotsPerSubframe(){return 1<<mu;}
    public double slotDurationSeconds(){return .001/slotsPerSubframe();}
    public int ofdmSymbolsPerSlot(){return normalCyclicPrefix?14:12;}
    public double usefulSymbolDurationSeconds(){return 1.0/subcarrierSpacingHz();}
}
