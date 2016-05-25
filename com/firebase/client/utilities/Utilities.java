/*     */ package com.firebase.client.utilities;
/*     */ 
/*     */ import com.firebase.client.FirebaseException;
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.core.RepoInfo;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.net.URI;
/*     */ import java.net.URISyntaxException;
/*     */ import java.nio.ByteBuffer;
/*     */ import java.security.MessageDigest;
/*     */ import java.security.NoSuchAlgorithmException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Map;
/*     */ 
/*     */ public class Utilities
/*     */ {
/*     */   public static ParsedUrl parseUrl(String url) throws FirebaseException
/*     */   {
/*  19 */     String original = url;
/*     */     try {
/*  21 */       int schemeOffset = original.indexOf("//");
/*  22 */       if (schemeOffset == -1) {
/*  23 */         throw new URISyntaxException(original, "Invalid scheme specified");
/*     */       }
/*  25 */       int pathOffset = original.substring(schemeOffset + 2).indexOf("/");
/*  26 */       if (pathOffset != -1) {
/*  27 */         pathOffset += schemeOffset + 2;
/*  28 */         String[] pathSegments = original.substring(pathOffset).split("/");
/*  29 */         StringBuilder builder = new StringBuilder();
/*  30 */         for (int i = 0; i < pathSegments.length; i++) {
/*  31 */           if (!pathSegments[i].equals("")) {
/*  32 */             builder.append("/");
/*  33 */             builder.append(java.net.URLEncoder.encode(pathSegments[i], "UTF-8"));
/*     */           }
/*     */         }
/*  36 */         original = original.substring(0, pathOffset) + builder.toString();
/*     */       }
/*     */       
/*  39 */       URI uri = new URI(original);
/*     */       
/*     */ 
/*  42 */       String pathString = uri.getPath().replace("+", " ");
/*  43 */       Validation.validateRootPathString(pathString);
/*  44 */       Path path = new Path(pathString);
/*  45 */       String scheme = uri.getScheme();
/*     */       
/*  47 */       RepoInfo repoInfo = new RepoInfo();
/*  48 */       repoInfo.host = uri.getHost().toLowerCase();
/*     */       
/*  50 */       int port = uri.getPort();
/*  51 */       if (port != -1) {
/*  52 */         repoInfo.secure = scheme.equals("https"); RepoInfo 
/*  53 */           tmp255_253 = repoInfo;tmp255_253.host = (tmp255_253.host + ":" + port);
/*     */       } else {
/*  55 */         repoInfo.secure = true;
/*     */       }
/*  57 */       String[] parts = repoInfo.host.split("\\.");
/*     */       
/*  59 */       repoInfo.namespace = parts[0].toLowerCase();
/*  60 */       repoInfo.internalHost = repoInfo.host;
/*  61 */       ParsedUrl parsedUrl = new ParsedUrl();
/*  62 */       parsedUrl.path = path;
/*  63 */       parsedUrl.repoInfo = repoInfo;
/*  64 */       return parsedUrl;
/*     */     }
/*     */     catch (URISyntaxException e) {
/*  67 */       throw new FirebaseException("Invalid Firebase url specified", e);
/*     */     } catch (UnsupportedEncodingException e) {
/*  69 */       throw new FirebaseException("Failed to URLEncode the path", e);
/*     */     }
/*     */   }
/*     */   
/*     */   public static String[] splitIntoFrames(String src, int maxFrameSize) {
/*  74 */     if (src.length() <= maxFrameSize) {
/*  75 */       return new String[] { src };
/*     */     }
/*  77 */     ArrayList<String> segs = new ArrayList();
/*  78 */     for (int i = 0; i < src.length(); i += maxFrameSize) {
/*  79 */       int end = Math.min(i + maxFrameSize, src.length());
/*  80 */       String seg = src.substring(i, end);
/*  81 */       segs.add(seg);
/*     */     }
/*  83 */     return (String[])segs.toArray(new String[segs.size()]);
/*     */   }
/*     */   
/*     */   public static String sha1HexDigest(String input)
/*     */   {
/*     */     try {
/*  89 */       MessageDigest md = MessageDigest.getInstance("SHA-1");
/*  90 */       md.update(input.getBytes("UTF-8"));
/*  91 */       byte[] bytes = md.digest();
/*  92 */       return Base64.encodeBytes(bytes);
/*     */     } catch (NoSuchAlgorithmException e) {
/*  94 */       throw new RuntimeException("Missing SHA-1 MessageDigest provider.", e);
/*     */     } catch (UnsupportedEncodingException e) {
/*  96 */       throw new RuntimeException("UTF-8 encoding is required for Firebase to run!");
/*     */     }
/*     */   }
/*     */   
/*     */   public static String stringHashV2Representation(String value) {
/* 101 */     String escaped = value;
/* 102 */     if (value.indexOf('\\') != -1) {
/* 103 */       escaped = escaped.replace("\\", "\\\\");
/*     */     }
/* 105 */     if (value.indexOf('"') != -1) {
/* 106 */       escaped = escaped.replace("\"", "\\\"");
/*     */     }
/* 108 */     return '"' + escaped + '"';
/*     */   }
/*     */   
/*     */   public static String doubleToHashString(double value) {
/* 112 */     StringBuilder sb = new StringBuilder(16);
/* 113 */     byte[] bytes = new byte[8];
/* 114 */     ByteBuffer.wrap(bytes).putDouble(value);
/* 115 */     for (int i = 0; i < 8; i++) {
/* 116 */       sb.append(String.format("%02x", new Object[] { Byte.valueOf(bytes[i]) }));
/*     */     }
/* 118 */     return sb.toString();
/*     */   }
/*     */   
/*     */ 
/*     */   public static Integer tryParseInt(String num)
/*     */   {
/* 124 */     if ((num.length() > 11) || (num.length() == 0)) {
/* 125 */       return null;
/*     */     }
/* 127 */     int i = 0;
/* 128 */     boolean negative = false;
/* 129 */     if (num.charAt(0) == '-') {
/* 130 */       if (num.length() == 1) {
/* 131 */         return null;
/*     */       }
/* 133 */       negative = true;
/* 134 */       i = 1;
/*     */     }
/*     */     
/* 137 */     long number = 0L;
/* 138 */     while (i < num.length()) {
/* 139 */       char c = num.charAt(i);
/* 140 */       if ((c < '0') || (c > '9')) {
/* 141 */         return null;
/*     */       }
/* 143 */       number = number * 10L + (c - '0');
/* 144 */       i++;
/*     */     }
/* 146 */     if (negative) {
/* 147 */       if (-number < -2147483648L) {
/* 148 */         return null;
/*     */       }
/* 150 */       return Integer.valueOf((int)-number);
/*     */     }
/*     */     
/* 153 */     if (number > 2147483647L) {
/* 154 */       return null;
/*     */     }
/* 156 */     return Integer.valueOf((int)number);
/*     */   }
/*     */   
/*     */   public static int compareInts(int i, int j)
/*     */   {
/* 161 */     if (i < j)
/* 162 */       return -1;
/* 163 */     if (i == j) {
/* 164 */       return 0;
/*     */     }
/* 166 */     return 1;
/*     */   }
/*     */   
/*     */   public static int compareLongs(long i, long j)
/*     */   {
/* 171 */     if (i < j)
/* 172 */       return -1;
/* 173 */     if (i == j) {
/* 174 */       return 0;
/*     */     }
/* 176 */     return 1;
/*     */   }
/*     */   
/*     */   public static <C> C castOrNull(Object o, Class<C> clazz)
/*     */   {
/* 181 */     if (clazz.isAssignableFrom(o.getClass())) {
/* 182 */       return (C)o;
/*     */     }
/* 184 */     return null;
/*     */   }
/*     */   
/*     */   public static <C> C getOrNull(Object o, String key, Class<C> clazz)
/*     */   {
/* 189 */     if (o == null) {
/* 190 */       return null;
/*     */     }
/* 192 */     Map map = (Map)castOrNull(o, Map.class);
/* 193 */     Object result = map.get(key);
/* 194 */     if (result != null) {
/* 195 */       return (C)castOrNull(result, clazz);
/*     */     }
/* 197 */     return null;
/*     */   }
/*     */   
/*     */   public static Long longFromObject(Object o)
/*     */   {
/* 202 */     if ((o instanceof Integer))
/* 203 */       return Long.valueOf(((Integer)o).intValue());
/* 204 */     if ((o instanceof Long)) {
/* 205 */       return (Long)o;
/*     */     }
/* 207 */     return null;
/*     */   }
/*     */   
/*     */   public static void hardAssert(boolean condition)
/*     */   {
/* 212 */     hardAssert(condition, "");
/*     */   }
/*     */   
/*     */   public static void hardAssert(boolean condition, String message) {
/* 216 */     if (!condition) {
/* 217 */       throw new AssertionError("hardAssert failed: " + message);
/*     */     }
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/utilities/Utilities.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */