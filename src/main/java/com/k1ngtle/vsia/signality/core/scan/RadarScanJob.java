package com.k1ngtle.vsia.signality.core.scan;

import com.k1ngtle.vsia.signality.api.geom.Cone;
import com.k1ngtle.vsia.signality.api.geom.ConeIntersection;
import com.k1ngtle.vsia.signality.api.occlusion.CompositeOcclusionProvider;
import com.k1ngtle.vsia.signality.api.radar.RadarMode;
import com.k1ngtle.vsia.signality.api.radar.RadarProfile;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.k1ngtle.vsia.signality.api.rcs.DefaultRcsProviders;
import com.k1ngtle.vsia.signality.core.math.BeamGeometry;
import com.k1ngtle.vsia.signality.core.math.ClutterModel;
import com.k1ngtle.vsia.signality.core.math.DopplerFilter;
import com.k1ngtle.vsia.signality.core.math.RadarEquation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

public final class RadarScanJob {
   public static final int MAX_CANDIDATES = 256;

   private RadarScanJob() {
   }

   public static List<RadarScanJob.CandidateContact> run(RadarSnapshot.EmitterSnap emitter, RadarSnapshot snapshot) {
      RadarProfile profile = emitter.profile();
      RadarMode mode = emitter.mode();
      double halfAngle = profile.halfBeamWidthRad() * mode.beamWidthMultiplier();
      Cone cone = Cone.of(emitter.originWorld(), emitter.axisWorld(), halfAngle, profile.maxRangeMeters());
      double grazing = BeamGeometry.grazingAngle(emitter.axisWorld());
      boolean lookDown = ClutterModel.isLookDown(grazing);
      CompositeOcclusionProvider occlusionChain = RadarRegistry.occlusionProviders();
      List<RadarScanJob.CandidateContact> out = new ArrayList<>();

      for (RadarSnapshot.TargetSnap target : snapshot.targets()) {
         if (out.size() >= 256) {
            break;
         }

         if (!sameHull(emitter, target)
            && !target.id().equals(emitter.id())
            && ConeIntersection.containsSphere(cone, target.positionWorld(), target.boundingRadius())) {
            Vec3 toTarget = target.positionWorld().subtract(emitter.originWorld());
            double range = toTarget.length();
            if (!(range < 0.001)) {
               Vec3 los = toTarget.scale(1.0 / range);
               double offAxis = BeamGeometry.angleBetween(emitter.axisWorld(), los);
               double aspect = DefaultRcsProviders.AspectAngle.broadsideWeight(0.0);
               aspect = aspectAngle(emitter.originWorld(), target);
               double bearing = bearing(emitter.axisWorld(), los);
               double elevation = elevation(emitter.axisWorld(), los);
               double rcs = RadarRegistry.resolveRcs(target.liveRef(), emitter.liveRef(), aspect);
               double effectiveGain = BeamGeometry.offAxisGain(profile.antennaGain(), offAxis, profile.halfBeamWidthRad());
               double targetEcho = RadarEquation.receivedPowerWatts(
                  profile.peakPowerWatts(), effectiveGain, profile.wavelengthMeters(), profile.systemLossLinear(), rcs, range
               );
               double snr = targetEcho / profile.noisePowerWatts();
               if (!(snr < profile.minDetectableSnr())) {
                  double closure = BeamGeometry.closureRateMps(emitter.originWorld(), emitter.velocityWorld(), target.positionWorld(), target.velocityWorld());
                  double clutter = 0.0;
                  double scrAfterMti;
                  boolean trackQuality;
                  if (lookDown && profile.band().isClutterProne()) {
                     double sigma0 = ClutterModel.defaultSigmaZero(grazing);
                     clutter = ClutterModel.clutterPowerWatts(profile, range, grazing, sigma0);
                     double rawScr = ClutterModel.scr(targetEcho, clutter);
                     double mti = DopplerFilter.clutterSuppressionFactor(closure, profile.dopplerGateMps());
                     scrAfterMti = rawScr * mti;
                     trackQuality = snr >= profile.minDetectableSnr() * 3.0 && scrAfterMti >= 1.0;
                     if (scrAfterMti < 0.1) {
                        continue;
                     }
                  } else {
                     scrAfterMti = Double.POSITIVE_INFINITY;
                     trackQuality = snr >= profile.minDetectableSnr() * 3.0;
                  }

                  Vec3 losEnd = occlusionEndpoint(emitter.originWorld(), target.positionWorld(), target.boundingRadius());
                  if (!occlusionChain.isOccludedThreadSafe(snapshot.level(), emitter.originWorld(), losEnd)) {
                     out.add(
                        new RadarScanJob.CandidateContact(
                           emitter, target, range, bearing, elevation, closure, aspect, targetEcho, clutter, snr, scrAfterMti, trackQuality
                        )
                     );
                  }
               }
            }
         }
      }

      return out;
   }

   private static double aspectAngle(Vec3 emitterPos, RadarSnapshot.TargetSnap target) {
      Vec3 vel = target.velocityWorld();
      double speed = vel.length();
      if (speed < 0.001) {
         return Math.PI / 2;
      } else {
         Vec3 toTarget = target.positionWorld().subtract(emitterPos);
         double r = toTarget.length();
         if (r < 1.0E-6) {
            return 0.0;
         } else {
            double dot = (toTarget.x * vel.x + toTarget.y * vel.y + toTarget.z * vel.z) / (r * speed);
            return Math.acos(Math.max(-1.0, Math.min(1.0, -dot)));
         }
      }
   }

   private static double bearing(Vec3 axis, Vec3 los) {
      double ax = axis.x;
      double az = axis.z;
      double lx = los.x;
      double lz = los.z;
      double dot = ax * lx + az * lz;
      double cross = ax * lz - az * lx;
      return Math.atan2(cross, dot);
   }

   private static double elevation(Vec3 axis, Vec3 los) {
      return Math.asin(Math.max(-1.0, Math.min(1.0, los.y))) - Math.asin(Math.max(-1.0, Math.min(1.0, axis.y)));
   }

   private static boolean sameHull(RadarSnapshot.EmitterSnap e, RadarSnapshot.TargetSnap t) {
      return e.vsShipHandle() != null && e.vsShipHandle() == t.vsShipHandle();
   }

   public static Vec3 occlusionEndpoint(Vec3 from, Vec3 toCenter, double targetRadius) {
      Vec3 delta = toCenter.subtract(from);
      double total = delta.length();
      if (total <= targetRadius + 0.001) {
         return from;
      } else {
         double t = (total - targetRadius) / total;
         return from.add(delta.scale(t));
      }
   }

   public static record CandidateContact(
      RadarSnapshot.EmitterSnap emitter,
      RadarSnapshot.TargetSnap target,
      double rangeMeters,
      double bearingRad,
      double elevationRad,
      double closureRateMps,
      double aspectRad,
      double targetEchoWatts,
      double clutterPowerWatts,
      double snrLinear,
      double scrAfterMtiLinear,
      boolean trackQualityCandidate
   ) {
   }
}
