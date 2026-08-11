package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.SignalityBlocks;
import com.k1ngtle.vsia.signality.api.signal.ISignalTransmitter;
import com.k1ngtle.vsia.signality.api.signal.SignalBand;
import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SignalTestEmitterBlockEntity extends BlockEntity implements ISignalTransmitter {
   public static final SignalTestEmitterBlockEntity.Channel[] CHANNELS = new SignalTestEmitterBlockEntity.Channel[]{
      new SignalTestEmitterBlockEntity.Channel("VHF-145MHz-5W", 1.455E8, 5.0, SignalBand.VHF),
      new SignalTestEmitterBlockEntity.Channel("UHF-446MHz-0.5W", 4.46E8, 0.5, SignalBand.UHF),
      new SignalTestEmitterBlockEntity.Channel("X-10GHz-100W", 1.0E10, 100.0, SignalBand.SHF)
   };
   private static final int BROADCAST_INTERVAL_TICKS = 40;
   private UUID id = UUID.randomUUID();
   private int channelIndex = 0;
   private int tickCounter = 0;
   private long broadcastCount = 0L;

   public SignalTestEmitterBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)SignalityBlocks.SIGNAL_TEST_EMITTER_BE.get(), pos, state);
   }

   public SignalTestEmitterBlockEntity.Channel channel() {
      return CHANNELS[this.channelIndex];
   }

   public SignalTestEmitterBlockEntity.Channel cycleChannel() {
      this.channelIndex = (this.channelIndex + 1) % CHANNELS.length;
      this.setChanged();
      return this.channel();
   }

   public long broadcastCount() {
      return this.broadcastCount;
   }

   public void serverTick() {
      if (this.level != null) {
         if (++this.tickCounter >= 40) {
            this.tickCounter = 0;
            this.broadcastOne();
         }
      }
   }

   public void broadcastOne() {
      if (this.level instanceof ServerLevel sl) {
         SignalTestEmitterBlockEntity.Channel var5 = this.channel();
         byte[] payload = ("PING#" + this.broadcastCount).getBytes(StandardCharsets.US_ASCII);
         SignalPacket packet = new SignalPacket(this.id, this.positionWorld(), var5.frequencyHz(), var5.powerW(), 1.0, payload, System.nanoTime(), 4, null);
         SignalBus.broadcast(packet, sl);
         this.broadcastCount++;
      }
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
      return this.channel().band;
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putUUID("Id", this.id);
      tag.putInt("ChannelIndex", this.channelIndex);
      tag.putLong("BroadcastCount", this.broadcastCount);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      if (tag.hasUUID("Id")) {
         this.id = tag.getUUID("Id");
      }

      if (tag.contains("ChannelIndex")) {
         int v = tag.getInt("ChannelIndex");
         if (v >= 0 && v < CHANNELS.length) {
            this.channelIndex = v;
         }
      }

      if (tag.contains("BroadcastCount")) {
         this.broadcastCount = tag.getLong("BroadcastCount");
      }
   }

   public static record Channel(String label, double frequencyHz, double powerW, SignalBand band) {
   }
}
