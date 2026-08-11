package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.api.signal.PathLossModel;
import com.k1ngtle.vsia.signality.api.signal.SignalBand;
import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SignalTestReceiverBlockEntity extends BlockEntity implements ISignalReceiver {
   private static final double DEFAULT_SENSITIVITY_WATTS = 1.0E-14;
   private static final double TUNING_BANDWIDTH_HZ = 1000000.0;
   private UUID id = UUID.randomUUID();
   private int channelIndex = 0;
   private boolean chatEnabled = true;
   private long receivedCount = 0L;

   public SignalTestReceiverBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)SignalityBlocks.SIGNAL_TEST_RECEIVER_BE.get(), pos, state);
   }

   public SignalTestEmitterBlockEntity.Channel channel() {
      return SignalTestEmitterBlockEntity.CHANNELS[this.channelIndex];
   }

   public SignalTestEmitterBlockEntity.Channel cycleChannel() {
      this.channelIndex = (this.channelIndex + 1) % SignalTestEmitterBlockEntity.CHANNELS.length;
      this.setChanged();
      return this.channel();
   }

   public boolean toggleChat() {
      this.chatEnabled = !this.chatEnabled;
      this.setChanged();
      return this.chatEnabled;
   }

   public long receivedCount() {
      return this.receivedCount;
   }

   public void onLoad() {
      super.onLoad();
      if (this.level != null && !this.level.isClientSide) {
         SignalBus.registerReceiver(this);
      }
   }

   public void setRemoved() {
      SignalBus.unregisterReceiver(this.id);
      super.setRemoved();
   }

   @Override
   public UUID id() {
      return this.id;
   }

   @Override
   public ServerLevel level() {
      return (ServerLevel)this.level;
   }

   @Override
   public Vec3 positionWorld() {
      return Vec3.atCenterOf(this.worldPosition).add(0.0, 0.5, 0.0);
   }

   @Override
   public SignalBand band() {
      return this.channel().band();
   }

   @Override
   public double sensitivityWatts() {
      return 1.0E-14;
   }

   @Override
   public double[] tunedFrequenciesHz() {
      return new double[]{this.channel().frequencyHz()};
   }

   @Override
   public double tuningBandwidthHz() {
      return 1000000.0;
   }

   @Override
   public void onReceive(SignalPacket packet, double receivedPowerWatts) {
      this.receivedCount++;
      if (this.chatEnabled && this.level != null) {
         String payload = packet.payload() == null ? "" : new String(packet.payload(), StandardCharsets.US_ASCII);
         double dbm = PathLossModel.dbm(receivedPowerWatts);
         double range = packet.originWorld().distanceTo(this.positionWorld());
         Component msg = Component.literal(
            String.format(
               "[RX %s] %.2f MHz  %.1f dBm  range=%.1fm  payload=%s", this.positionWorldString(), packet.frequencyHz() / 1000000.0, dbm, range, payload
            )
         );
         AABB near = new AABB(this.worldPosition).inflate(32.0);

         for (Player player : this.level.players()) {
            if (near.contains(player.position())) {
               player.displayClientMessage(msg, false);
            }
         }
      }
   }

   private String positionWorldString() {
      return "(" + this.worldPosition.getX() + "," + this.worldPosition.getY() + "," + this.worldPosition.getZ() + ")";
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putUUID("Id", this.id);
      tag.putInt("ChannelIndex", this.channelIndex);
      tag.putBoolean("ChatEnabled", this.chatEnabled);
      tag.putLong("ReceivedCount", this.receivedCount);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      if (tag.hasUUID("Id")) {
         this.id = tag.getUUID("Id");
      }

      if (tag.contains("ChannelIndex")) {
         int v = tag.getInt("ChannelIndex");
         if (v >= 0 && v < SignalTestEmitterBlockEntity.CHANNELS.length) {
            this.channelIndex = v;
         }
      }

      if (tag.contains("ChatEnabled")) {
         this.chatEnabled = tag.getBoolean("ChatEnabled");
      }

      if (tag.contains("ReceivedCount")) {
         this.receivedCount = tag.getLong("ReceivedCount");
      }
   }
}
