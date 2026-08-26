package com.k1ngtle.vsia.signality.integration.vs.mixin;

import com.k1ngtle.vsia.signality.integration.vs.access.SignalityShipSnapshot;

import java.util.concurrent.atomic.AtomicReference;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;

@Mixin(
   targets = {"org.valkyrienskies.core.impl.game.ships.ShipObjectServer"},
   remap = false
)
public abstract class ShipObjectServerMixin implements SignalityShipSnapshot {
   @Unique
   private final AtomicReference<SignalityShipSnapshot.Snapshot> signality$snapshot = new AtomicReference<>();

   @Shadow
   public abstract ShipTransform getTransform();

   @Shadow
   public abstract Vector3dc getVelocity();

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")},
      remap = false,
      require = 0
   )
   private void signality$captureSnapshot(CallbackInfo ci) {
      long gameTime = 0L;
      this.signality$snapshot.set(new SignalityShipSnapshot.Snapshot(this.getTransform().getShipToWorld(), this.getVelocity(), gameTime));
   }

   @Override
   public SignalityShipSnapshot.Snapshot signality$readSnapshot() {
      return this.signality$snapshot.get();
   }
}
