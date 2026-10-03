package com.k1ngtle.vsia.cockpit.program;

import com.google.gson.*;
import java.util.*;

/** A bounded, declarative scene. Never evaluates expressions, loads classes or opens resources. */
public record DisplayDesign(String theme, List<Widget> widgets) {
    public static final int WIDTH = 860, HEIGHT = 343, MAX_TEXT = 24576, MAX_WIDGETS = 48;
    public static final List<String> TYPES = List.of("LABEL", "PANEL", "RADAR360", "RADARFORWARD", "CONTACTS", "LOCK", "DAMAGE", "HORIZON", "COMPASS", "VALUE", "BAR", "GAUGE", "STORES", "CLOCK");
    public static final List<String> THEMES = List.of("CYAN", "GREEN", "AMBER", "WHITE");
    public static final List<String> BINDINGS = List.of("SPEED", "ALTITUDE", "HEADING", "PITCH", "ROLL", "VSPEED", "FUEL", "FUEL_KG", "CONTACT_COUNT", "STRUCTURE", "LOCK_RANGE");
    public static final List<String> PRESETS = List.of("Tactical", "Wide radar", "Flight", "Landing", "Damage", "Stores", "IFF contacts", "Minimal");
    public record Widget(String type, int x, int y, int w, int h, int variant, String text, String binding, int section) {
        public Widget(String type,int x,int y,int w,int h,int variant,String text,String binding){this(type,x,y,w,h,variant,text,binding,0);}
        public Widget {
            if (!TYPES.contains(type) || x < 0 || y < 0 || w < 24 || h < 24 || x + w > WIDTH || y + h > HEIGHT || variant < 0 || variant > 4 || section<0 || section>6)
                throw new IllegalArgumentException("Invalid widget type, size, position or variant");
            text = text == null ? "" : text;
            binding = binding == null ? "SPEED" : binding;
            if (text.length() > 48 || text.chars().anyMatch(c -> c < 32 || c == 167) || !BINDINGS.contains(binding))
                throw new IllegalArgumentException("Invalid label or telemetry binding");
        }
        public Widget move(int nx, int ny) { return new Widget(type, Math.max(0, Math.min(WIDTH-w, nx)), Math.max(0, Math.min(HEIGHT-h, ny)), w, h, variant, text, binding,section); }
        public Widget resize(int nw, int nh) { return new Widget(type, x, y, Math.max(24, Math.min(WIDTH-x, nw)), Math.max(24, Math.min(HEIGHT-y, nh)), variant, text, binding,section); }
        public Widget style(int v, String label, String data) { return new Widget(type,x,y,w,h,v,label,data,section); }
        public Widget inSection(int value){return new Widget(type,x,y,w,h,variant,text,binding,value);}
    }
    public DisplayDesign {
        if (!THEMES.contains(theme) || widgets == null || widgets.size() > MAX_WIDGETS) throw new IllegalArgumentException("Invalid theme or widget count");
        widgets = List.copyOf(widgets);
    }
    private static String string(JsonObject o, String k, String fallback) {
        if (!o.has(k)) return fallback;
        JsonElement v=o.get(k); if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) throw new IllegalArgumentException(k+" must be a string");
        return v.getAsString();
    }
    private static int integer(JsonObject o, String k, int fallback) {
        if (!o.has(k)) return fallback;
        JsonElement v=o.get(k); if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber() || !v.toString().matches("-?[0-9]{1,6}")) throw new IllegalArgumentException(k+" must be an integer");
        return v.getAsInt();
    }
    private static void fields(JsonObject o, Set<String> allowed) { if (!allowed.containsAll(o.keySet())) throw new IllegalArgumentException("Unknown scene field"); }
    public static DisplayDesign parse(String json) {
        if (json == null || json.length() > MAX_TEXT) throw new IllegalArgumentException("Design is too large");
        // Limit nesting before Gson parsing so adversarial recursion cannot exhaust a server thread.
        int depth=0; boolean quoted=false, escaped=false;
        for (char c:json.toCharArray()) { if (quoted) { if(escaped) escaped=false; else if(c=='\\') escaped=true; else if(c=='\"') quoted=false; } else if(c=='\"') quoted=true; else if(c=='{'||c=='[') { if(++depth>6) throw new IllegalArgumentException("Design nesting too deep"); } else if(c=='}'||c==']') { if(--depth<0) throw new IllegalArgumentException("Invalid JSON"); } }
        if(depth!=0||quoted) throw new IllegalArgumentException("Incomplete JSON");
        try {
            JsonObject root=JsonParser.parseString(json).getAsJsonObject(); fields(root,Set.of("version","theme","widgets"));
            if(integer(root,"version",0)!=1) throw new IllegalArgumentException("Scene version must be 1");
            JsonArray array=root.getAsJsonArray("widgets"); if(array==null||array.size()>MAX_WIDGETS) throw new IllegalArgumentException("At most 48 widgets");
            List<Widget> list=new ArrayList<>();
            for(JsonElement e:array) { JsonObject w=e.getAsJsonObject(); fields(w,Set.of("type","x","y","w","h","variant","text","binding","section")); list.add(new Widget(string(w,"type",""),integer(w,"x",0),integer(w,"y",0),integer(w,"w",180),integer(w,"h",120),integer(w,"variant",0),string(w,"text",""),string(w,"binding","SPEED"),integer(w,"section",0))); }
            return new DisplayDesign(string(root,"theme","CYAN"),list);
        } catch (JsonParseException | IllegalStateException | ClassCastException e) { throw new IllegalArgumentException("Invalid design JSON"); }
    }
    public String json() {
        JsonObject root=new JsonObject(); root.addProperty("version",1);root.addProperty("theme",theme);JsonArray array=new JsonArray();
        for(Widget w:widgets){JsonObject o=new JsonObject();o.addProperty("type",w.type);o.addProperty("x",w.x);o.addProperty("y",w.y);o.addProperty("w",w.w);o.addProperty("h",w.h);o.addProperty("variant",w.variant);o.addProperty("text",w.text);o.addProperty("binding",w.binding);o.addProperty("section",w.section);array.add(o);} root.add("widgets",array);return root.toString();
    }
    private static Widget w(String type,int x,int y,int width,int height,int style,String binding){return new Widget(type,x,y,width,height,style,"",binding);}
    public static DisplayDesign preset(int index) {
        List<Widget> list=switch(Math.floorMod(index,8)) {
            case 1 -> List.of(w("RADAR360",0,0,600,343,1,"SPEED"),w("LOCK",610,0,250,150,0,"SPEED"),w("CONTACTS",610,160,250,183,2,"SPEED"));
            case 2 -> List.of(w("HORIZON",215,0,430,343,1,"SPEED"),w("GAUGE",0,0,205,160,0,"SPEED"),w("VALUE",0,170,205,80,1,"VSPEED"),w("COMPASS",655,0,205,180,2,"HEADING"),w("VALUE",655,190,205,100,0,"ALTITUDE"));
            case 3 -> List.of(w("HORIZON",0,0,510,343,3,"SPEED"),w("VALUE",520,0,340,100,2,"ALTITUDE"),w("BAR",520,110,340,100,1,"VSPEED"),w("VALUE",520,220,340,100,0,"SPEED"));
            case 4 -> List.of(w("DAMAGE",0,0,570,343,1,"STRUCTURE"),w("BAR",580,0,280,140,2,"STRUCTURE"),w("STORES",580,150,280,193,0,"FUEL"));
            case 5 -> List.of(w("STORES",0,0,480,343,2,"FUEL"),w("GAUGE",490,0,180,180,0,"FUEL"),w("DAMAGE",680,0,180,343,0,"STRUCTURE"),w("VALUE",490,190,180,90,1,"FUEL_KG"));
            case 6 -> List.of(w("CONTACTS",0,0,530,343,1,"CONTACT_COUNT"),w("LOCK",540,0,320,170,3,"LOCK_RANGE"),w("RADARFORWARD",540,180,320,163,2,"SPEED"));
            case 7 -> List.of(w("RADAR360",0,0,430,343,4,"SPEED"),w("VALUE",440,0,200,100,4,"SPEED"),w("VALUE",650,0,210,100,4,"ALTITUDE"),w("LOCK",440,110,420,180,4,"SPEED"));
            default -> List.of(w("DAMAGE",0,0,200,343,0,"STRUCTURE"),w("RADARFORWARD",210,0,325,343,0,"SPEED"),w("RADAR360",545,0,315,215,1,"SPEED"),w("LOCK",545,225,315,118,0,"SPEED"));
        };return new DisplayDesign("CYAN",list);
    }
}
