package com.k1ngtle.vsia.cockpit.program;

import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.nbt.*;

/** Source limits are independent of the smaller, validated display scene. */
public final class DisplayProgramLimits {
    public static final int SOURCE_CHARS=131072, SOURCE_BYTES=393216, UPLOAD_CHARS=6000, NBT_CHARS=16384;
    private static final String LEGACY="VsiaDisplaySource", CHUNKS="VsiaDisplaySourceChunks";
    private DisplayProgramLimits(){}
    public static void validate(String source){if(source==null||source.length()>SOURCE_CHARS||source.getBytes(StandardCharsets.UTF_8).length>SOURCE_BYTES)throw new IllegalArgumentException("Code limit: 131072 characters / 393216 UTF-8 bytes");}
    /** Network UTF-8 must never split a supplementary Unicode character between packets. */
    public static List<String> uploadParts(String source){validate(source);if(source.isEmpty())return List.of("");List<String> parts=new ArrayList<>();for(int start=0;start<source.length();){int end=Math.min(source.length(),start+UPLOAD_CHARS);if(end<source.length()&&Character.isHighSurrogate(source.charAt(end-1))&&Character.isLowSurrogate(source.charAt(end)))end--;parts.add(source.substring(start,end));start=end;}return List.copyOf(parts);}
    public static void store(CompoundTag tag,String source){validate(source);ListTag parts=new ListTag();for(int i=0;i<source.length();i+=NBT_CHARS)parts.add(StringTag.valueOf(source.substring(i,Math.min(source.length(),i+NBT_CHARS))));tag.put(CHUNKS,parts);tag.remove(LEGACY);}
    public static String read(CompoundTag tag){if(tag==null)return "";if(!tag.contains(CHUNKS,Tag.TAG_LIST))return tag.getString(LEGACY);ListTag parts=tag.getList(CHUNKS,Tag.TAG_STRING);if(parts.size()>(SOURCE_CHARS+NBT_CHARS-1)/NBT_CHARS)return "";StringBuilder s=new StringBuilder();for(int i=0;i<parts.size();i++){String part=parts.getString(i);if(part.length()>NBT_CHARS||s.length()+part.length()>SOURCE_CHARS)return "";s.append(part);}return s.toString();}
    public static void clear(CompoundTag tag){tag.remove(CHUNKS);tag.remove(LEGACY);}
}
