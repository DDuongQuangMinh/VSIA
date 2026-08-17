package com.k1ngtle.vsia.signality.engineering.wifi;
import java.util.Random;
public final class CsmaCaController {
    private final int cwMin,cwMax; private int cw,backoff;
    public CsmaCaController(int cwMin,int cwMax,Random r){
        if(cwMin<1||cwMax<cwMin)throw new IllegalArgumentException();
        this.cwMin=cwMin;this.cwMax=cwMax;this.cw=cwMin;select(r);
    }
    public int contentionWindow(){return cw;}
    public int backoffSlots(){return backoff;}
    public boolean tickIdleSlot(){if(backoff>0)backoff--;return backoff==0;}
    public void onSuccess(Random r){cw=cwMin;select(r);}
    public void onFailure(Random r){cw=Math.min(cwMax,((cw+1)*2)-1);select(r);}
    public void onMediumBusy(){}
    private void select(Random r){backoff=r.nextInt(cw+1);}
}
