/*     */ package com.firebase.client.core;
/*     */ 
/*     */ import java.util.Map;
/*     */ 
/*     */ public class PersistentConnection implements com.firebase.client.realtime.Connection.Delegate { private static final String REQUEST_ERROR = "error";
/*     */   private static final String REQUEST_QUERIES = "q";
/*     */   private static final String REQUEST_TAG = "t";
/*     */   private static final String REQUEST_STATUS = "s";
/*     */   private static final String REQUEST_PATH = "p";
/*     */   private static final String REQUEST_NUMBER = "r";
/*     */   private static final String REQUEST_PAYLOAD = "b";
/*     */   private static final String REQUEST_COUNTERS = "c";
/*     */   private static final String REQUEST_DATA_PAYLOAD = "d";
/*     */   private static final String REQUEST_DATA_HASH = "h";
/*     */   private static final String REQUEST_COMPOUND_HASH = "ch";
/*     */   private static final String REQUEST_COMPOUND_HASH_PATHS = "ps";
/*     */   private static final String REQUEST_COMPOUND_HASH_HASHES = "hs";
/*     */   private static final String REQUEST_CREDENTIAL = "cred";
/*     */   private static final String REQUEST_ACTION = "a";
/*     */   private static final String REQUEST_ACTION_STATS = "s";
/*     */   private static final String REQUEST_ACTION_LISTEN = "l";
/*     */   private static final String REQUEST_ACTION_QUERY = "q";
/*     */   private static final String REQUEST_ACTION_PUT = "p";
/*     */   private static final String REQUEST_ACTION_MERGE = "m";
/*     */   private static final String REQUEST_ACTION_UNLISTEN = "u";
/*     */   private static final String REQUEST_ACTION_QUERY_UNLISTEN = "n";
/*     */   private static final String REQUEST_ACTION_ONDISCONNECT_PUT = "o";
/*     */   private static final String REQUEST_ACTION_ONDISCONNECT_MERGE = "om";
/*     */   
/*     */   public static abstract interface Delegate { public abstract void onDataUpdate(String paramString, Object paramObject, boolean paramBoolean, Tag paramTag);
/*     */     
/*     */     public abstract void onRangeMergeUpdate(Path paramPath, java.util.List<RangeMerge> paramList, Tag paramTag);
/*     */     
/*     */     public abstract void onConnect();
/*     */     
/*     */     public abstract void onDisconnect();
/*     */     
/*     */     public abstract void onAuthStatus(boolean paramBoolean);
/*     */     
/*     */     public abstract void onServerInfoUpdate(Map<com.firebase.client.snapshot.ChildKey, Object> paramMap);
/*     */   }
/*     */   
/*     */   private static abstract interface ResponseListener { public abstract void onResponse(Map<String, Object> paramMap);
/*     */   }
/*     */   
/*     */   static abstract interface RequestResultListener { public abstract void onRequestResult(com.firebase.client.FirebaseError paramFirebaseError);
/*     */   }
/*     */   
/*  49 */   static class OutstandingListen { private OutstandingListen(PersistentConnection.RequestResultListener listener, com.firebase.client.core.view.QuerySpec query, Tag tag, SyncTree.SyncTreeHash hashFunction) { this.resultListener = listener;
/*  50 */       this.query = query;
/*  51 */       this.hashFunction = hashFunction;
/*  52 */       this.tag = tag; }
/*     */     
/*     */     private final PersistentConnection.RequestResultListener resultListener;
/*     */     
/*  56 */     public com.firebase.client.core.view.QuerySpec getQuery() { return this.query; }
/*     */     
/*     */     private final com.firebase.client.core.view.QuerySpec query;
/*     */     
/*  60 */     public Tag getTag() { return this.tag; }
/*     */     
/*     */     private final SyncTree.SyncTreeHash hashFunction;
/*     */     private final Tag tag;
/*  64 */     public SyncTree.SyncTreeHash getHashFunction() { return this.hashFunction; }
/*     */     
/*     */ 
/*     */     public String toString()
/*     */     {
/*  69 */       return this.query.toString() + " (Tag: " + this.tag + ")";
/*     */     }
/*     */   }
/*     */   
/*     */   private static class OutstandingPut
/*     */   {
/*     */     private String action;
/*     */     private Map<String, Object> request;
/*     */     private com.firebase.client.Firebase.CompletionListener onComplete;
/*     */     
/*     */     private OutstandingPut(String action, Map<String, Object> request, com.firebase.client.Firebase.CompletionListener onComplete) {
/*  80 */       this.action = action;
/*  81 */       this.request = request;
/*  82 */       this.onComplete = onComplete;
/*     */     }
/*     */     
/*     */     public String getAction() {
/*  86 */       return this.action;
/*     */     }
/*     */     
/*     */     public Map<String, Object> getRequest() {
/*  90 */       return this.request;
/*     */     }
/*     */     
/*     */     public com.firebase.client.Firebase.CompletionListener getOnComplete() {
/*  94 */       return this.onComplete;
/*     */     }
/*     */   }
/*     */   
/*     */   private static class OutstandingDisconnect
/*     */   {
/*     */     private final String action;
/*     */     private final Path path;
/*     */     private final Object data;
/*     */     private final com.firebase.client.Firebase.CompletionListener onComplete;
/*     */     
/*     */     private OutstandingDisconnect(String action, Path path, Object data, com.firebase.client.Firebase.CompletionListener onComplete) {
/* 106 */       this.action = action;
/* 107 */       this.path = path;
/* 108 */       this.data = data;
/* 109 */       this.onComplete = onComplete;
/*     */     }
/*     */     
/*     */     public String getAction() {
/* 113 */       return this.action;
/*     */     }
/*     */     
/*     */     public Path getPath() {
/* 117 */       return this.path;
/*     */     }
/*     */     
/*     */     public Object getData() {
/* 121 */       return this.data;
/*     */     }
/*     */     
/*     */     public com.firebase.client.Firebase.CompletionListener getOnComplete() {
/* 125 */       return this.onComplete;
/*     */     }
/*     */   }
/*     */   
/*     */   private static class AuthCredential {
/*     */     private java.util.List<com.firebase.client.Firebase.AuthListener> listeners;
/*     */     private String credential;
/* 132 */     private boolean onSuccessCalled = false;
/*     */     private Object authData;
/*     */     
/*     */     AuthCredential(com.firebase.client.Firebase.AuthListener listener, String credential)
/*     */     {
/* 137 */       this.listeners = new java.util.ArrayList();
/* 138 */       this.listeners.add(listener);
/* 139 */       this.credential = credential;
/*     */     }
/*     */     
/*     */     public boolean matches(String credential) {
/* 143 */       return this.credential.equals(credential);
/*     */     }
/*     */     
/*     */     public void preempt() {
/* 147 */       com.firebase.client.FirebaseError error = com.firebase.client.FirebaseError.fromStatus("preempted");
/* 148 */       for (com.firebase.client.Firebase.AuthListener listener : this.listeners) {
/* 149 */         listener.onAuthError(error);
/*     */       }
/*     */     }
/*     */     
/*     */     public void addListener(com.firebase.client.Firebase.AuthListener listener) {
/* 154 */       this.listeners.add(listener);
/*     */     }
/*     */     
/*     */     public void replay(com.firebase.client.Firebase.AuthListener listener)
/*     */     {
/* 159 */       assert (this.authData != null);
/* 160 */       listener.onAuthSuccess(this.authData);
/*     */     }
/*     */     
/*     */     public boolean isComplete() {
/* 164 */       return this.onSuccessCalled;
/*     */     }
/*     */     
/*     */     public String getCredential() {
/* 168 */       return this.credential;
/*     */     }
/*     */     
/*     */     public void onCancel(com.firebase.client.FirebaseError error) {
/* 172 */       if (this.onSuccessCalled) {
/* 173 */         onRevoked(error);
/*     */       } else {
/* 175 */         for (com.firebase.client.Firebase.AuthListener listener : this.listeners) {
/* 176 */           listener.onAuthError(error);
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */     public void onRevoked(com.firebase.client.FirebaseError error) {
/* 182 */       for (com.firebase.client.Firebase.AuthListener listener : this.listeners) {
/* 183 */         listener.onAuthRevoked(error);
/*     */       }
/*     */     }
/*     */     
/*     */     public void onSuccess(Object authData) {
/* 188 */       if (!this.onSuccessCalled) {
/* 189 */         this.onSuccessCalled = true;
/* 190 */         this.authData = authData;
/* 191 */         for (com.firebase.client.Firebase.AuthListener listener : this.listeners) {
/* 192 */           listener.onAuthSuccess(authData);
/*     */         }
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private static enum ConnectionState {
/* 199 */     Disconnected, 
/* 200 */     Authenticating, 
/* 201 */     Connected;
/*     */     
/*     */ 
/*     */     private ConnectionState() {}
/*     */   }
/*     */   
/*     */ 
/*     */   private static final String REQUEST_ACTION_ONDISCONNECT_CANCEL = "oc";
/*     */   
/*     */   private static final String REQUEST_ACTION_AUTH = "auth";
/*     */   
/*     */   private static final String REQUEST_ACTION_UNAUTH = "unauth";
/*     */   
/*     */   private static final String RESPONSE_FOR_REQUEST = "b";
/*     */   
/*     */   private static final String SERVER_ASYNC_ACTION = "a";
/*     */   
/*     */   private static final String SERVER_ASYNC_PAYLOAD = "b";
/*     */   
/*     */   private static final String SERVER_ASYNC_DATA_UPDATE = "d";
/*     */   
/*     */   private static final String SERVER_ASYNC_DATA_MERGE = "m";
/*     */   
/*     */   private static final String SERVER_ASYNC_DATA_RANGE_MERGE = "rm";
/*     */   
/*     */   private static final String SERVER_ASYNC_AUTH_REVOKED = "ac";
/*     */   
/*     */   private static final String SERVER_ASYNC_LISTEN_CANCELLED = "c";
/*     */   
/*     */   private static final String SERVER_ASYNC_SECURITY_DEBUG = "sd";
/*     */   
/*     */   private static final String SERVER_DATA_UPDATE_PATH = "p";
/*     */   
/*     */   private static final String SERVER_DATA_UPDATE_BODY = "d";
/*     */   
/*     */   private static final String SERVER_DATA_START_PATH = "s";
/*     */   
/*     */   private static final String SERVER_DATA_END_PATH = "e";
/*     */   
/*     */   private static final String SERVER_DATA_RANGE_MERGE = "m";
/*     */   
/*     */   private static final String SERVER_DATA_TAG = "t";
/*     */   
/*     */   private static final String SERVER_DATA_WARNINGS = "w";
/*     */   
/*     */   private static final String SERVER_RESPONSE_DATA = "d";
/*     */   
/*     */   private static final long RECONNECT_MIN_DELAY = 1000L;
/*     */   
/*     */   private static final long RECONNECT_RESET_TIMEOUT = 30000L;
/*     */   
/*     */   private static final long RECONNECT_MAX_DELAY = 30000L;
/*     */   private static final double RECONNECT_MULTIPLIER = 1.3D;
/* 254 */   private static long connectionIds = 0L;
/*     */   
/*     */   private Delegate delegate;
/*     */   private RepoInfo repoInfo;
/* 258 */   private boolean shouldReconnect = true;
/* 259 */   private boolean firstConnection = true;
/*     */   private long lastConnectionAttemptTime;
/*     */   private long lastConnectionEstablishedTime;
/*     */   private com.firebase.client.realtime.Connection realtime;
/* 263 */   private ConnectionState connectionState = ConnectionState.Disconnected;
/* 264 */   private long writeCounter = 0L;
/* 265 */   private long requestCounter = 0L;
/* 266 */   private long reconnectDelay = 1000L;
/*     */   
/*     */   private Map<Long, ResponseListener> requestCBHash;
/*     */   private boolean writesPaused;
/*     */   private java.util.List<OutstandingDisconnect> onDisconnectRequestQueue;
/*     */   private Map<Long, OutstandingPut> outstandingPuts;
/*     */   private Map<com.firebase.client.core.view.QuerySpec, OutstandingListen> listens;
/*     */   private java.util.Random random;
/*     */   private java.util.concurrent.ScheduledFuture reconnectFuture;
/*     */   private AuthCredential authCredential;
/*     */   private Context ctx;
/*     */   private com.firebase.client.utilities.LogWrapper logger;
/*     */   private String lastSessionId;
/*     */   
/*     */   public PersistentConnection(Context ctx, RepoInfo info, Delegate delegate)
/*     */   {
/* 282 */     this.delegate = delegate;
/* 283 */     this.ctx = ctx;
/* 284 */     this.repoInfo = info;
/* 285 */     this.listens = new java.util.HashMap();
/* 286 */     this.requestCBHash = new java.util.HashMap();
/* 287 */     this.writesPaused = false;
/* 288 */     this.outstandingPuts = new java.util.HashMap();
/* 289 */     this.onDisconnectRequestQueue = new java.util.ArrayList();
/* 290 */     this.random = new java.util.Random();
/* 291 */     long connId = connectionIds++;
/* 292 */     this.logger = this.ctx.getLogger("PersistentConnection", "pc_" + connId);
/* 293 */     this.lastSessionId = null;
/*     */   }
/*     */   
/*     */   public void establishConnection() {
/* 297 */     if (this.shouldReconnect) {
/* 298 */       this.lastConnectionAttemptTime = System.currentTimeMillis();
/* 299 */       this.lastConnectionEstablishedTime = 0L;
/* 300 */       this.realtime = new com.firebase.client.realtime.Connection(this.ctx, this.repoInfo, this, this.lastSessionId);
/* 301 */       this.realtime.open();
/*     */     }
/*     */   }
/*     */   
/*     */   public void onReady(long timestamp, String sessionId)
/*     */   {
/* 307 */     if (this.logger.logsDebug()) this.logger.debug("onReady");
/* 308 */     this.lastConnectionEstablishedTime = System.currentTimeMillis();
/* 309 */     handleTimestamp(timestamp);
/*     */     
/* 311 */     if (this.firstConnection) {
/* 312 */       sendConnectStats();
/*     */     }
/*     */     
/* 315 */     restoreState();
/* 316 */     this.firstConnection = false;
/* 317 */     this.lastSessionId = sessionId;
/* 318 */     this.delegate.onConnect();
/*     */   }
/*     */   
/*     */   public void listen(com.firebase.client.core.view.QuerySpec query, SyncTree.SyncTreeHash currentHashFn, Tag tag, RequestResultListener listener) {
/* 322 */     if (this.logger.logsDebug()) {
/* 323 */       this.logger.debug("Listening on " + query);
/*     */     }
/*     */     
/* 326 */     com.firebase.client.utilities.Utilities.hardAssert((query.isDefault()) || (!query.loadsAllData()), "listen() called for non-default but complete query");
/* 327 */     com.firebase.client.utilities.Utilities.hardAssert(!this.listens.containsKey(query), "listen() called twice for same QuerySpec.");
/* 328 */     if (this.logger.logsDebug()) this.logger.debug("Adding listen query: " + query);
/* 329 */     OutstandingListen outstandingListen = new OutstandingListen(listener, query, tag, currentHashFn, null);
/* 330 */     this.listens.put(query, outstandingListen);
/* 331 */     if (connected()) {
/* 332 */       sendListen(outstandingListen);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public Map<com.firebase.client.core.view.QuerySpec, OutstandingListen> getListens()
/*     */   {
/* 340 */     return this.listens;
/*     */   }
/*     */   
/*     */   public void put(String pathString, Object data, com.firebase.client.Firebase.CompletionListener onComplete) {
/* 344 */     put(pathString, data, null, onComplete);
/*     */   }
/*     */   
/*     */   public void put(String pathString, Object data, String hash, com.firebase.client.Firebase.CompletionListener onComplete) {
/* 348 */     putInternal("p", pathString, data, hash, onComplete);
/*     */   }
/*     */   
/*     */   public void merge(String pathString, Object data, com.firebase.client.Firebase.CompletionListener onComplete) {
/* 352 */     putInternal("m", pathString, data, null, onComplete);
/*     */   }
/*     */   
/*     */   public void purgeOutstandingWrites() {
/* 356 */     com.firebase.client.FirebaseError error = com.firebase.client.FirebaseError.fromCode(-25);
/* 357 */     for (OutstandingPut put : this.outstandingPuts.values()) {
/* 358 */       if (put.onComplete != null) {
/* 359 */         put.onComplete.onComplete(error, null);
/*     */       }
/*     */     }
/* 362 */     for (OutstandingDisconnect onDisconnect : this.onDisconnectRequestQueue) {
/* 363 */       if (onDisconnect.onComplete != null) {
/* 364 */         onDisconnect.onComplete.onComplete(error, null);
/*     */       }
/*     */     }
/* 367 */     this.outstandingPuts.clear();
/* 368 */     this.onDisconnectRequestQueue.clear();
/*     */   }
/*     */   
/*     */   public void onDataMessage(Map<String, Object> message) {
/* 372 */     if (message.containsKey("r"))
/*     */     {
/*     */ 
/* 375 */       long rn = ((Integer)message.get("r")).intValue();
/* 376 */       ResponseListener responseListener = (ResponseListener)this.requestCBHash.remove(Long.valueOf(rn));
/* 377 */       if (responseListener != null)
/*     */       {
/* 379 */         Map<String, Object> response = (Map)message.get("b");
/*     */         
/* 381 */         responseListener.onResponse(response);
/*     */       }
/* 383 */     } else if (!message.containsKey("error"))
/*     */     {
/* 385 */       if (message.containsKey("a")) {
/* 386 */         String action = (String)message.get("a");
/*     */         
/* 388 */         Map<String, Object> body = (Map)message.get("b");
/*     */         
/* 390 */         onDataPush(action, body);
/*     */       }
/* 392 */       else if (this.logger.logsDebug()) { this.logger.debug("Ignoring unknown message: " + message);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public void onDisconnect(com.firebase.client.realtime.Connection.DisconnectReason reason) {
/* 398 */     if (this.logger.logsDebug()) this.logger.debug("Got on disconnect due to " + reason.name());
/* 399 */     this.connectionState = ConnectionState.Disconnected;
/* 400 */     if (!this.shouldReconnect)
/*     */     {
/* 402 */       cancelTransactions();
/* 403 */       this.requestCBHash.clear();
/*     */     } else { long recDelay;
/*     */       long recDelay;
/* 406 */       if (reason == com.firebase.client.realtime.Connection.DisconnectReason.SERVER_RESET) {
/* 407 */         recDelay = 0L;
/*     */       } else {
/* 409 */         if (this.lastConnectionEstablishedTime > 0L) {
/* 410 */           long timeSinceLastConnectSucceeded = System.currentTimeMillis() - this.lastConnectionEstablishedTime;
/* 411 */           if (timeSinceLastConnectSucceeded > 30000L) {
/* 412 */             this.reconnectDelay = 1000L;
/*     */           }
/* 414 */           this.lastConnectionEstablishedTime = 0L;
/*     */         }
/* 416 */         long timeSinceLastConnectAttempt = System.currentTimeMillis() - this.lastConnectionAttemptTime;
/*     */         
/* 418 */         recDelay = Math.max(1L, this.reconnectDelay - timeSinceLastConnectAttempt);
/* 419 */         recDelay = this.random.nextInt((int)recDelay);
/*     */       }
/*     */       
/* 422 */       if (this.logger.logsDebug()) this.logger.debug("Reconnecting in " + recDelay + "ms");
/* 423 */       this.reconnectFuture = this.ctx.getRunLoop().schedule(new Runnable()
/*     */       {
/*     */ 
/* 426 */         public void run() { PersistentConnection.this.establishConnection(); } }, recDelay);
/*     */       
/*     */ 
/*     */ 
/* 430 */       this.reconnectDelay = Math.min(30000L, (this.reconnectDelay * 1.3D));
/*     */     }
/* 432 */     this.delegate.onDisconnect();
/*     */   }
/*     */   
/*     */   public void onKill(String reason) {
/* 436 */     if (this.logger.logsDebug()) this.logger.debug("Firebase connection was forcefully killed by the server. Will not attempt reconnect. Reason: " + reason);
/* 437 */     this.shouldReconnect = false;
/*     */   }
/*     */   
/*     */   void unlisten(com.firebase.client.core.view.QuerySpec query) {
/* 441 */     if (this.logger.logsDebug()) { this.logger.debug("unlistening on " + query);
/*     */     }
/* 443 */     com.firebase.client.utilities.Utilities.hardAssert((query.isDefault()) || (!query.loadsAllData()), "unlisten() called for non-default but complete query");
/* 444 */     OutstandingListen listen = removeListen(query);
/* 445 */     if ((listen != null) && (connected())) {
/* 446 */       sendUnlisten(listen);
/*     */     }
/*     */   }
/*     */   
/*     */   private boolean connected() {
/* 451 */     return this.connectionState != ConnectionState.Disconnected;
/*     */   }
/*     */   
/*     */   void onDisconnectPut(Path path, Object data, com.firebase.client.Firebase.CompletionListener onComplete) {
/* 455 */     if (canSendWrites()) {
/* 456 */       sendOnDisconnect("o", path, data, onComplete);
/*     */     } else {
/* 458 */       this.onDisconnectRequestQueue.add(new OutstandingDisconnect("o", path, data, onComplete, null));
/*     */     }
/*     */   }
/*     */   
/*     */   private boolean canSendWrites()
/*     */   {
/* 464 */     return (this.connectionState == ConnectionState.Connected) && (!this.writesPaused);
/*     */   }
/*     */   
/*     */   void onDisconnectMerge(Path path, Map<String, Object> updates, com.firebase.client.Firebase.CompletionListener onComplete)
/*     */   {
/* 469 */     if (canSendWrites()) {
/* 470 */       sendOnDisconnect("om", path, updates, onComplete);
/*     */     } else {
/* 472 */       this.onDisconnectRequestQueue.add(new OutstandingDisconnect("om", path, updates, onComplete, null));
/*     */     }
/*     */   }
/*     */   
/*     */   void onDisconnectCancel(Path path, com.firebase.client.Firebase.CompletionListener onComplete)
/*     */   {
/* 478 */     if (canSendWrites()) {
/* 479 */       sendOnDisconnect("oc", path, null, onComplete);
/*     */     } else {
/* 481 */       this.onDisconnectRequestQueue.add(new OutstandingDisconnect("oc", path, null, onComplete, null));
/*     */     }
/*     */   }
/*     */   
/*     */   void interrupt()
/*     */   {
/* 487 */     this.shouldReconnect = false;
/* 488 */     if (this.realtime != null) {
/* 489 */       this.realtime.close();
/* 490 */       this.realtime = null;
/*     */     } else {
/* 492 */       if (this.reconnectFuture != null) {
/* 493 */         this.reconnectFuture.cancel(false);
/* 494 */         this.reconnectFuture = null;
/*     */       }
/* 496 */       onDisconnect(com.firebase.client.realtime.Connection.DisconnectReason.OTHER);
/*     */     }
/*     */   }
/*     */   
/*     */   public void resume() {
/* 501 */     this.shouldReconnect = true;
/* 502 */     if (this.realtime == null) {
/* 503 */       establishConnection();
/*     */     }
/*     */   }
/*     */   
/*     */   public void auth(String credential, com.firebase.client.Firebase.AuthListener listener) {
/* 508 */     if (this.authCredential == null) {
/* 509 */       this.authCredential = new AuthCredential(listener, credential);
/* 510 */     } else if (this.authCredential.matches(credential)) {
/* 511 */       this.authCredential.addListener(listener);
/* 512 */       if (this.authCredential.isComplete()) {
/* 513 */         this.authCredential.replay(listener);
/*     */       }
/*     */     } else {
/* 516 */       this.authCredential.preempt();
/* 517 */       this.authCredential = new AuthCredential(listener, credential);
/*     */     }
/* 519 */     if (connected()) {
/* 520 */       if (this.logger.logsDebug()) this.logger.debug("Authenticating with credential: " + credential);
/* 521 */       sendAuth();
/*     */     }
/*     */   }
/*     */   
/*     */   public void unauth(final com.firebase.client.Firebase.CompletionListener listener) {
/* 526 */     this.authCredential = null;
/* 527 */     this.delegate.onAuthStatus(false);
/*     */     
/* 529 */     if (connected()) {
/* 530 */       sendAction("unauth", new java.util.HashMap(), new ResponseListener()
/*     */       {
/*     */         public void onResponse(Map<String, Object> response)
/*     */         {
/* 534 */           String status = (String)response.get("s");
/* 535 */           com.firebase.client.FirebaseError error = null;
/* 536 */           if (!status.equals("ok")) {
/* 537 */             error = com.firebase.client.FirebaseError.fromStatus(status, (String)response.get("d"));
/*     */           }
/*     */           
/* 540 */           listener.onComplete(error, null);
/*     */         }
/*     */       });
/*     */     }
/*     */   }
/*     */   
/*     */   public void pauseWrites()
/*     */   {
/* 548 */     if (this.logger.logsDebug()) this.logger.debug("Writes paused.");
/* 549 */     this.writesPaused = true;
/*     */   }
/*     */   
/*     */   public void unpauseWrites() {
/* 553 */     if (this.logger.logsDebug()) this.logger.debug("Writes unpaused.");
/* 554 */     this.writesPaused = false;
/* 555 */     if (canSendWrites()) {
/* 556 */       restoreWrites();
/*     */     }
/*     */   }
/*     */   
/*     */   public boolean writesPaused() {
/* 561 */     return this.writesPaused;
/*     */   }
/*     */   
/*     */   private void sendOnDisconnect(String action, Path path, Object data, final com.firebase.client.Firebase.CompletionListener onComplete)
/*     */   {
/* 566 */     Map<String, Object> request = new java.util.HashMap();
/* 567 */     request.put("p", path.toString());
/* 568 */     request.put("d", data);
/* 569 */     if (this.logger.logsDebug()) this.logger.debug("onDisconnect " + action + " " + request);
/* 570 */     sendAction(action, request, new ResponseListener()
/*     */     {
/*     */       public void onResponse(Map<String, Object> response) {
/* 573 */         String status = (String)response.get("s");
/* 574 */         com.firebase.client.FirebaseError error = null;
/* 575 */         if (!status.equals("ok")) {
/* 576 */           error = com.firebase.client.FirebaseError.fromStatus(status, (String)response.get("d"));
/*     */         }
/*     */         
/* 579 */         if (onComplete != null) {
/* 580 */           onComplete.onComplete(error, null);
/*     */         }
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private void cancelTransactions() {
/* 587 */     java.util.Iterator<java.util.Map.Entry<Long, OutstandingPut>> iter = this.outstandingPuts.entrySet().iterator();
/* 588 */     while (iter.hasNext()) {
/* 589 */       java.util.Map.Entry<Long, OutstandingPut> entry = (java.util.Map.Entry)iter.next();
/* 590 */       OutstandingPut put = (OutstandingPut)entry.getValue();
/* 591 */       if (put.getRequest().containsKey("h")) {
/* 592 */         put.getOnComplete().onComplete(com.firebase.client.FirebaseError.fromStatus("disconnected"), null);
/* 593 */         iter.remove();
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void sendUnlisten(OutstandingListen listen) {
/* 599 */     Map<String, Object> request = new java.util.HashMap();
/* 600 */     request.put("p", listen.query.getPath().toString());
/*     */     
/* 602 */     Tag tag = listen.getTag();
/* 603 */     if (tag != null) {
/* 604 */       request.put("q", listen.getQuery().getParams().getWireProtocolParams());
/* 605 */       request.put("t", Long.valueOf(tag.getTagNumber()));
/*     */     }
/*     */     
/* 608 */     sendAction("n", request, null);
/*     */   }
/*     */   
/*     */   private OutstandingListen removeListen(com.firebase.client.core.view.QuerySpec query) {
/* 612 */     if (this.logger.logsDebug()) this.logger.debug("removing query " + query);
/* 613 */     if (!this.listens.containsKey(query)) {
/* 614 */       if (this.logger.logsDebug()) this.logger.debug("Trying to remove listener for QuerySpec " + query + " but no listener exists.");
/* 615 */       return null;
/*     */     }
/* 617 */     OutstandingListen oldListen = (OutstandingListen)this.listens.get(query);
/* 618 */     this.listens.remove(query);
/* 619 */     return oldListen;
/*     */   }
/*     */   
/*     */   public java.util.Collection<OutstandingListen> removeListens(Path path)
/*     */   {
/* 624 */     if (this.logger.logsDebug()) this.logger.debug("removing all listens at path " + path);
/* 625 */     java.util.List<OutstandingListen> removedListens = new java.util.ArrayList();
/* 626 */     for (java.util.Map.Entry<com.firebase.client.core.view.QuerySpec, OutstandingListen> entry : this.listens.entrySet()) {
/* 627 */       com.firebase.client.core.view.QuerySpec query = (com.firebase.client.core.view.QuerySpec)entry.getKey();
/* 628 */       OutstandingListen listen = (OutstandingListen)entry.getValue();
/* 629 */       if (query.getPath().equals(path)) {
/* 630 */         removedListens.add(listen);
/*     */       }
/*     */     }
/*     */     
/* 634 */     for (OutstandingListen toRemove : removedListens) {
/* 635 */       this.listens.remove(toRemove.getQuery());
/*     */     }
/*     */     
/* 638 */     return removedListens;
/*     */   }
/*     */   
/*     */   private void onDataPush(String action, Map<String, Object> body) {
/* 642 */     if (this.logger.logsDebug()) this.logger.debug("handleServerMessage: " + action + " " + body);
/* 643 */     if ((action.equals("d")) || (action.equals("m"))) {
/* 644 */       boolean isMerge = action.equals("m");
/*     */       
/* 646 */       String pathString = (String)body.get("p");
/* 647 */       Object payloadData = body.get("d");
/* 648 */       Long tagNumber = com.firebase.client.utilities.Utilities.longFromObject(body.get("t"));
/* 649 */       Tag tag = tagNumber != null ? new Tag(tagNumber.longValue()) : null;
/*     */       
/* 651 */       if ((isMerge) && ((payloadData instanceof Map)) && (((Map)payloadData).size() == 0)) {
/* 652 */         if (this.logger.logsDebug()) this.logger.debug("ignoring empty merge for path " + pathString);
/*     */       } else {
/* 654 */         this.delegate.onDataUpdate(pathString, payloadData, isMerge, tag);
/*     */       }
/* 656 */     } else if (action.equals("rm")) {
/* 657 */       String pathString = (String)body.get("p");
/* 658 */       Object payloadData = body.get("d");
/* 659 */       Long tagNumber = com.firebase.client.utilities.Utilities.longFromObject(body.get("t"));
/* 660 */       Tag tag = tagNumber != null ? new Tag(tagNumber.longValue()) : null;
/* 661 */       java.util.List<Map<String, Object>> ranges = (java.util.List)payloadData;
/* 662 */       java.util.List<RangeMerge> rangeMerges = new java.util.ArrayList();
/* 663 */       for (Map<String, Object> range : ranges) {
/* 664 */         String startString = (String)range.get("s");
/* 665 */         String endString = (String)range.get("e");
/* 666 */         Path start = startString != null ? new Path(startString) : null;
/* 667 */         Path end = endString != null ? new Path(endString) : null;
/* 668 */         com.firebase.client.snapshot.Node update = com.firebase.client.snapshot.NodeUtilities.NodeFromJSON(range.get("m"));
/* 669 */         rangeMerges.add(new RangeMerge(start, end, update));
/*     */       }
/* 671 */       if (rangeMerges.isEmpty()) {
/* 672 */         if (this.logger.logsDebug()) this.logger.debug("Ignoring empty range merge for path " + pathString);
/*     */       } else {
/* 674 */         this.delegate.onRangeMergeUpdate(new Path(pathString), rangeMerges, tag);
/*     */       }
/* 676 */     } else if (action.equals("c")) {
/* 677 */       String pathString = (String)body.get("p");
/* 678 */       onListenRevoked(new Path(pathString));
/* 679 */     } else if (action.equals("ac")) {
/* 680 */       String status = (String)body.get("s");
/* 681 */       String reason = (String)body.get("d");
/* 682 */       onAuthRevoked(status, reason);
/* 683 */     } else if (action.equals("sd")) {
/* 684 */       onSecurityDebugPacket(body);
/*     */     }
/* 686 */     else if (this.logger.logsDebug()) { this.logger.debug("Unrecognized action from server: " + action);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private void onListenRevoked(Path path)
/*     */   {
/* 693 */     java.util.Collection<OutstandingListen> listens = removeListens(path);
/*     */     com.firebase.client.FirebaseError error;
/* 695 */     if (listens != null) {
/* 696 */       error = com.firebase.client.FirebaseError.fromStatus("permission_denied");
/* 697 */       for (OutstandingListen listen : listens) {
/* 698 */         listen.resultListener.onRequestResult(error);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void onAuthRevoked(String status, String reason) {
/* 704 */     if (this.authCredential != null) {
/* 705 */       com.firebase.client.FirebaseError error = com.firebase.client.FirebaseError.fromStatus(status, reason);
/* 706 */       this.authCredential.onRevoked(error);
/* 707 */       this.authCredential = null;
/*     */     }
/*     */   }
/*     */   
/*     */   private void onSecurityDebugPacket(Map<String, Object> message)
/*     */   {
/* 713 */     this.logger.info((String)message.get("msg"));
/*     */   }
/*     */   
/*     */   private void sendAuth() {
/* 717 */     sendAuthHelper(false);
/*     */   }
/*     */   
/*     */   private void sendAuthAndRestoreWrites() {
/* 721 */     sendAuthHelper(true);
/*     */   }
/*     */   
/*     */   private void sendAuthHelper(final boolean restoreWritesAfterComplete) {
/* 725 */     assert (connected()) : "Must be connected to send auth.";
/* 726 */     assert (this.authCredential != null) : "Can't send auth if it's null.";
/*     */     
/* 728 */     Map<String, Object> request = new java.util.HashMap();
/* 729 */     request.put("cred", this.authCredential.getCredential());
/* 730 */     final AuthCredential credential = this.authCredential;
/* 731 */     sendAction("auth", request, new ResponseListener()
/*     */     {
/*     */       public void onResponse(Map<String, Object> response) {
/* 734 */         PersistentConnection.this.connectionState = PersistentConnection.ConnectionState.Connected;
/*     */         
/*     */ 
/* 737 */         if (credential == PersistentConnection.this.authCredential) {
/* 738 */           String status = (String)response.get("s");
/* 739 */           if (status.equals("ok")) {
/* 740 */             PersistentConnection.this.delegate.onAuthStatus(true);
/* 741 */             credential.onSuccess(response.get("d"));
/*     */           }
/*     */           else {
/* 744 */             PersistentConnection.this.authCredential = null;
/* 745 */             PersistentConnection.this.delegate.onAuthStatus(false);
/* 746 */             String reason = (String)response.get("d");
/* 747 */             credential.onCancel(com.firebase.client.FirebaseError.fromStatus(status, reason));
/*     */           }
/*     */         }
/*     */         
/* 751 */         if (restoreWritesAfterComplete) {
/* 752 */           PersistentConnection.this.restoreWrites();
/*     */         }
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private void restoreState() {
/* 759 */     if (this.logger.logsDebug()) { this.logger.debug("calling restore state");
/*     */     }
/* 761 */     if (this.authCredential != null) {
/* 762 */       if (this.logger.logsDebug()) this.logger.debug("Restoring auth.");
/* 763 */       this.connectionState = ConnectionState.Authenticating;
/* 764 */       sendAuthAndRestoreWrites();
/*     */     } else {
/* 766 */       this.connectionState = ConnectionState.Connected;
/*     */     }
/*     */     
/*     */ 
/* 770 */     if (this.logger.logsDebug()) this.logger.debug("Restoring outstanding listens");
/* 771 */     for (OutstandingListen listen : this.listens.values()) {
/* 772 */       if (this.logger.logsDebug()) this.logger.debug("Restoring listen " + listen.getQuery());
/* 773 */       sendListen(listen);
/*     */     }
/*     */     
/* 776 */     if (this.connectionState == ConnectionState.Connected)
/*     */     {
/* 778 */       restoreWrites();
/*     */     }
/*     */   }
/*     */   
/*     */   private void restoreWrites() {
/* 783 */     assert (this.connectionState == ConnectionState.Connected) : "Should be connected if we're restoring writes.";
/*     */     
/* 785 */     if (this.writesPaused) {
/* 786 */       if (this.logger.logsDebug()) this.logger.debug("Writes are paused; skip restoring writes.");
/*     */     } else {
/* 788 */       if (this.logger.logsDebug()) { this.logger.debug("Restoring writes.");
/*     */       }
/* 790 */       java.util.ArrayList<Long> outstanding = new java.util.ArrayList(this.outstandingPuts.keySet());
/*     */       
/* 792 */       java.util.Collections.sort(outstanding);
/* 793 */       for (Long put : outstanding) {
/* 794 */         sendPut(put.longValue());
/*     */       }
/*     */       
/*     */ 
/* 798 */       for (OutstandingDisconnect disconnect : this.onDisconnectRequestQueue) {
/* 799 */         sendOnDisconnect(disconnect.getAction(), disconnect.getPath(), disconnect.getData(), disconnect.getOnComplete());
/*     */       }
/*     */       
/* 802 */       this.onDisconnectRequestQueue.clear();
/*     */     }
/*     */   }
/*     */   
/*     */   private void handleTimestamp(long timestamp) {
/* 807 */     if (this.logger.logsDebug()) this.logger.debug("handling timestamp");
/* 808 */     long timestampDelta = timestamp - System.currentTimeMillis();
/* 809 */     Map<com.firebase.client.snapshot.ChildKey, Object> updates = new java.util.HashMap();
/* 810 */     updates.put(Constants.DOT_INFO_SERVERTIME_OFFSET, Long.valueOf(timestampDelta));
/* 811 */     this.delegate.onServerInfoUpdate(updates);
/*     */   }
/*     */   
/*     */   private Map<String, Object> getPutObject(String pathString, Object data, String hash) {
/* 815 */     Map<String, Object> request = new java.util.HashMap();
/* 816 */     request.put("p", pathString);
/* 817 */     request.put("d", data);
/* 818 */     if (hash != null) {
/* 819 */       request.put("h", hash);
/*     */     }
/* 821 */     return request;
/*     */   }
/*     */   
/*     */   private void putInternal(String action, String pathString, Object data, String hash, com.firebase.client.Firebase.CompletionListener onComplete)
/*     */   {
/* 826 */     Map<String, Object> request = getPutObject(pathString, data, hash);
/*     */     
/*     */ 
/* 829 */     long writeId = this.writeCounter++;
/*     */     
/* 831 */     this.outstandingPuts.put(Long.valueOf(writeId), new OutstandingPut(action, request, onComplete, null));
/* 832 */     if (canSendWrites()) {
/* 833 */       sendPut(writeId);
/*     */     }
/*     */   }
/*     */   
/*     */   private void sendPut(final long putId) {
/* 838 */     assert (canSendWrites()) : "sendPut called when we can't send writes (we're disconnected or writes are paused).";
/* 839 */     OutstandingPut put = (OutstandingPut)this.outstandingPuts.get(Long.valueOf(putId));
/* 840 */     final com.firebase.client.Firebase.CompletionListener onComplete = put.getOnComplete();
/* 841 */     final String action = put.getAction();
/*     */     
/* 843 */     sendAction(action, put.getRequest(), new ResponseListener()
/*     */     {
/*     */       public void onResponse(Map<String, Object> response) {
/* 846 */         if (PersistentConnection.this.logger.logsDebug()) { PersistentConnection.this.logger.debug(action + " response: " + response);
/*     */         }
/* 848 */         PersistentConnection.OutstandingPut currentPut = (PersistentConnection.OutstandingPut)PersistentConnection.this.outstandingPuts.get(Long.valueOf(putId));
/* 849 */         if (currentPut == onComplete) {
/* 850 */           PersistentConnection.this.outstandingPuts.remove(Long.valueOf(putId));
/*     */           
/* 852 */           if (this.val$onComplete != null) {
/* 853 */             String status = (String)response.get("s");
/* 854 */             if (status.equals("ok")) {
/* 855 */               this.val$onComplete.onComplete(null, null);
/*     */             } else {
/* 857 */               this.val$onComplete.onComplete(com.firebase.client.FirebaseError.fromStatus(status, (String)response.get("d")), null);
/*     */             }
/*     */             
/*     */           }
/*     */         }
/* 862 */         else if (PersistentConnection.this.logger.logsDebug()) { PersistentConnection.this.logger.debug("Ignoring on complete for put " + putId + " because it was removed already.");
/*     */         }
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private void sendListen(final OutstandingListen listen) {
/* 869 */     Map<String, Object> request = new java.util.HashMap();
/* 870 */     request.put("p", listen.getQuery().getPath().toString());
/* 871 */     Tag tag = listen.getTag();
/*     */     
/* 873 */     if (tag != null) {
/* 874 */       request.put("q", listen.getQuery().getParams().getWireProtocolParams());
/* 875 */       request.put("t", Long.valueOf(tag.getTagNumber()));
/*     */     }
/*     */     
/* 878 */     SyncTree.SyncTreeHash hashFunction = listen.getHashFunction();
/* 879 */     request.put("h", hashFunction.getSimpleHash());
/*     */     
/* 881 */     if (hashFunction.shouldIncludeCompoundHash()) {
/* 882 */       CompoundHash compoundHash = hashFunction.getCompoundHash();
/*     */       
/* 884 */       java.util.List<String> posts = new java.util.ArrayList();
/* 885 */       for (Path path : compoundHash.getPosts()) {
/* 886 */         posts.add(path.wireFormat());
/*     */       }
/* 888 */       Map<String, Object> hash = new java.util.HashMap();
/* 889 */       hash.put("hs", compoundHash.getHashes());
/* 890 */       hash.put("ps", posts);
/* 891 */       request.put("ch", hash);
/*     */     }
/*     */     
/* 894 */     sendAction("q", request, new ResponseListener()
/*     */     {
/*     */       public void onResponse(Map<String, Object> response)
/*     */       {
/* 898 */         String status = (String)response.get("s");
/*     */         
/* 900 */         if (status.equals("ok")) {
/* 901 */           Map<String, Object> serverBody = (Map)response.get("d");
/* 902 */           if (serverBody.containsKey("w")) {
/* 903 */             java.util.List<String> warnings = (java.util.List)serverBody.get("w");
/* 904 */             PersistentConnection.this.warnOnListenerWarnings(warnings, listen.getQuery());
/*     */           }
/*     */         }
/*     */         
/* 908 */         PersistentConnection.OutstandingListen currentListen = (PersistentConnection.OutstandingListen)PersistentConnection.this.listens.get(listen.getQuery());
/*     */         
/* 910 */         if (currentListen == listen) {
/* 911 */           if (!status.equals("ok")) {
/* 912 */             PersistentConnection.this.removeListen(listen.getQuery());
/* 913 */             com.firebase.client.FirebaseError error = com.firebase.client.FirebaseError.fromStatus(status, (String)response.get("d"));
/* 914 */             listen.resultListener.onRequestResult(error);
/*     */           } else {
/* 916 */             listen.resultListener.onRequestResult(null);
/*     */           }
/*     */         }
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private void sendStats(Map<String, Integer> stats) {
/* 924 */     if (!stats.isEmpty()) {
/* 925 */       Map<String, Object> request = new java.util.HashMap();
/* 926 */       request.put("c", stats);
/* 927 */       sendAction("s", request, new ResponseListener()
/*     */       {
/*     */         public void onResponse(Map<String, Object> response) {
/* 930 */           String status = (String)response.get("s");
/* 931 */           if (!status.equals("ok")) {
/* 932 */             com.firebase.client.FirebaseError error = com.firebase.client.FirebaseError.fromStatus(status, (String)response.get("d"));
/*     */             
/* 934 */             if (PersistentConnection.this.logger.logsDebug()) { PersistentConnection.this.logger.debug("Failed to send stats: " + error);
/*     */             }
/*     */           }
/*     */         }
/*     */       });
/*     */     }
/* 940 */     else if (this.logger.logsDebug()) { this.logger.debug("Not sending stats because stats are empty");
/*     */     }
/*     */   }
/*     */   
/*     */   private void warnOnListenerWarnings(java.util.List<String> warnings, com.firebase.client.core.view.QuerySpec query)
/*     */   {
/* 946 */     if (warnings.contains("no_index")) {
/* 947 */       String indexSpec = "\".indexOn\": \"" + query.getIndex().getQueryDefinition() + '"';
/* 948 */       this.logger.warn("Using an unspecified index. Consider adding '" + indexSpec + "' at " + query.getPath() + " to your security and Firebase rules for better performance");
/*     */     }
/*     */   }
/*     */   
/*     */   private void sendConnectStats()
/*     */   {
/* 954 */     Map<String, Integer> stats = new java.util.HashMap();
/* 955 */     if (AndroidSupport.isAndroid()) {
/* 956 */       if (this.ctx.isPersistenceEnabled()) {
/* 957 */         stats.put("persistence.android.enabled", Integer.valueOf(1));
/*     */       }
/* 959 */       stats.put("sdk.android." + com.firebase.client.Firebase.getSdkVersion().replace('.', '-'), Integer.valueOf(1));
/*     */     } else {
/* 961 */       assert (!this.ctx.isPersistenceEnabled()) : "Stats for persistence on JVM missing (persistence not yet supported)";
/* 962 */       stats.put("sdk.java." + com.firebase.client.Firebase.getSdkVersion().replace('.', '-'), Integer.valueOf(1));
/*     */     }
/* 964 */     if (this.logger.logsDebug()) this.logger.debug("Sending first connection stats");
/* 965 */     sendStats(stats);
/*     */   }
/*     */   
/*     */   private void sendAction(String action, Map<String, Object> message, ResponseListener onResponse) {
/* 969 */     long rn = nextRequestNumber();
/* 970 */     Map<String, Object> request = new java.util.HashMap();
/* 971 */     request.put("r", Long.valueOf(rn));
/* 972 */     request.put("a", action);
/* 973 */     request.put("b", message);
/* 974 */     this.realtime.sendRequest(request);
/* 975 */     this.requestCBHash.put(Long.valueOf(rn), onResponse);
/*     */   }
/*     */   
/*     */   private long nextRequestNumber() {
/* 979 */     return this.requestCounter++;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/PersistentConnection.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */