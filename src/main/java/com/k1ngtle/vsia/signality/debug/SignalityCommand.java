package com.k1ngtle.vsia.signality.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.api.signal.PathLossModel;
import com.k1ngtle.vsia.signality.api.signal.SignalBand;
import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import com.k1ngtle.vsia.signality.core.scan.RadarScanScheduler;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class SignalityCommand {
   private static final Map<UUID, SignalityCommand.PlayerListener> ACTIVE_LISTENERS = new HashMap<>();

   private SignalityCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("signality")
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("radar")
                                    .then(Commands.literal("list").executes(SignalityCommand::radarList)))
                                 .then(Commands.literal("nearest").executes(SignalityCommand::radarNearest)))
                              .then(((LiteralArgumentBuilder)Commands.literal("ping").requires(s -> s.hasPermission(2))).executes(SignalityCommand::radarPing)))
                           .then(
                              ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("visualize").requires(s -> s.hasPermission(2)))
                                    .executes(SignalityCommand::radarVisualizeNearest))
                                 .then(
                                    Commands.literal("id")
                                       .then(Commands.argument("emitterId", StringArgumentType.string()).executes(SignalityCommand::radarVisualizeById))
                                 )
                           ))
                        .then(Commands.literal("targets").executes(SignalityCommand::radarTargets))
                  ))
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("signal")
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("broadcast").requires(s -> s.hasPermission(2)))
                                 .then(
                                    Commands.argument("freqHz", DoubleArgumentType.doubleArg(1.0))
                                       .then(Commands.argument("powerW", DoubleArgumentType.doubleArg(0.001)).executes(SignalityCommand::signalBroadcast))
                                 )
                           ))
                        .then(
                           Commands.literal("listen")
                              .then(
                                 Commands.argument("freqHz", DoubleArgumentType.doubleArg(1.0))
                                    .then(Commands.argument("bandwidthHz", DoubleArgumentType.doubleArg(1.0)).executes(SignalityCommand::signalListen))
                              )
                        ))
                     .then(Commands.literal("unlisten").executes(SignalityCommand::signalUnlisten))
               ))
            .then(Commands.literal("stats").executes(SignalityCommand::stats))
      );
   }

   private static int radarList(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack src = (CommandSourceStack)ctx.getSource();
      ServerLevel level = src.getLevel();
      List<IRadarEmitter> emitters = RadarRegistry.emittersIn(level);
      if (emitters.isEmpty()) {
         src.sendSuccess(() -> Component.literal("No active radars in this level."), false);
         return 0;
      } else {
         src.sendSuccess(() -> Component.literal("Active radars (" + emitters.size() + "):").withStyle(ChatFormatting.AQUA), false);

         for (IRadarEmitter e : emitters) {
            Vec3 o = e.originWorld();
            String viz = DebugVisualization.isVisualized(e.id()) ? "  [viz]" : "";
            src.sendSuccess(
               () -> Component.literal(
                     String.format(
                        "  %s  @(%.1f,%.1f,%.1f)  mode=%s  PRF=%.1fHz  band=%s%s",
                        shortId(e.id()),
                        o.x,
                        o.y,
                        o.z,
                        e.mode().name(),
                        e.profile().pulseRepetitionHz(),
                        e.profile().band().name(),
                        viz
                     )
                  ),
               false
            );
         }

         return emitters.size();
      }
   }

   private static int radarNearest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack src = (CommandSourceStack)ctx.getSource();
      IRadarEmitter e = nearestEmitter(src);
      Vec3 o = e.originWorld();
      Vec3 a = e.axisWorld();
      src.sendSuccess(
         () -> Component.literal(
                  String.format(
                     "Nearest radar: %s%n  origin=(%.1f,%.1f,%.1f)  axis=(%.2f,%.2f,%.2f)%n  range=%.0fm  half-beam=%.1fÂ°  Pt=%.0fW  G=%.0f  Î»=%.4fm",
                     shortId(e.id()),
                     o.x,
                     o.y,
                     o.z,
                     a.x,
                     a.y,
                     a.z,
                     e.profile().maxRangeMeters(),
                     Math.toDegrees(e.profile().halfBeamWidthRad()),
                     e.profile().peakPowerWatts(),
                     e.profile().antennaGain(),
                     e.profile().wavelengthMeters()
                  )
               )
               .withStyle(ChatFormatting.AQUA),
         false
      );
      return 1;
   }

   private static int radarPing(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      CommandSourceStack src = (CommandSourceStack)ctx.getSource();
      IRadarEmitter e = nearestEmitter(src);
      List<RadarContact> contacts = RadarScanScheduler.scanNow(src.getLevel(), e);
      src.sendSuccess(() -> Component.literal("Ping on " + shortId(e.id()) + " â†’ " + contacts.size() + " contacts").withStyle(ChatFormatting.GREEN), false);

      for (RadarContact c : contacts) {
         src.sendSuccess(
            () -> Component.literal(
                  String.format(
                     "  tgt=%s  R=%.0fm  brg=%+.1fÂ°  el=%+.1fÂ°  v_r=%+.1fm/s  SNR=%.1fdB  %s",
                     shortId(c.targetId()),
                     c.rangeMeters(),
                     Math.toDegrees(c.bearingRad()),
                     Math.toDegrees(c.elevationRad()),
                     c.closureRateMps(),
                     10.0 * Math.log10(Math.max(1.0E-9, c.signalToNoiseRatio())),
                     c.trackQuality() ? "[track]" : ""
                  )
               ),
            false
         );
      }

      return contacts.size();
   }

   private static int radarTargets(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack src = (CommandSourceStack)ctx.getSource();
      ServerLevel level = src.getLevel();
      List<IRadarTarget> all = new ArrayList<>();

      for (RadarRegistry.TargetSource s : RadarRegistry.targetSources()) {
         try (Stream<IRadarTarget> stream = s.apply(level)) {
            if (stream != null) {
               stream.forEach(all::add);
            }
         } catch (Throwable var15) {
            src.sendFailure(Component.literal("Target source threw: " + var15));
         }
      }

      if (all.isEmpty()) {
         src.sendSuccess(() -> Component.literal("No custom-source targets in this level. (Vanilla entities scan separately.)"), false);
         return 0;
      } else {
         src.sendSuccess(() -> Component.literal("Custom-source targets (" + all.size() + "):").withStyle(ChatFormatting.AQUA), false);
         Vec3 origin = src.getPosition();

         for (IRadarTarget t : all) {
            Vec3 p = t.positionWorld();
            double range = p.distanceTo(origin);
            double rcs = t.radarCrossSection(Math.PI / 2, 0.03);
            String tag = t.vsShip() != null ? "ship" : "custom";
            src.sendSuccess(
               () -> Component.literal(
                     String.format(
                        "  %s [%s]  pos=(%.1f,%.1f,%.1f)  r=%.1fm  R=%.0fm  RCSâ‰ˆ%.0f mÂ²",
                        shortId(t.id()),
                        tag,
                        p.x,
                        p.y,
                        p.z,
                        t.boundingRadius(),
                        range,
                        rcs
                     )
                  ),
               false
            );
         }

         return all.size();
      }
   }

   private static int radarVisualizeNearest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      IRadarEmitter e = nearestEmitter((CommandSourceStack)ctx.getSource());
      return toggleViz((CommandSourceStack)ctx.getSource(), e.id());
   }

   private static int radarVisualizeById(CommandContext<CommandSourceStack> ctx) {
      String idStr = StringArgumentType.getString(ctx, "emitterId");

      UUID id;
      try {
         id = UUID.fromString(idStr);
      } catch (IllegalArgumentException var4) {
         id = RadarRegistry.emittersIn(((CommandSourceStack)ctx.getSource()).getLevel())
            .stream()
            .map(IRadarEmitter::id)
            .filter(u -> shortId(u).equalsIgnoreCase(idStr))
            .findFirst()
            .orElse(null);
         if (id == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Unknown emitter id: " + idStr));
            return 0;
         }
      }

      return toggleViz((CommandSourceStack)ctx.getSource(), id);
   }

   private static int toggleViz(CommandSourceStack src, UUID id) {
      boolean nowOn = DebugVisualization.toggle(id);
      src.sendSuccess(
         () -> Component.literal("Visualization " + (nowOn ? "on" : "off") + " for " + shortId(id))
               .withStyle(nowOn ? ChatFormatting.GREEN : ChatFormatting.GRAY),
         false
      );
      return 1;
   }

   private static int signalBroadcast(CommandContext<CommandSourceStack> ctx) {
      CommandSourceStack src = (CommandSourceStack)ctx.getSource();
      double freqHz = DoubleArgumentType.getDouble(ctx, "freqHz");
      double powerW = DoubleArgumentType.getDouble(ctx, "powerW");
      Vec3 pos = src.getPosition();
      SignalPacket pkt = new SignalPacket(
         UUID.randomUUID(),
         pos,
         freqHz,
         powerW,
         1.0,
         ("/signality broadcast from " + src.getTextName()).getBytes(StandardCharsets.US_ASCII),
         System.nanoTime(),
         1,
         null
      );
      SignalBus.broadcast(pkt, src.getLevel());
      src.sendSuccess(
         () -> Component.literal(
                  String.format("Broadcast %.3f MHz / %s W from (%.1f,%.1f,%.1f).", freqHz / 1000000.0, powerW, pos.x, pos.y, pos.z)
               )
               .withStyle(ChatFormatting.GREEN),
         true
      );
      return 1;
   }

   private static int signalListen(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
      double freqHz = DoubleArgumentType.getDouble(ctx, "freqHz");
      double bwHz = DoubleArgumentType.getDouble(ctx, "bandwidthHz");
      SignalityCommand.PlayerListener prev = ACTIVE_LISTENERS.remove(player.getUUID());
      if (prev != null) {
         SignalBus.unregisterReceiver(prev.id);
      }

      SignalityCommand.PlayerListener listener = new SignalityCommand.PlayerListener(player, freqHz, bwHz);
      ACTIVE_LISTENERS.put(player.getUUID(), listener);
      SignalBus.registerReceiver(listener);
      player.displayClientMessage(
         Component.literal(String.format("Listening on %.3f MHz Â±%.1f kHz", freqHz / 1000000.0, bwHz / 2000.0)).withStyle(ChatFormatting.AQUA), false
      );
      return 1;
   }

   private static int signalUnlisten(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
      SignalityCommand.PlayerListener prev = ACTIVE_LISTENERS.remove(player.getUUID());
      if (prev != null) {
         SignalBus.unregisterReceiver(prev.id);
         player.displayClientMessage(Component.literal("Listening stopped."), false);
         return 1;
      } else {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("You weren't listening."));
         return 0;
      }
   }

   private static int stats(CommandContext<CommandSourceStack> ctx) {
      ((CommandSourceStack)ctx.getSource())
         .sendSuccess(
            () -> Component.literal(
                     String.format(
                        "Scheduler: queued=%d  dropped=%d  contactsBuilt=%d",
                        RadarScanScheduler.jobsQueued(),
                        RadarScanScheduler.jobsDropped(),
                        RadarScanScheduler.contactsBuilt()
                     )
                  )
                  .withStyle(ChatFormatting.AQUA),
            false
         );
      return 1;
   }

   private static IRadarEmitter nearestEmitter(CommandSourceStack src) throws CommandSyntaxException {
      ServerLevel level = src.getLevel();
      Vec3 origin = src.getPosition();
      IRadarEmitter best = null;
      double bestDist = Double.POSITIVE_INFINITY;

      for (IRadarEmitter e : RadarRegistry.emittersIn(level)) {
         double d = e.originWorld().distanceToSqr(origin);
         if (d < bestDist) {
            bestDist = d;
            best = e;
         }
      }

      if (best == null) {
         throw new SimpleCommandExceptionType(Component.literal("No active radars in this level.")).create();
      } else {
         return best;
      }
   }

   private static String shortId(UUID id) {
      String s = id.toString();
      return s.substring(0, 8);
   }

   private static final class PlayerListener implements ISignalReceiver {
      private final UUID id = UUID.randomUUID();
      private final ServerPlayer player;
      private final double freqHz;
      private final double bwHz;

      PlayerListener(ServerPlayer player, double freqHz, double bwHz) {
         this.player = player;
         this.freqHz = freqHz;
         this.bwHz = bwHz;
      }

      @Override
      public UUID id() {
         return this.id;
      }

      @Override
      public ServerLevel level() {
         return this.player.serverLevel();
      }

      @Override
      public Vec3 positionWorld() {
         return this.player.position();
      }

      @Override
      public SignalBand band() {
         return SignalBand.forFrequency(this.freqHz);
      }

      @Override
      public double sensitivityWatts() {
         return 1.0E-15;
      }

      @Override
      public double[] tunedFrequenciesHz() {
         return new double[]{this.freqHz};
      }

      @Override
      public double tuningBandwidthHz() {
         return this.bwHz;
      }

      @Override
      public void onReceive(SignalPacket packet, double receivedPowerWatts) {
         double dbm = PathLossModel.dbm(receivedPowerWatts);
         double range = packet.originWorld().distanceTo(this.positionWorld());
         String payload = packet.payload() == null ? "" : new String(packet.payload(), StandardCharsets.US_ASCII);
         this.player
            .displayClientMessage(
               Component.literal(String.format("[RX] %.3f MHz  %.1f dBm  R=%.0fm  \"%s\"", packet.frequencyHz() / 1000000.0, dbm, range, payload))
                  .withStyle(ChatFormatting.YELLOW),
               true
            );
      }
   }
}
