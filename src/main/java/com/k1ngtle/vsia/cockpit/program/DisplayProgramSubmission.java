package com.k1ngtle.vsia.cockpit.program;

/** Both designer and client-compiled scenes are UNTRUSTED input to the server. */
public record DisplayProgramSubmission(String name,DisplayCodeLanguage language,DisplayDesign design,String source) {
    public static final int DESIGNER=0, CLIENT_COMPILED=4;
    public static DisplayProgramSubmission validate(int action,String name,String language,String layout,String source){
        if(action!=DESIGNER&&action!=CLIENT_COMPILED)throw new IllegalArgumentException("Not a display-scene submission");
        if(name==null||name.length()>48||name.chars().anyMatch(c->c<32||c==167))throw new IllegalArgumentException("Invalid program name");
        DisplayProgramLimits.validate(source);
        return new DisplayProgramSubmission(name,DisplayCodeLanguage.valueOf(language),DisplayDesign.parse(layout),source);
    }
}
