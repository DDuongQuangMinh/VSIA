package com.k1ngtle.vsia.signality.api.events;

import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public abstract class RadarScanEvent extends Event {
   private final IRadarEmitter emitter;

   protected RadarScanEvent(IRadarEmitter emitter) {
      this.emitter = emitter;
   }

   public IRadarEmitter emitter() {
      return this.emitter;
   }

   public static final class Post extends RadarScanEvent {
      private final List<RadarContact> contacts;

      public Post(IRadarEmitter emitter, List<RadarContact> contacts) {
         super(emitter);
         this.contacts = Collections.unmodifiableList(contacts);
      }

      public List<RadarContact> contacts() {
         return this.contacts;
      }
   }

   @Cancelable
   public static final class Pre extends RadarScanEvent {
      public Pre(IRadarEmitter emitter) {
         super(emitter);
      }
   }
}
