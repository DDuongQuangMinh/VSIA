package com.k1ngtle.vsia.signality.core.scan;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.api.events.ContactDetectedEvent;
import com.k1ngtle.vsia.signality.api.events.RadarScanEvent;
import com.k1ngtle.vsia.signality.api.occlusion.CompositeOcclusionProvider;
import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

public final class RadarScanScheduler {
   private static volatile ExecutorService POOL;
   private static final Map<UUID, Long> NEXT_SCAN_TICK = new HashMap<>();
   private static final ConcurrentLinkedQueue<RadarScanScheduler.CompletedScan> RESULTS = new ConcurrentLinkedQueue<>();
   private static final AtomicLong JOBS_QUEUED = new AtomicLong();
   private static final AtomicLong JOBS_DROPPED = new AtomicLong();
   private static final AtomicLong CONTACTS_BUILT = new AtomicLong();
   private static final int MAX_SUBSCANS_PER_TICK = 20;

   private RadarScanScheduler() {
   }

   public static synchronized void start(int workers) {
      if (POOL == null) {
         if (workers < 1) {
            workers = 1;
         }

         AtomicInteger counter = new AtomicInteger();
         ThreadFactory factory = r -> {
            Thread t = new Thread(r, "Signality-RadarWorker-" + counter.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(4);
            return t;
         };
         POOL = Executors.newFixedThreadPool(workers, factory);
         Signality.LOGGER.info("RadarScanScheduler started with {} worker(s)", workers);
      }
   }

   public static synchronized void stop() {
      if (POOL != null) {
         POOL.shutdownNow();
         POOL = null;
         NEXT_SCAN_TICK.clear();
         RESULTS.clear();
         Signality.LOGGER
            .info(
               "RadarScanScheduler stopped (queued={}, dropped={}, contactsBuilt={})",
               new Object[]{JOBS_QUEUED.get(), JOBS_DROPPED.get(), CONTACTS_BUILT.get()}
            );
      }
   }

   public static void onServerTick(ServerLevel level) {
      if (POOL != null) {
         long now = level.getGameTime();
         RadarSnapshot snapshot = SnapshotCollector.collect(level);

         for (RadarSnapshot.EmitterSnap emitterSnap : snapshot.emitters()) {
            double prf = emitterSnap.profile().pulseRepetitionHz();
            if (prf >= 20.0) {
               int scans = (int)Math.round(prf / 20.0);
               scans = Math.max(1, Math.min(20, scans));

               for (int i = 0; i < scans; i++) {
                  double frac = scans == 1 ? 0.0 : (double)i / (double)scans;
                  RadarSnapshot.EmitterSnap subSnap = withSubTickAxis(emitterSnap, frac);
                  dispatch(snapshot, subSnap);
               }

               NEXT_SCAN_TICK.put(emitterSnap.id(), now);
            } else {
               long next = NEXT_SCAN_TICK.getOrDefault(emitterSnap.id(), 0L);
               if (now >= next) {
                  int interval = Math.max(1, emitterSnap.profile().ticksPerScan());
                  NEXT_SCAN_TICK.put(emitterSnap.id(), now + (long)interval);
                  dispatch(snapshot, emitterSnap);
               }
            }
         }

         drain(level, 32);
      }
   }

   private static RadarSnapshot.EmitterSnap withSubTickAxis(RadarSnapshot.EmitterSnap base, double subTickFraction) {
      return subTickFraction == 0.0
         ? base
         : new RadarSnapshot.EmitterSnap(
            base.id(),
            base.level(),
            base.originWorld(),
            base.liveRef().axisWorldAt(subTickFraction),
            base.velocityWorld(),
            base.profile(),
            base.mode(),
            base.vsShipHandle(),
            base.liveRef()
         );
   }

   private static void dispatch(RadarSnapshot snapshot, RadarSnapshot.EmitterSnap emitter) {
      RadarScanEvent.Pre pre = new RadarScanEvent.Pre(emitter.liveRef());
      if (!MinecraftForge.EVENT_BUS.post(pre)) {
         try {
            JOBS_QUEUED.incrementAndGet();
            CompletableFuture<List<RadarScanJob.CandidateContact>> future = CompletableFuture.supplyAsync(() -> RadarScanJob.run(emitter, snapshot), POOL);
            future.whenComplete((candidates, error) -> {
               if (error != null) {
                  Signality.LOGGER.warn("Radar scan failed for emitter {}", emitter.id(), error);
               } else {
                  RESULTS.offer(new RadarScanScheduler.CompletedScan(emitter, (List<RadarScanJob.CandidateContact>)candidates));
               }
            });
         } catch (Throwable var4) {
            JOBS_DROPPED.incrementAndGet();
            Signality.LOGGER.warn("Failed to submit radar scan for {}", emitter.id(), var4);
         }
      }
   }

   private static void drain(ServerLevel level, int budget) {
      CompositeOcclusionProvider occlusion = RadarRegistry.occlusionProviders();
      int processed = 0;

      RadarScanScheduler.CompletedScan scan;
      while (processed < budget && (scan = RESULTS.poll()) != null) {
         if (scan.emitter.level() == level) {
            List<RadarContact> contacts = new ArrayList<>(scan.candidates.size());

            for (RadarScanJob.CandidateContact c : scan.candidates) {
               Vec3 from = c.emitter().originWorld();
               Vec3 losEnd = RadarScanJob.occlusionEndpoint(from, c.target().positionWorld(), c.target().boundingRadius());
               if (!occlusion.isOccluded(level, from, losEnd)) {
                  RadarContact contact = new RadarContact(
                     c.emitter().id(),
                     c.target().id(),
                     c.target().positionWorld(),
                     c.target().velocityWorld(),
                     c.rangeMeters(),
                     c.bearingRad(),
                     c.elevationRad(),
                     c.closureRateMps(),
                     c.snrLinear(),
                     c.scrAfterMtiLinear(),
                     c.trackQualityCandidate(),
                     c.target().vsShipHandle()
                  );
                  ContactDetectedEvent detEvt = new ContactDetectedEvent(scan.emitter.liveRef(), contact);
                  if (!MinecraftForge.EVENT_BUS.post(detEvt)) {
                     contacts.add(contact);
                     CONTACTS_BUILT.incrementAndGet();
                  }
               }
            }

            MinecraftForge.EVENT_BUS.post(new RadarScanEvent.Post(scan.emitter.liveRef(), contacts));

            try {
               scan.emitter.liveRef().onContacts(contacts);
            } catch (Throwable var12) {
               Signality.LOGGER.warn("Emitter {} threw in onContacts", scan.emitter.id(), var12);
            }

            processed++;
         }
      }
   }

   public static List<RadarContact> scanNow(ServerLevel level, IRadarEmitter emitter) {
      RadarSnapshot snapshot = SnapshotCollector.collect(level);
      RadarSnapshot.EmitterSnap snap = snapshot.emitters().stream().filter(e -> e.id().equals(emitter.id())).findFirst().orElse(null);
      if (snap == null) {
         return List.of();
      } else {
         List<RadarScanJob.CandidateContact> candidates = RadarScanJob.run(snap, snapshot);
         List<RadarContact> contacts = new ArrayList<>(candidates.size());
         CompositeOcclusionProvider occlusion = RadarRegistry.occlusionProviders();

         for (RadarScanJob.CandidateContact c : candidates) {
            if (!occlusion.isOccluded(level, c.emitter().originWorld(), c.target().positionWorld())) {
               contacts.add(
                  new RadarContact(
                     c.emitter().id(),
                     c.target().id(),
                     c.target().positionWorld(),
                     c.target().velocityWorld(),
                     c.rangeMeters(),
                     c.bearingRad(),
                     c.elevationRad(),
                     c.closureRateMps(),
                     c.snrLinear(),
                     c.scrAfterMtiLinear(),
                     c.trackQualityCandidate(),
                     c.target().vsShipHandle()
                  )
               );
            }
         }

         return contacts;
      }
   }

   public static long jobsQueued() {
      return JOBS_QUEUED.get();
   }

   public static long jobsDropped() {
      return JOBS_DROPPED.get();
   }

   public static long contactsBuilt() {
      return CONTACTS_BUILT.get();
   }

   private static record CompletedScan(RadarSnapshot.EmitterSnap emitter, List<RadarScanJob.CandidateContact> candidates) {
   }
}
