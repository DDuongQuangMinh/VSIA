package com.k1ngtle.vsia.signality.integration.vs;

import com.k1ngtle.vsia.signality.api.geom.Obb;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4dc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class VsHookImpl implements VsCompat.VsHook {
   @Override
   public Object shipManagingPos(ServerLevel level, BlockPos pos) {
      return VSGameUtilsKt.getShipManagingPos(level, pos);
   }

   @Override
   public Vec3 transformShipToWorld(Object ship, Vec3 shipPos) {
      Matrix4dc m = ((Ship)ship).getTransform().getShipToWorld();
      Vector3d v = new Vector3d(shipPos.x, shipPos.y, shipPos.z);
      m.transformPosition(v);
      return new Vec3(v.x, v.y, v.z);
   }

   @Override
   public Vec3 transformDirShipToWorld(Object ship, Vec3 shipDir) {
      Matrix4dc m = ((Ship)ship).getTransform().getShipToWorld();
      Vector3d v = new Vector3d(shipDir.x, shipDir.y, shipDir.z);
      m.transformDirection(v);
      return new Vec3(v.x, v.y, v.z);
   }

   @Override
   public Vec3 transformWorldToShip(Object ship, Vec3 worldPos) {
      Matrix4dc m = ((Ship)ship).getTransform().getWorldToShip();
      Vector3d v = new Vector3d(worldPos.x, worldPos.y, worldPos.z);
      m.transformPosition(v);
      return new Vec3(v.x, v.y, v.z);
   }

   @Override
   public Vec3 transformDirWorldToShip(Object ship, Vec3 worldDir) {
      Matrix4dc m = ((Ship)ship).getTransform().getWorldToShip();
      Vector3d v = new Vector3d(worldDir.x, worldDir.y, worldDir.z);
      m.transformDirection(v);
      return new Vec3(v.x, v.y, v.z);
   }

   @Override
   public Vec3 shipCenter(Object ship) {
      AABBdc aabb = ((Ship)ship).getWorldAABB();
      return new Vec3((aabb.minX() + aabb.maxX()) * 0.5, (aabb.minY() + aabb.maxY()) * 0.5, (aabb.minZ() + aabb.maxZ()) * 0.5);
   }

   @Override
   public Vec3 shipVelocity(Object ship) {
      Vector3dc v = ((ServerShip)ship).getVelocity();
      return new Vec3(v.x(), v.y(), v.z());
   }

   @Override
   public Obb shipObb(Object ship) {
      Ship s = (Ship)ship;
      ShipTransform t = s.getTransform();
      AABBic shipAabb = s.getShipAABB();
      double cx = (double)(shipAabb.minX() + shipAabb.maxX()) * 0.5;
      double cy = (double)(shipAabb.minY() + shipAabb.maxY()) * 0.5;
      double cz = (double)(shipAabb.minZ() + shipAabb.maxZ()) * 0.5;
      double hx = (double)(shipAabb.maxX() - shipAabb.minX()) * 0.5;
      double hy = (double)(shipAabb.maxY() - shipAabb.minY()) * 0.5;
      double hz = (double)(shipAabb.maxZ() - shipAabb.minZ()) * 0.5;
      Vector3d centerShip = new Vector3d(cx, cy, cz);
      t.getShipToWorld().transformPosition(centerShip);
      Quaterniond rot = new Quaterniond();
      t.getShipToWorld().getNormalizedRotation(rot);
      return new Obb(new Vec3(centerShip.x, centerShip.y, centerShip.z), new Vec3(hx, hy, hz), rot);
   }

   @Override
   public Stream<IRadarTarget> shipTargetsIn(ServerLevel level) {
      QueryableShipData<LoadedServerShip> ships = VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips();
      return StreamSupport.<LoadedServerShip>stream(ships.spliterator(), false).map(ship -> new VsShipTargetView(level, ship, this));
   }
}
