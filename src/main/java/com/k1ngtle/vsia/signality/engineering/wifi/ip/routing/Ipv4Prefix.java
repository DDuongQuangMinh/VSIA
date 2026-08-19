package com.k1ngtle.vsia.signality.engineering.wifi.ip.routing;
public final class Ipv4Prefix {
 private Ipv4Prefix(){}
 public static int parse(String a){
  if(a==null) throw new IllegalArgumentException("IPv4 address is null");
  String[] p=a.trim().split("\\."); if(p.length!=4) throw new IllegalArgumentException("Invalid IPv4 address: "+a);
  int v=0; for(String s:p){int o;try{o=Integer.parseInt(s);}catch(Exception e){throw new IllegalArgumentException("Invalid IPv4 address: "+a);}
   if(o<0||o>255) throw new IllegalArgumentException("Invalid IPv4 address: "+a); v=(v<<8)|o;} return v;
 }
 public static String format(int v){return ((v>>>24)&255)+"."+((v>>>16)&255)+"."+((v>>>8)&255)+"."+(v&255);}
 public static int maskFromPrefixLength(int p){if(p<0||p>32)throw new IllegalArgumentException("prefix"); return p==0?0:(int)(0xffffffffL<<(32-p));}
 public static int prefixLengthFromMask(String m){
  int v=parse(m),p=0; boolean zero=false; for(int b=31;b>=0;b--){boolean one=((v>>>b)&1)!=0;if(one){if(zero)throw new IllegalArgumentException("Non-contiguous mask: "+m);p++;}else zero=true;} return p;
 }
 public static String network(String a,int p){return format(parse(a)&maskFromPrefixLength(p));}
 public static boolean matches(String a,String n,int p){int m=maskFromPrefixLength(p);return (parse(a)&m)==(parse(n)&m);}
 public static boolean isUsableUnicast(String a){int v;try{v=parse(a);}catch(Exception e){return false;}long u=Integer.toUnsignedLong(v);if(u==0||u==0xffffffffL)return false;int first=(v>>>24)&255;return first<224&&first!=127;}
}
