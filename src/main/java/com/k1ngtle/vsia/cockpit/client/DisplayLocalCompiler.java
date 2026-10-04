package com.k1ngtle.vsia.cockpit.client;

import com.google.gson.*;
import com.k1ngtle.vsia.cockpit.program.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Client-initiated compilation only. Never invoked by incoming server/drive packets. */
public final class DisplayLocalCompiler {
    public static final String IMAGE="vsia-display-runner:2.7.4";
    private static final ExecutorService JOB=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"VSIA local display compiler");t.setDaemon(true);return t;});
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    private DisplayLocalCompiler(){}
    public record Result(DisplayDesign design,String error){public boolean ok(){return design!=null;}}
    public static CompletableFuture<Result> compile(String language,String source){
        DisplayCodeLanguage.valueOf(language);DisplayProgramLimits.validate(source);
        if(!BUSY.compareAndSet(false,true))return CompletableFuture.completedFuture(new Result(null,"Local compiler busy; wait for the previous job"));
        CompletableFuture<Result> result=new CompletableFuture<>();
        JOB.execute(()->{try{result.complete(new Result(run(language,source),null));}catch(Exception e){String message=e.getMessage();result.complete(new Result(null,message==null?"Local compilation failed":message.substring(0,Math.min(1800,message.length()))));}finally{BUSY.set(false);}});
        return result;
    }
    public static boolean busy(){return BUSY.get();}
    /** Fixed arguments only: no source text, server values, image override or host mount. */
    public static List<String> command(String name){
        if(!name.matches("vsia-display-client-[a-f0-9]{32}"))throw new IllegalArgumentException("Invalid local job name");
        // .NET 8 needs a sparse 2 TiB anonymous double-mapping file and
        // MSBuild needs more than 128 descriptors. Actual memory stays
        // capped at 768 MiB; the only writable filesystem stays 256 MiB.
        return List.of("docker","--context","default","run","--rm","--pull=never","--platform=linux/amd64","--name",name,
            "--network=none","--read-only","--user=10001:10001","--cap-drop=ALL","--security-opt=no-new-privileges=true",
            "--pids-limit=96","--cpus=1","--memory=768m","--memory-swap=768m","--ulimit=nofile=1024:1024","--ulimit=fsize=2199023255552:2199023255552",
            "--tmpfs=/tmp:rw,exec,nosuid,nodev,size=268435456,mode=1777","--log-driver=none","--env=VSIA_ISOLATED_JOB=1",
            "--env=HTTP_PROXY=","--env=HTTPS_PROXY=","--env=ALL_PROXY=","--env=NO_PROXY=","--env=http_proxy=","--env=https_proxy=","--env=all_proxy=","--env=no_proxy=","-i",IMAGE);
    }
    private static ProcessBuilder builder(List<String> argv){
        ProcessBuilder b=new ProcessBuilder(argv);
        // Never pass game/launcher/credential environment into the CLI or the job.
        Map<String,String> inherited=new HashMap<>(b.environment());b.environment().clear();
        for(String key:List.of("PATH","Path","SystemRoot","WINDIR","COMSPEC","PATHEXT","TEMP","TMP","USERPROFILE","HOME","APPDATA","LOCALAPPDATA","XDG_RUNTIME_DIR"))if(inherited.containsKey(key))b.environment().put(key,inherited.get(key));
        return b;
    }
    private static DisplayDesign run(String language,String source)throws Exception{
        JsonObject job=new JsonObject();job.addProperty("language",language);job.addProperty("source",source);
        byte[] input=job.toString().getBytes(StandardCharsets.UTF_8);if(input.length>1048576)throw new IOException("Encoded source exceeds local job limit");
        String name="vsia-display-client-"+UUID.randomUUID().toString().replace("-","");
        Process process;
        try{process=builder(command(name)).start();}catch(IOException e){throw new IOException("Docker is not available on this client. Start Docker Desktop (Linux containers) and run SETUP-CLIENT.ps1 from this package's display-service folder.");}
        try{
            String output=bounded(process,input,40000,24576,8192);
            return DisplayDesign.parse(output);
        }finally{
            if(process.isAlive())process.destroyForcibly();
            // Exact generated name only. Never prune containers or load host compilers.
            try{Process cleanup=builder(List.of("docker","--context","default","rm","-f",name)).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();if(!cleanup.waitFor(5,TimeUnit.SECONDS))cleanup.destroyForcibly();}catch(IOException e){}catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
    }
    /** Public for headless bounded-process tests; production calls this only with docker run. */
    public static String bounded(Process process,byte[] input,long timeoutMs,int stdoutLimit,int stderrLimit)throws Exception{
        ByteArrayOutputStream stdout=new ByteArrayOutputStream(),stderr=new ByteArrayOutputStream();AtomicBoolean overflow=new AtomicBoolean(),inputFailed=new AtomicBoolean();
        Thread out=reader(process.getInputStream(),stdout,stdoutLimit,process,overflow),err=reader(process.getErrorStream(),stderr,stderrLimit,process,overflow);
        Thread writer=new Thread(()->{try(OutputStream pipe=process.getOutputStream()){pipe.write(input);}catch(IOException e){inputFailed.set(true);}},"VSIA local compiler input");writer.setDaemon(true);writer.start();
        try{
            if(!process.waitFor(timeoutMs,TimeUnit.MILLISECONDS)){process.destroyForcibly();process.waitFor(3,TimeUnit.SECONDS);throw new IOException("Local compiler timeout; job stopped");}
            writer.join(1000);out.join(1000);err.join(1000);
            if(overflow.get())throw new IOException("Local compiler output limit exceeded");
            if(writer.isAlive()||out.isAlive()||err.isAlive())throw new IOException("Local compiler pipes did not complete");
            if(process.exitValue()!=0){String text=stderr.toString(StandardCharsets.UTF_8).replaceAll("[\\p{Cntrl}§]"," ");throw new IOException("Local compiler: "+text.substring(Math.max(0,text.length()-1400))+". Check Docker is running and the "+IMAGE+" image was built with SETUP-CLIENT.ps1.");}
            if(inputFailed.get())throw new IOException("Local compiler input failed");
            return stdout.toString(StandardCharsets.UTF_8);
        }finally{
            if(process.isAlive())process.destroyForcibly();
            try{process.getOutputStream().close();}catch(IOException e){}try{process.getInputStream().close();}catch(IOException e){}try{process.getErrorStream().close();}catch(IOException e){}
        }
    }
    private static Thread reader(InputStream pipe,ByteArrayOutputStream buffer,int limit,Process process,AtomicBoolean overflow){
        Thread t=new Thread(()->{try{byte[] part=new byte[4096];int size;while((size=pipe.read(part))!=-1){int room=limit-buffer.size();buffer.write(part,0,Math.min(size,Math.max(0,room)));if(size>room){overflow.set(true);process.destroyForcibly();break;}}}catch(IOException e){if(process.isAlive())process.destroyForcibly();}},"VSIA local compiler output");t.setDaemon(true);t.start();return t;
    }
    public static boolean sameEditor(UUID expectedSession,UUID expectedDrive,long expectedRevision,UUID session,UUID drive,long revision){return expectedSession!=null&&expectedSession.equals(session)&&expectedDrive!=null&&expectedDrive.equals(drive)&&expectedRevision==revision;}
}
