/*     */ package com.firebase.client.realtime;
/*     */ 
/*     */ import com.firebase.client.core.Context;
/*     */ import com.firebase.client.core.RepoInfo;
/*     */ import com.firebase.client.utilities.LogWrapper;
/*     */ import java.util.HashMap;
/*     */ import java.util.Map;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class Connection
/*     */   implements WebsocketConnection.Delegate
/*     */ {
/*     */   public static enum DisconnectReason
/*     */   {
/*  18 */     SERVER_RESET, 
/*  19 */     OTHER;
/*     */     
/*     */     private DisconnectReason() {}
/*     */   }
/*     */   
/*     */   public static abstract interface Delegate { public abstract void onReady(long paramLong, String paramString);
/*     */     
/*     */     public abstract void onDataMessage(Map<String, Object> paramMap);
/*     */     
/*     */     public abstract void onDisconnect(Connection.DisconnectReason paramDisconnectReason);
/*     */     
/*     */     public abstract void onKill(String paramString); }
/*     */   
/*  32 */   private static long connectionIds = 0L;
/*     */   
/*  34 */   private static enum State { REALTIME_CONNECTING,  REALTIME_CONNECTED,  REALTIME_DISCONNECTED;
/*     */     
/*     */     private State() {}
/*     */   }
/*     */   
/*     */   private static final String REQUEST_TYPE = "t";
/*     */   private static final String REQUEST_TYPE_DATA = "d";
/*     */   private static final String REQUEST_PAYLOAD = "d";
/*     */   private static final String SERVER_ENVELOPE_TYPE = "t";
/*     */   private static final String SERVER_DATA_MESSAGE = "d";
/*     */   private static final String SERVER_CONTROL_MESSAGE = "c";
/*     */   private static final String SERVER_ENVELOPE_DATA = "d";
/*     */   private static final String SERVER_CONTROL_MESSAGE_TYPE = "t";
/*     */   private static final String SERVER_CONTROL_MESSAGE_SHUTDOWN = "s";
/*     */   private static final String SERVER_CONTROL_MESSAGE_RESET = "r";
/*     */   private static final String SERVER_CONTROL_MESSAGE_HELLO = "h";
/*     */   private static final String SERVER_CONTROL_MESSAGE_DATA = "d";
/*     */   private static final String SERVER_HELLO_TIMESTAMP = "ts";
/*     */   private static final String SERVER_HELLO_HOST = "h";
/*     */   private static final String SERVER_HELLO_SESSION_ID = "s";
/*     */   private RepoInfo repoInfo;
/*     */   private WebsocketConnection conn;
/*     */   private Delegate delegate;
/*     */   private State state;
/*     */   private LogWrapper logger;
/*     */   public Connection(Context ctx, RepoInfo repoInfo, Delegate delegate, String optLastSessionId)
/*     */   {
/*  61 */     long connId = connectionIds++;
/*  62 */     this.repoInfo = repoInfo;
/*  63 */     this.delegate = delegate;
/*  64 */     this.logger = ctx.getLogger("Connection", "conn_" + connId);
/*  65 */     this.state = State.REALTIME_CONNECTING;
/*  66 */     this.conn = new WebsocketConnection(ctx, repoInfo, this, optLastSessionId);
/*     */   }
/*     */   
/*     */   public void open() {
/*  70 */     if (this.logger.logsDebug()) this.logger.debug("Opening a connection");
/*  71 */     this.conn.open();
/*     */   }
/*     */   
/*     */   public void close(DisconnectReason reason) {
/*  75 */     if (this.state != State.REALTIME_DISCONNECTED) {
/*  76 */       if (this.logger.logsDebug()) this.logger.debug("closing realtime connection");
/*  77 */       this.state = State.REALTIME_DISCONNECTED;
/*     */       
/*  79 */       if (this.conn != null) {
/*  80 */         this.conn.close();
/*  81 */         this.conn = null;
/*     */       }
/*     */       
/*  84 */       this.delegate.onDisconnect(reason);
/*     */     }
/*     */   }
/*     */   
/*     */   public void close() {
/*  89 */     close(DisconnectReason.OTHER);
/*     */   }
/*     */   
/*     */ 
/*     */   public void sendRequest(Map<String, Object> message)
/*     */   {
/*  95 */     Map<String, Object> request = new HashMap();
/*  96 */     request.put("t", "d");
/*  97 */     request.put("d", message);
/*     */     
/*  99 */     sendData(request);
/*     */   }
/*     */   
/*     */   public void onMessage(Map<String, Object> message)
/*     */   {
/*     */     try {
/* 105 */       String messageType = (String)message.get("t");
/* 106 */       if (messageType != null) {
/* 107 */         if (messageType.equals("d")) {
/* 108 */           Map<String, Object> data = (Map)message.get("d");
/* 109 */           onDataMessage(data);
/* 110 */         } else if (messageType.equals("c")) {
/* 111 */           Map<String, Object> data = (Map)message.get("d");
/* 112 */           onControlMessage(data);
/*     */         }
/* 114 */         else if (this.logger.logsDebug()) { this.logger.debug("Ignoring unknown server message type: " + messageType);
/*     */         }
/*     */       } else {
/* 117 */         if (this.logger.logsDebug()) this.logger.debug("Failed to parse server message: missing message type:" + message.toString());
/* 118 */         close();
/*     */       }
/*     */     } catch (ClassCastException e) {
/* 121 */       if (this.logger.logsDebug()) this.logger.debug("Failed to parse server message: " + e.toString());
/* 122 */       close();
/*     */     }
/*     */   }
/*     */   
/*     */   public void onDisconnect(boolean wasEverConnected)
/*     */   {
/* 128 */     this.conn = null;
/* 129 */     if ((!wasEverConnected) && (this.state == State.REALTIME_CONNECTING)) {
/* 130 */       if (this.logger.logsDebug()) this.logger.debug("Realtime connection failed");
/* 131 */       if (!this.repoInfo.isCacheableHost()) {}
/*     */ 
/*     */ 
/*     */ 
/*     */     }
/* 136 */     else if (this.logger.logsDebug()) { this.logger.debug("Realtime connection lost");
/*     */     }
/*     */     
/* 139 */     close();
/*     */   }
/*     */   
/*     */   private void onDataMessage(Map<String, Object> data) {
/* 143 */     if (this.logger.logsDebug()) { this.logger.debug("received data message: " + data.toString());
/*     */     }
/* 145 */     this.delegate.onDataMessage(data);
/*     */   }
/*     */   
/*     */   private void onControlMessage(Map<String, Object> data) {
/* 149 */     if (this.logger.logsDebug()) this.logger.debug("Got control message: " + data.toString());
/*     */     try {
/* 151 */       String messageType = (String)data.get("t");
/* 152 */       if (messageType != null) {
/* 153 */         if (messageType.equals("s")) {
/* 154 */           String reason = (String)data.get("d");
/* 155 */           onConnectionShutdown(reason);
/* 156 */         } else if (messageType.equals("r")) {
/* 157 */           String host = (String)data.get("d");
/* 158 */           onReset(host);
/* 159 */         } else if (messageType.equals("h")) {
/* 160 */           Map<String, Object> handshakeData = (Map)data.get("d");
/*     */           
/* 162 */           onHandshake(handshakeData);
/*     */         }
/* 164 */         else if (this.logger.logsDebug()) { this.logger.debug("Ignoring unknown control message: " + messageType);
/*     */         }
/*     */       } else {
/* 167 */         if (this.logger.logsDebug()) this.logger.debug("Got invalid control message: " + data.toString());
/* 168 */         close();
/*     */       }
/*     */     } catch (ClassCastException e) {
/* 171 */       if (this.logger.logsDebug()) this.logger.debug("Failed to parse control message: " + e.toString());
/* 172 */       close();
/*     */     }
/*     */   }
/*     */   
/*     */   private void onConnectionShutdown(String reason) {
/* 177 */     if (this.logger.logsDebug()) this.logger.debug("Connection shutdown command received. Shutting down...");
/* 178 */     this.delegate.onKill(reason);
/* 179 */     close();
/*     */   }
/*     */   
/*     */   private void onHandshake(Map<String, Object> handshake) {
/* 183 */     long timestamp = ((Long)handshake.get("ts")).longValue();
/* 184 */     String host = (String)handshake.get("h");
/* 185 */     this.repoInfo.internalHost = host;
/* 186 */     String sessionId = (String)handshake.get("s");
/*     */     
/* 188 */     if (this.state == State.REALTIME_CONNECTING) {
/* 189 */       this.conn.start();
/* 190 */       onConnectionReady(timestamp, sessionId);
/*     */     }
/*     */   }
/*     */   
/*     */   private void onConnectionReady(long timestamp, String sessionId) {
/* 195 */     if (this.logger.logsDebug()) this.logger.debug("realtime connection established");
/* 196 */     this.state = State.REALTIME_CONNECTED;
/* 197 */     this.delegate.onReady(timestamp, sessionId);
/*     */   }
/*     */   
/*     */   private void onReset(String host) {
/* 201 */     if (this.logger.logsDebug()) this.logger.debug("Got a reset; killing connection to " + this.repoInfo.internalHost + "; Updating internalHost to " + host);
/* 202 */     this.repoInfo.internalHost = host;
/*     */     
/*     */ 
/* 205 */     close(DisconnectReason.SERVER_RESET);
/*     */   }
/*     */   
/*     */   private void sendData(Map<String, Object> data) {
/* 209 */     if (this.state != State.REALTIME_CONNECTED) {
/* 210 */       if (this.logger.logsDebug()) this.logger.debug("Tried to send on an unconnected connection");
/*     */     } else {
/* 212 */       if (this.logger.logsDebug()) this.logger.debug("Sending data: " + data.toString());
/* 213 */       this.conn.send(data);
/*     */     }
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/realtime/Connection.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */