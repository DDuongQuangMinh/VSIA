package com.k1ngtle.vsia.cockpit.program;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Properties;
import java.util.List;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import net.minecraftforge.fml.loading.FMLPaths;

/** Only an administrator-configured loopback endpoint. Never invokes a host compiler. */
public final class DisplayCompilerClient {
    private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).proxy(new java.net.ProxySelector(){
        public List<java.net.Proxy> select(URI uri){return List.of(java.net.Proxy.NO_PROXY);}
        public void connectFailed(URI uri,java.net.SocketAddress address,IOException error){}
    }).build();
    private DisplayCompilerClient(){}
    public static DisplayDesign compile(String language,String source) throws Exception {
        DisplayCodeLanguage.valueOf(language);
        DisplayProgramLimits.validate(source);
        Properties p=new Properties();Path config=FMLPaths.CONFIGDIR.get().resolve("vsia-display-service.properties");
        if(!Files.isRegularFile(config))throw new IOException("Compiler service is not configured. See DISPLAY-SERVICE.md");
        try(InputStream in=Files.newInputStream(config)){p.load(in);}
        if(!Boolean.parseBoolean(p.getProperty("enabled","false")))throw new IOException("Compiler service disabled by server administrator");
        String token=p.getProperty("token","");if(!token.matches("[a-f0-9]{64}"))throw new IOException("Invalid service token");
        int port=Integer.parseInt(p.getProperty("port","38735"));if(port<1024||port>65535)throw new IOException("Invalid service port");
        JsonObject body=new JsonObject();body.addProperty("language",language);body.addProperty("source",source);
        HttpRequest request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/compile")).timeout(Duration.ofSeconds(50)).header("Authorization","Bearer "+token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        CompletableFuture<HttpResponse<byte[]>> future=CLIENT.sendAsync(request,info->new LimitedBody());
        try {
            HttpResponse<byte[]> reply=future.get(50,TimeUnit.SECONDS);byte[] bytes=reply.body();
            String text=new String(bytes,StandardCharsets.UTF_8);
            if(reply.statusCode()!=200){String safe=text.replaceAll("[\\p{Cntrl}§]"," ");throw new IOException("Compiler: "+safe.substring(0,Math.min(1800,safe.length())));}
            return DisplayDesign.parse(text);
        }finally{future.cancel(true);}
    }
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        final CompletableFuture<byte[]> result=new CompletableFuture<>();final ByteArrayOutputStream bytes=new ByteArrayOutputStream();Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody(){return result;}
        public void onSubscribe(Flow.Subscription s){subscription=s;s.request(1);}
        public void onNext(List<ByteBuffer> buffers){for(ByteBuffer b:buffers){if(bytes.size()+b.remaining()>65536){subscription.cancel();result.completeExceptionally(new IOException("Oversized service response"));return;}byte[] part=new byte[b.remaining()];b.get(part);bytes.writeBytes(part);}subscription.request(1);}
        public void onError(Throwable t){result.completeExceptionally(t);}
        public void onComplete(){result.complete(bytes.toByteArray());}
    }
}
