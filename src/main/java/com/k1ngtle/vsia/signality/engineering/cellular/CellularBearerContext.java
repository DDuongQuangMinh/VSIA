package com.k1ngtle.vsia.signality.engineering.cellular;

import com.k1ngtle.vsia.signality.engineering.cellular.mac.HarqManager;
import com.k1ngtle.vsia.signality.engineering.cellular.pdcp.PdcpEntity;
import com.k1ngtle.vsia.signality.engineering.cellular.pdcp.PdcpSecurityContext;
import com.k1ngtle.vsia.signality.engineering.cellular.rlc.RlcAmEntity;

public final class CellularBearerContext {
    private final HarqManager harq =
            new HarqManager(8);

    private final RlcAmEntity rlc =
            new RlcAmEntity();

    private final PdcpEntity pdcp =
            new PdcpEntity();

    private PdcpSecurityContext security;

    public HarqManager harq() {
        return harq;
    }

    public RlcAmEntity rlc() {
        return rlc;
    }

    public PdcpEntity pdcp() {
        return pdcp;
    }

    public PdcpSecurityContext security() {
        return security;
    }

    public void setSecurity(
            PdcpSecurityContext security
    ) {
        this.security = security;
    }

    public boolean secured() {
        return security != null;
    }
}
