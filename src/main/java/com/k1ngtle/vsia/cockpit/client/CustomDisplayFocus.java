package com.k1ngtle.vsia.cockpit.client;

import java.util.*;
/** Local pilot focus only; keyed by cockpit and installed drive/revision, never global configuration. */
public final class CustomDisplayFocus {
    private record Focus(UUID drive,long revision,int section){}
    private static final Map<UUID,Focus> FOCUS=new HashMap<>();
    private CustomDisplayFocus(){}
    public static void select(UUID cockpit,UUID drive,long revision,int section){if(cockpit!=null&&section>=1&&section<=6)FOCUS.put(cockpit,new Focus(drive,revision,section));}
    public static int selected(UUID cockpit,UUID drive,long revision){Focus f=FOCUS.get(cockpit);return f!=null&&Objects.equals(f.drive,drive)&&f.revision==revision?f.section:0;}
    public static void clearAll(){FOCUS.clear();}
}
