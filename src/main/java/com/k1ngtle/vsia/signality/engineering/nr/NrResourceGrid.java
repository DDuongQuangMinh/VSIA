package com.k1ngtle.vsia.signality.engineering.nr;
public record NrResourceGrid(NrNumerology numerology,int resourceBlocks,int allocatedSymbols,int dmrsResourceElements,int otherOverheadResourceElements){
    public NrResourceGrid{if(numerology==null||resourceBlocks<1||allocatedSymbols<1)throw new IllegalArgumentException();}
    public int totalResourceElements(){return resourceBlocks*12*allocatedSymbols;}
    public int dataResourceElements(){return Math.max(0,totalResourceElements()-dmrsResourceElements-otherOverheadResourceElements);}
}
