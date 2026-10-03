package com.k1ngtle.vsia.cockpit.program;

import java.util.UUID;

/** One bounded, ordered, short-lived upload per authenticated laptop session. */
public final class DisplaySourceUpload {
    private UUID id;private int count,next;private long started;private final StringBuilder source=new StringBuilder();
    public void append(UUID upload,int index,int total,String part,long now){
        int maximum=(DisplayProgramLimits.SOURCE_CHARS+DisplayProgramLimits.UPLOAD_CHARS-1)/DisplayProgramLimits.UPLOAD_CHARS;
        if(upload==null||total<1||total>maximum||index<0||index>=total||part==null||part.length()>DisplayProgramLimits.UPLOAD_CHARS)throw new IllegalArgumentException("Invalid source chunk");
        if(index==0){id=upload;count=total;next=0;started=now;source.setLength(0);}
        if(!upload.equals(id)||count!=total||index!=next||now-started>30000||source.length()+part.length()>DisplayProgramLimits.SOURCE_CHARS){clear();throw new IllegalArgumentException("Stale or out-of-order source upload");}
        source.append(part);next++;
    }
    public String take(UUID upload,long now){if(upload==null||!upload.equals(id)||next!=count||now-started>30000){clear();throw new IllegalArgumentException("Incomplete or expired source upload");}String result=source.toString();clear();DisplayProgramLimits.validate(result);return result;}
    public void clear(){id=null;count=next=0;source.setLength(0);}
}
