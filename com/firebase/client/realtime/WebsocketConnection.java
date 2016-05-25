/*     */ package com.firebase.client.realtime;
/*     */ 
/*     */ import com.firebase.client.RunLoop;
/*     */ import com.firebase.client.core.Context;
/*     */ import com.firebase.client.core.RepoInfo;
/*     */ import com.firebase.client.realtime.util.StringListReader;
/*     */ import com.firebase.client.utilities.LogWrapper;
/*     */ import com.firebase.client.utilities.Utilities;
/*     */ import com.firebase.client.utilities.encoding.JsonHelpers;
/*     */ import com.firebase.tubesock.WebSocket;
/*     */ import com.firebase.tubesock.WebSocketEventHandler;
/*     */ import com.firebase.tubesock.WebSocketException;
/*     */ import com.firebase.tubesock.WebSocketMessage;
/*     */ import com.shaded.fasterxml.jackson.databind.ObjectMapper;
/*     */ import com.shaded.fasterxml.jackson.databind.type.MapType;
/*     */ import com.shaded.fasterxml.jackson.databind.type.TypeFactory;
/*     */ import java.io.IOException;
/*     */ import java.net.URI;
/*     */ import java.util.HashMap;
/*     */ import java.util.Map;
/*     */ import java.util.concurrent.ScheduledFuture;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class WebsocketConnection
/*     */ {
/*  29 */   private static long connectionId = 0L;
/*     */   
/*     */ 
/*     */   private static final long KEEP_ALIVE = 45000L;
/*     */   
/*     */ 
/*     */   private static final long CONNECT_TIMEOUT = 30000L;
/*     */   
/*     */   private static final int MAX_FRAME_SIZE = 16384;
/*     */   
/*     */   private WSClient conn;
/*     */   
/*     */ 
/*     */   private class WSClientTubesock
/*     */     implements WebsocketConnection.WSClient, WebSocketEventHandler
/*     */   {
/*     */     private WebSocket ws;
/*     */     
/*     */ 
/*     */     private WSClientTubesock(WebSocket ws)
/*     */     {
/*  50 */       this.ws = ws;
/*  51 */       this.ws.setEventHandler(this);
/*     */     }
/*     */     
/*     */     public void onOpen()
/*     */     {
/*  56 */       WebsocketConnection.this.ctx.getRunLoop().scheduleNow(new Runnable() {
/*     */         public void run() {
/*  58 */           WebsocketConnection.this.connectTimeout.cancel(false);
/*  59 */           WebsocketConnection.this.everConnected = true;
/*  60 */           if (WebsocketConnection.this.logger.logsDebug()) WebsocketConnection.this.logger.debug("websocket opened");
/*  61 */           WebsocketConnection.this.resetKeepAlive();
/*     */         }
/*     */       });
/*     */     }
/*     */     
/*     */     public void onMessage(WebSocketMessage msg)
/*     */     {
/*  68 */       final String str = msg.getText();
/*  69 */       if (WebsocketConnection.this.logger.logsDebug()) WebsocketConnection.this.logger.debug("ws message: " + str);
/*  70 */       WebsocketConnection.this.ctx.getRunLoop().scheduleNow(new Runnable() {
/*     */         public void run() {
/*  72 */           WebsocketConnection.this.handleIncomingFrame(str);
/*     */         }
/*     */       });
/*     */     }
/*     */     
/*     */     public void onClose()
/*     */     {
/*  79 */       String logMessage = "closed";
/*  80 */       WebsocketConnection.this.ctx.getRunLoop().scheduleNow(new Runnable() {
/*     */         public void run() {
/*  82 */           if (WebsocketConnection.this.logger.logsDebug()) WebsocketConnection.this.logger.debug("closed");
/*  83 */           WebsocketConnection.this.onClosed();
/*     */         }
/*     */       });
/*     */     }
/*     */     
/*     */     public void onError(final WebSocketException e)
/*     */     {
/*  90 */       WebsocketConnection.this.ctx.getRunLoop().scheduleNow(new Runnable() {
/*     */         public void run() {
/*  92 */           String logMessage = "had an error";
/*  93 */           if (WebsocketConnection.this.logger.logsDebug()) WebsocketConnection.this.logger.debug(logMessage, e);
/*  94 */           if (e.getMessage().startsWith("unknown host")) {
/*  95 */             if (WebsocketConnection.this.logger.logsDebug()) WebsocketConnection.this.logger.debug("If you are running on Android, have you added <uses-permission android:name=\"android.permission.INTERNET\" /> under <manifest> in AndroidManifest.xml?");
/*     */           }
/*  97 */           else if (WebsocketConnection.this.logger.logsDebug()) { WebsocketConnection.this.logger.debug("|" + e.getMessage() + "|");
/*     */           }
/*  99 */           WebsocketConnection.this.onClosed();
/*     */         }
/*     */       });
/*     */     }
/*     */     
/*     */     public void onLogMessage(String msg)
/*     */     {
/* 106 */       if (WebsocketConnection.this.logger.logsDebug()) WebsocketConnection.this.logger.debug("Tubesock: " + msg);
/*     */     }
/*     */     
/*     */     public void send(String msg) {
/* 110 */       this.ws.send(msg);
/*     */     }
/*     */     
/*     */     public void close() {
/* 114 */       this.ws.close();
/*     */     }
/*     */     
/*     */     private void shutdown() {
/* 118 */       this.ws.close();
/*     */       try {
/* 120 */         this.ws.blockClose();
/*     */       } catch (InterruptedException e) {
/* 122 */         WebsocketConnection.this.logger.error("Interrupted while shutting down websocket threads", e);
/*     */       }
/*     */     }
/*     */     
/*     */     public void connect() {
/*     */       try {
/* 128 */         this.ws.connect();
/*     */       } catch (WebSocketException e) {
/* 130 */         if (WebsocketConnection.this.logger.logsDebug()) WebsocketConnection.this.logger.debug("Error connecting", e);
/* 131 */         shutdown();
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/* 137 */   private boolean everConnected = false;
/* 138 */   private boolean isClosed = false;
/* 139 */   private long totalFrames = 0L;
/*     */   private StringListReader frameReader;
/*     */   private Delegate delegate;
/*     */   private ScheduledFuture keepAlive;
/*     */   private ObjectMapper mapper;
/*     */   private ScheduledFuture connectTimeout;
/*     */   private Context ctx;
/*     */   private LogWrapper logger;
/*     */   private MapType mapType;
/*     */   
/*     */   public WebsocketConnection(Context ctx, RepoInfo repoInfo, Delegate delegate, String optLastSessionId) {
/* 150 */     long connId = connectionId++;
/* 151 */     this.mapper = JsonHelpers.getMapper();
/* 152 */     this.mapType = this.mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);
/* 153 */     this.delegate = delegate;
/* 154 */     this.ctx = ctx;
/* 155 */     this.logger = ctx.getLogger("WebSocket", "ws_" + connId);
/* 156 */     this.conn = createConnection(repoInfo, optLastSessionId);
/*     */   }
/*     */   
/*     */   private WSClient createConnection(RepoInfo repoInfo, String optLastSessionId) {
/* 160 */     URI uri = repoInfo.getConnectionURL(optLastSessionId);
/* 161 */     Map<String, String> extraHeaders = new HashMap();
/* 162 */     extraHeaders.put("User-Agent", this.ctx.getUserAgent());
/* 163 */     WebSocket ws = new WebSocket(uri, null, extraHeaders);
/* 164 */     WSClientTubesock client = new WSClientTubesock(ws, null);
/* 165 */     return client;
/*     */   }
/*     */   
/*     */   public void open() {
/* 169 */     this.conn.connect();
/* 170 */     this.connectTimeout = this.ctx.getRunLoop().schedule(new Runnable()
/*     */     {
/* 172 */       public void run() { WebsocketConnection.this.closeIfNeverConnected(); } }, 30000L);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void start() {}
/*     */   
/*     */ 
/*     */   public void close()
/*     */   {
/* 182 */     if (this.logger.logsDebug()) this.logger.debug("websocket is being closed");
/* 183 */     this.isClosed = true;
/*     */     
/*     */ 
/* 186 */     this.conn.close();
/* 187 */     if (this.connectTimeout != null) {
/* 188 */       this.connectTimeout.cancel(true);
/*     */     }
/* 190 */     if (this.keepAlive != null) {
/* 191 */       this.keepAlive.cancel(true);
/*     */     }
/*     */   }
/*     */   
/*     */   public void send(Map<String, Object> message) {
/* 196 */     resetKeepAlive();
/*     */     try
/*     */     {
/* 199 */       String toSend = this.mapper.writeValueAsString(message);
/* 200 */       String[] segs = Utilities.splitIntoFrames(toSend, 16384);
/* 201 */       if (segs.length > 1) {
/* 202 */         this.conn.send("" + segs.length);
/*     */       }
/*     */       
/* 205 */       for (int i = 0; i < segs.length; i++) {
/* 206 */         this.conn.send(segs[i]);
/*     */       }
/*     */     } catch (IOException e) {
/* 209 */       this.logger.error("Failed to serialize message: " + message.toString(), e);
/* 210 */       shutdown();
/*     */     }
/*     */   }
/*     */   
/*     */   private void appendFrame(String message) {
/* 215 */     this.frameReader.addString(message);
/* 216 */     this.totalFrames -= 1L;
/* 217 */     if (this.totalFrames == 0L) {
/*     */       try
/*     */       {
/* 220 */         this.frameReader.freeze();
/* 221 */         Map<String, Object> decoded = (Map)this.mapper.readValue(this.frameReader, this.mapType);
/* 222 */         this.frameReader = null;
/* 223 */         if (this.logger.logsDebug()) this.logger.debug("handleIncomingFrame complete frame: " + decoded);
/* 224 */         this.delegate.onMessage(decoded);
/*     */       } catch (IOException e) {
/* 226 */         this.logger.error("Error parsing frame: " + this.frameReader.toString(), e);
/* 227 */         close();
/* 228 */         shutdown();
/*     */       } catch (ClassCastException e) {
/* 230 */         this.logger.error("Error parsing frame (cast error): " + this.frameReader.toString(), e);
/* 231 */         close();
/* 232 */         shutdown();
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void handleNewFrameCount(int numFrames) {
/* 238 */     this.totalFrames = numFrames;
/* 239 */     this.frameReader = new StringListReader();
/* 240 */     if (this.logger.logsDebug()) { this.logger.debug("HandleNewFrameCount: " + this.totalFrames);
/*     */     }
/*     */   }
/*     */   
/*     */   private String extractFrameCount(String message)
/*     */   {
/* 246 */     if (message.length() <= 6) {
/*     */       try {
/* 248 */         int frameCount = Integer.parseInt(message);
/* 249 */         if (frameCount > 0) {
/* 250 */           handleNewFrameCount(frameCount);
/*     */         }
/* 252 */         return null;
/*     */       }
/*     */       catch (NumberFormatException e) {}
/*     */     }
/*     */     
/* 257 */     handleNewFrameCount(1);
/* 258 */     return message;
/*     */   }
/*     */   
/*     */   private void handleIncomingFrame(String message) {
/* 262 */     if (!this.isClosed) {
/* 263 */       resetKeepAlive();
/* 264 */       if (isBuffering()) {
/* 265 */         appendFrame(message);
/*     */       } else {
/* 267 */         String remaining = extractFrameCount(message);
/* 268 */         if (remaining != null) {
/* 269 */           appendFrame(remaining);
/*     */         }
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void resetKeepAlive() {
/* 276 */     if (!this.isClosed) {
/* 277 */       if (this.keepAlive != null) {
/* 278 */         this.keepAlive.cancel(false);
/* 279 */         if (this.logger.logsDebug()) this.logger.debug("Reset keepAlive. Remaining: " + this.keepAlive.getDelay(TimeUnit.MILLISECONDS));
/*     */       }
/* 281 */       else if (this.logger.logsDebug()) { this.logger.debug("Reset keepAlive");
/*     */       }
/* 283 */       this.keepAlive = this.ctx.getRunLoop().schedule(nop(), 45000L);
/*     */     }
/*     */   }
/*     */   
/*     */   private Runnable nop() {
/* 288 */     new Runnable() {
/*     */       public void run() {
/* 290 */         if (WebsocketConnection.this.conn != null) {
/* 291 */           WebsocketConnection.this.conn.send("0");
/* 292 */           WebsocketConnection.this.resetKeepAlive();
/*     */         }
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */   private boolean isBuffering() {
/* 299 */     return this.frameReader != null;
/*     */   }
/*     */   
/*     */ 
/*     */   private void onClosed()
/*     */   {
/* 305 */     if (!this.isClosed) {
/* 306 */       if (this.logger.logsDebug()) this.logger.debug("closing itself");
/* 307 */       shutdown();
/*     */     }
/* 309 */     this.conn = null;
/* 310 */     if (this.keepAlive != null) {
/* 311 */       this.keepAlive.cancel(false);
/*     */     }
/*     */   }
/*     */   
/*     */   private void shutdown() {
/* 316 */     this.isClosed = true;
/* 317 */     this.delegate.onDisconnect(this.everConnected);
/*     */   }
/*     */   
/*     */   private void closeIfNeverConnected() {
/* 321 */     if ((!this.everConnected) && (!this.isClosed)) {
/* 322 */       if (this.logger.logsDebug()) this.logger.debug("timed out on connect");
/* 323 */       this.conn.close();
/*     */     }
/*     */   }
/*     */   
/*     */   private static abstract interface WSClient
/*     */   {
/*     */     public abstract void connect();
/*     */     
/*     */     public abstract void close();
/*     */     
/*     */     public abstract void send(String paramString);
/*     */   }
/*     */   
/*     */   public static abstract interface Delegate
/*     */   {
/*     */     public abstract void onMessage(Map<String, Object> paramMap);
/*     */     
/*     */     public abstract void onDisconnect(boolean paramBoolean);
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/realtime/WebsocketConnection.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */