/*      */ package com.firebase.client.core;
/*      */ 
/*      */ import com.firebase.client.DataSnapshot;
/*      */ import com.firebase.client.Firebase;
/*      */ import com.firebase.client.Firebase.CompletionListener;
/*      */ import com.firebase.client.FirebaseError;
/*      */ import com.firebase.client.Transaction.Handler;
/*      */ import com.firebase.client.Transaction.Result;
/*      */ import com.firebase.client.core.utilities.Tree;
/*      */ import com.firebase.client.core.view.Event;
/*      */ import com.firebase.client.core.view.QuerySpec;
/*      */ import com.firebase.client.snapshot.ChildKey;
/*      */ import com.firebase.client.snapshot.Node;
/*      */ import com.firebase.client.snapshot.NodeUtilities;
/*      */ import com.firebase.client.utilities.LogWrapper;
/*      */ import java.util.ArrayList;
/*      */ import java.util.List;
/*      */ import java.util.Map;
/*      */ import java.util.Map.Entry;
/*      */ 
/*      */ public class Repo implements PersistentConnection.Delegate
/*      */ {
/*      */   private final RepoInfo repoInfo;
/*   24 */   private final com.firebase.client.utilities.OffsetClock serverClock = new com.firebase.client.utilities.OffsetClock(new com.firebase.client.utilities.DefaultClock(), 0L);
/*      */   private final PersistentConnection connection;
/*      */   private final com.firebase.client.authentication.AuthenticationManager authenticationManager;
/*      */   private SnapshotHolder infoData;
/*      */   private SparseSnapshotTree onDisconnect;
/*      */   private Tree<List<TransactionData>> transactionQueueTree;
/*   30 */   private boolean hijackHash = false;
/*      */   private final com.firebase.client.core.view.EventRaiser eventRaiser;
/*      */   private final Context ctx;
/*      */   private final LogWrapper operationLogger;
/*      */   private final LogWrapper transactionLogger;
/*      */   private final LogWrapper dataLogger;
/*   36 */   public long dataUpdateCount = 0L;
/*   37 */   private long nextWriteId = 1L;
/*      */   private SyncTree infoSyncTree;
/*      */   private SyncTree serverSyncTree;
/*      */   private com.firebase.client.FirebaseApp app;
/*   41 */   private boolean loggedTransactionPersistenceWarning = false;
/*      */   private static final int TRANSACTION_MAX_RETRIES = 25;
/*      */   
/*      */   private static class FirebaseAppImpl extends com.firebase.client.FirebaseApp {
/*   45 */     protected FirebaseAppImpl(Repo repo) { super(); }
/*      */   }
/*      */   
/*      */   Repo(RepoInfo repoInfo, Context ctx)
/*      */   {
/*   50 */     this.repoInfo = repoInfo;
/*   51 */     this.ctx = ctx;
/*   52 */     this.app = new FirebaseAppImpl(this);
/*      */     
/*   54 */     this.operationLogger = this.ctx.getLogger("RepoOperation");
/*   55 */     this.transactionLogger = this.ctx.getLogger("Transaction");
/*   56 */     this.dataLogger = this.ctx.getLogger("DataOperation");
/*      */     
/*   58 */     this.eventRaiser = new com.firebase.client.core.view.EventRaiser(this.ctx);
/*      */     
/*   60 */     this.connection = new PersistentConnection(ctx, repoInfo, this);
/*   61 */     this.authenticationManager = new com.firebase.client.authentication.AuthenticationManager(ctx, this, repoInfo, this.connection);
/*      */     
/*      */ 
/*      */ 
/*   65 */     this.authenticationManager.resumeSession();
/*      */     
/*      */ 
/*   68 */     scheduleNow(new Runnable()
/*      */     {
/*      */       public void run() {
/*   71 */         Repo.this.deferredInitialization();
/*      */       }
/*      */     });
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   private void deferredInitialization()
/*      */   {
/*   82 */     this.connection.establishConnection();
/*      */     
/*   84 */     com.firebase.client.core.persistence.PersistenceManager persistenceManager = this.ctx.getPersistenceManager(this.repoInfo.host);
/*      */     
/*   86 */     this.infoData = new SnapshotHolder();
/*   87 */     this.onDisconnect = new SparseSnapshotTree();
/*      */     
/*   89 */     this.transactionQueueTree = new Tree();
/*      */     
/*   91 */     this.infoSyncTree = new SyncTree(this.ctx, new com.firebase.client.core.persistence.NoopPersistenceManager(), new SyncTree.ListenProvider()
/*      */     {
/*      */       public void startListening(final QuerySpec query, Tag tag, SyncTree.SyncTreeHash hash, final SyncTree.CompletionListener onComplete)
/*      */       {
/*   95 */         Repo.this.scheduleNow(new Runnable()
/*      */         {
/*      */ 
/*      */           public void run()
/*      */           {
/*  100 */             Node node = Repo.this.infoData.getNode(query.getPath());
/*  101 */             if (!node.isEmpty()) {
/*  102 */               List<? extends Event> infoEvents = Repo.this.infoSyncTree.applyServerOverwrite(query.getPath(), node);
/*  103 */               Repo.this.postEvents(infoEvents);
/*  104 */               onComplete.onListenComplete(null);
/*      */             }
/*      */           }
/*      */         });
/*      */       }
/*      */       
/*      */ 
/*      */ 
/*      */       public void stopListening(QuerySpec query, Tag tag) {}
/*  113 */     });
/*  114 */     this.serverSyncTree = new SyncTree(this.ctx, persistenceManager, new SyncTree.ListenProvider()
/*      */     {
/*      */       public void startListening(QuerySpec query, Tag tag, SyncTree.SyncTreeHash hash, final SyncTree.CompletionListener onListenComplete) {
/*  117 */         Repo.this.connection.listen(query, hash, tag, new PersistentConnection.RequestResultListener()
/*      */         {
/*      */           public void onRequestResult(FirebaseError error) {
/*  120 */             List<? extends Event> events = onListenComplete.onListenComplete(error);
/*  121 */             Repo.this.postEvents(events);
/*      */           }
/*      */         });
/*      */       }
/*      */       
/*      */       public void stopListening(QuerySpec query, Tag tag)
/*      */       {
/*  128 */         Repo.this.connection.unlisten(query);
/*      */       }
/*      */       
/*  131 */     });
/*  132 */     restoreWrites(persistenceManager);
/*      */     
/*  134 */     boolean authenticated = this.authenticationManager.getAuth() != null;
/*  135 */     updateInfo(Constants.DOT_INFO_AUTHENTICATED, Boolean.valueOf(authenticated));
/*  136 */     updateInfo(Constants.DOT_INFO_CONNECTED, Boolean.valueOf(false));
/*      */   }
/*      */   
/*      */   private void restoreWrites(com.firebase.client.core.persistence.PersistenceManager persistenceManager) {
/*  140 */     List<UserWriteRecord> writes = persistenceManager.loadUserWrites();
/*      */     
/*  142 */     Map<String, Object> serverValues = ServerValues.generateServerValues(this.serverClock);
/*  143 */     long lastWriteId = Long.MIN_VALUE;
/*  144 */     for (final UserWriteRecord write : writes) {
/*  145 */       Firebase.CompletionListener onComplete = new Firebase.CompletionListener()
/*      */       {
/*      */         public void onComplete(FirebaseError error, Firebase ref) {
/*  148 */           Repo.this.warnIfWriteFailed("Persisted write", write.getPath(), error);
/*  149 */           Repo.this.ackWriteAndRerunTransactions(write.getWriteId(), write.getPath(), error);
/*      */         }
/*      */       };
/*  152 */       if (lastWriteId >= write.getWriteId()) {
/*  153 */         throw new IllegalStateException("Write ids were not in order.");
/*      */       }
/*  155 */       lastWriteId = write.getWriteId();
/*  156 */       this.nextWriteId = (write.getWriteId() + 1L);
/*  157 */       if (write.isOverwrite()) {
/*  158 */         if (this.operationLogger.logsDebug()) this.operationLogger.debug("Restoring overwrite with id " + write.getWriteId());
/*  159 */         this.connection.put(write.getPath().toString(), write.getOverwrite().getValue(true), null, onComplete);
/*  160 */         Node resolved = ServerValues.resolveDeferredValueSnapshot(write.getOverwrite(), serverValues);
/*  161 */         this.serverSyncTree.applyUserOverwrite(write.getPath(), write.getOverwrite(), resolved, write.getWriteId(), true, false);
/*      */       }
/*      */       else {
/*  164 */         if (this.operationLogger.logsDebug()) this.operationLogger.debug("Restoring merge with id " + write.getWriteId());
/*  165 */         this.connection.merge(write.getPath().toString(), write.getMerge().getValue(true), onComplete);
/*  166 */         CompoundWrite resolved = ServerValues.resolveDeferredValueMerge(write.getMerge(), serverValues);
/*  167 */         this.serverSyncTree.applyUserMerge(write.getPath(), write.getMerge(), resolved, write.getWriteId(), false);
/*      */       }
/*      */     }
/*      */   }
/*      */   
/*      */   public com.firebase.client.authentication.AuthenticationManager getAuthenticationManager() {
/*  173 */     return this.authenticationManager;
/*      */   }
/*      */   
/*      */   public com.firebase.client.FirebaseApp getFirebaseApp() {
/*  177 */     return this.app;
/*      */   }
/*      */   
/*      */   public String toString()
/*      */   {
/*  182 */     return this.repoInfo.toString();
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */   public void scheduleNow(Runnable r)
/*      */   {
/*  191 */     this.ctx.requireStarted();
/*  192 */     this.ctx.getRunLoop().scheduleNow(r);
/*      */   }
/*      */   
/*      */   public void postEvent(Runnable r) {
/*  196 */     this.ctx.requireStarted();
/*  197 */     this.ctx.getEventTarget().postEvent(r);
/*      */   }
/*      */   
/*      */   private void postEvents(List<? extends Event> events) {
/*  201 */     if (!events.isEmpty()) {
/*  202 */       this.eventRaiser.raiseEvents(events);
/*      */     }
/*      */   }
/*      */   
/*      */   public long getServerTime() {
/*  207 */     return this.serverClock.millis();
/*      */   }
/*      */   
/*      */   boolean hasListeners() {
/*  211 */     return (!this.infoSyncTree.isEmpty()) || (!this.serverSyncTree.isEmpty());
/*      */   }
/*      */   
/*      */ 
/*      */   public void onDataUpdate(String pathString, Object message, boolean isMerge, Tag tag)
/*      */   {
/*  217 */     if (this.operationLogger.logsDebug()) this.operationLogger.debug("onDataUpdate: " + pathString);
/*  218 */     if (this.dataLogger.logsDebug()) this.operationLogger.debug("onDataUpdate: " + pathString + " " + message);
/*  219 */     this.dataUpdateCount += 1L;
/*      */     
/*  221 */     Path path = new Path(pathString);
/*      */     try {
/*      */       List<? extends Event> events;
/*      */       List<? extends Event> events;
/*  225 */       if (tag != null) { List<? extends Event> events;
/*  226 */         if (isMerge) {
/*  227 */           Map<Path, Node> taggedChildren = new java.util.HashMap();
/*  228 */           Map<String, Object> rawMergeData = (Map)message;
/*  229 */           for (Map.Entry<String, Object> entry : rawMergeData.entrySet()) {
/*  230 */             Node newChildNode = NodeUtilities.NodeFromJSON(entry.getValue());
/*  231 */             taggedChildren.put(new Path((String)entry.getKey()), newChildNode);
/*      */           }
/*  233 */           events = this.serverSyncTree.applyTaggedQueryMerge(path, taggedChildren, tag);
/*      */         } else {
/*  235 */           Node taggedSnap = NodeUtilities.NodeFromJSON(message);
/*  236 */           events = this.serverSyncTree.applyTaggedQueryOverwrite(path, taggedSnap, tag);
/*      */         } } else { List<? extends Event> events;
/*  238 */         if (isMerge) {
/*  239 */           Map<Path, Node> changedChildren = new java.util.HashMap();
/*  240 */           Map<String, Object> rawMergeData = (Map)message;
/*  241 */           for (Map.Entry<String, Object> entry : rawMergeData.entrySet()) {
/*  242 */             Node newChildNode = NodeUtilities.NodeFromJSON(entry.getValue());
/*  243 */             changedChildren.put(new Path((String)entry.getKey()), newChildNode);
/*      */           }
/*  245 */           events = this.serverSyncTree.applyServerMerge(path, changedChildren);
/*      */         } else {
/*  247 */           Node snap = NodeUtilities.NodeFromJSON(message);
/*  248 */           events = this.serverSyncTree.applyServerOverwrite(path, snap);
/*      */         } }
/*  250 */       if (events.size() > 0)
/*      */       {
/*      */ 
/*  253 */         rerunTransactions(path);
/*      */       }
/*      */       
/*  256 */       postEvents(events);
/*      */     } catch (com.firebase.client.FirebaseException e) {
/*  258 */       this.operationLogger.error("FIREBASE INTERNAL ERROR", e);
/*      */     }
/*      */   }
/*      */   
/*      */   public void onRangeMergeUpdate(Path path, List<RangeMerge> merges, Tag tag)
/*      */   {
/*  264 */     if (this.operationLogger.logsDebug()) this.operationLogger.debug("onRangeMergeUpdate: " + path);
/*  265 */     if (this.dataLogger.logsDebug()) this.operationLogger.debug("onRangeMergeUpdate: " + path + " " + merges);
/*  266 */     this.dataUpdateCount += 1L;
/*      */     List<? extends Event> events;
/*      */     List<? extends Event> events;
/*  269 */     if (tag != null) {
/*  270 */       events = this.serverSyncTree.applyTaggedRangeMerges(path, merges, tag);
/*      */     } else {
/*  272 */       events = this.serverSyncTree.applyServerRangeMerges(path, merges);
/*      */     }
/*  274 */     if (events.size() > 0)
/*      */     {
/*      */ 
/*  277 */       rerunTransactions(path);
/*      */     }
/*      */     
/*  280 */     postEvents(events);
/*      */   }
/*      */   
/*      */   void callOnComplete(final Firebase.CompletionListener onComplete, final FirebaseError error, Path path) {
/*  284 */     if (onComplete != null)
/*      */     {
/*  286 */       ChildKey last = path.getBack();
/*  287 */       Firebase ref; final Firebase ref; if ((last != null) && (last.isPriorityChildName())) {
/*  288 */         ref = new Firebase(this, path.getParent());
/*      */       } else {
/*  290 */         ref = new Firebase(this, path);
/*      */       }
/*  292 */       postEvent(new Runnable()
/*      */       {
/*      */         public void run() {
/*  295 */           onComplete.onComplete(error, ref);
/*      */         }
/*      */       });
/*      */     }
/*      */   }
/*      */   
/*      */   private void ackWriteAndRerunTransactions(long writeId, Path path, FirebaseError error) {
/*  302 */     if ((error == null) || (error.getCode() != -25))
/*      */     {
/*      */ 
/*  305 */       boolean success = error == null;
/*  306 */       List<? extends Event> clearEvents = this.serverSyncTree.ackUserWrite(writeId, !success, true, this.serverClock);
/*  307 */       if (clearEvents.size() > 0) {
/*  308 */         rerunTransactions(path);
/*      */       }
/*  310 */       postEvents(clearEvents);
/*      */     }
/*      */   }
/*      */   
/*      */   public void setValue(final Path path, Node newValueUnresolved, Firebase.CompletionListener onComplete) {
/*  315 */     if (this.operationLogger.logsDebug()) this.operationLogger.debug("set: " + path);
/*  316 */     if (this.dataLogger.logsDebug()) { this.dataLogger.debug("set: " + path + " " + newValueUnresolved);
/*      */     }
/*  318 */     Map<String, Object> serverValues = ServerValues.generateServerValues(this.serverClock);
/*  319 */     Node newValue = ServerValues.resolveDeferredValueSnapshot(newValueUnresolved, serverValues);
/*      */     
/*  321 */     final long writeId = getNextWriteId();
/*  322 */     List<? extends Event> events = this.serverSyncTree.applyUserOverwrite(path, newValueUnresolved, newValue, writeId, true, true);
/*      */     
/*  324 */     postEvents(events);
/*      */     
/*  326 */     this.connection.put(path.toString(), newValueUnresolved.getValue(true), new Firebase.CompletionListener()
/*      */     {
/*      */       public void onComplete(FirebaseError error, Firebase ref) {
/*  329 */         Repo.this.warnIfWriteFailed("setValue", path, error);
/*  330 */         Repo.this.ackWriteAndRerunTransactions(writeId, path, error);
/*  331 */         Repo.this.callOnComplete(this.val$onComplete, error, path);
/*      */       }
/*      */       
/*  334 */     });
/*  335 */     Path affectedPath = abortTransactions(path, -9);
/*  336 */     rerunTransactions(affectedPath);
/*      */   }
/*      */   
/*      */ 
/*      */   public void updateChildren(final Path path, CompoundWrite updates, Firebase.CompletionListener onComplete, Map<String, Object> unParsedUpdates)
/*      */   {
/*  342 */     if (this.operationLogger.logsDebug()) this.operationLogger.debug("update: " + path);
/*  343 */     if (this.dataLogger.logsDebug()) this.dataLogger.debug("update: " + path + " " + unParsedUpdates);
/*  344 */     if (updates.isEmpty()) {
/*  345 */       if (this.operationLogger.logsDebug()) { this.operationLogger.debug("update called with no changes. No-op");
/*      */       }
/*  347 */       callOnComplete(onComplete, null, path);
/*  348 */       return;
/*      */     }
/*      */     
/*      */ 
/*  352 */     Map<String, Object> serverValues = ServerValues.generateServerValues(this.serverClock);
/*  353 */     CompoundWrite resolved = ServerValues.resolveDeferredValueMerge(updates, serverValues);
/*      */     
/*  355 */     final long writeId = getNextWriteId();
/*  356 */     List<? extends Event> events = this.serverSyncTree.applyUserMerge(path, updates, resolved, writeId, true);
/*  357 */     postEvents(events);
/*      */     
/*      */ 
/*  360 */     this.connection.merge(path.toString(), unParsedUpdates, new Firebase.CompletionListener()
/*      */     {
/*      */       public void onComplete(FirebaseError error, Firebase ref)
/*      */       {
/*  364 */         Repo.this.warnIfWriteFailed("updateChildren", path, error);
/*  365 */         Repo.this.ackWriteAndRerunTransactions(writeId, path, error);
/*  366 */         Repo.this.callOnComplete(this.val$onComplete, error, path);
/*      */       }
/*      */       
/*  369 */     });
/*  370 */     Path affectedPath = abortTransactions(path, -9);
/*  371 */     rerunTransactions(affectedPath);
/*      */   }
/*      */   
/*      */   public void purgeOutstandingWrites() {
/*  375 */     if (this.operationLogger.logsDebug()) this.operationLogger.debug("Purging writes");
/*  376 */     List<? extends Event> events = this.serverSyncTree.removeAllWrites();
/*  377 */     postEvents(events);
/*      */     
/*  379 */     abortTransactions(Path.getEmptyPath(), -25);
/*      */     
/*  381 */     this.connection.purgeOutstandingWrites();
/*      */   }
/*      */   
/*      */   public void removeEventCallback(@com.firebase.client.annotations.NotNull EventRegistration eventRegistration)
/*      */   {
/*      */     List<Event> events;
/*      */     List<Event> events;
/*  388 */     if (Constants.DOT_INFO.equals(eventRegistration.getQuerySpec().getPath().getFront())) {
/*  389 */       events = this.infoSyncTree.removeEventRegistration(eventRegistration);
/*      */     } else {
/*  391 */       events = this.serverSyncTree.removeEventRegistration(eventRegistration);
/*      */     }
/*  393 */     postEvents(events);
/*      */   }
/*      */   
/*      */   public void onDisconnectSetValue(final Path path, final Node newValue, final Firebase.CompletionListener onComplete) {
/*  397 */     this.connection.onDisconnectPut(path, newValue.getValue(true), new Firebase.CompletionListener()
/*      */     {
/*      */       public void onComplete(FirebaseError error, Firebase ref) {
/*  400 */         Repo.this.warnIfWriteFailed("onDisconnect().setValue", path, error);
/*  401 */         if (error == null) {
/*  402 */           Repo.this.onDisconnect.remember(path, newValue);
/*      */         }
/*  404 */         Repo.this.callOnComplete(onComplete, error, path);
/*      */       }
/*      */     });
/*      */   }
/*      */   
/*      */   public void onDisconnectUpdate(final Path path, final Map<Path, Node> newChildren, final Firebase.CompletionListener listener, Map<String, Object> unParsedUpdates)
/*      */   {
/*  411 */     this.connection.onDisconnectMerge(path, unParsedUpdates, new Firebase.CompletionListener()
/*      */     {
/*      */       public void onComplete(FirebaseError error, Firebase ref) {
/*  414 */         Repo.this.warnIfWriteFailed("onDisconnect().updateChildren", path, error);
/*  415 */         if (error == null) {
/*  416 */           for (Map.Entry<Path, Node> entry : newChildren.entrySet()) {
/*  417 */             Repo.this.onDisconnect.remember(path.child((Path)entry.getKey()), (Node)entry.getValue());
/*      */           }
/*      */         }
/*  420 */         Repo.this.callOnComplete(listener, error, path);
/*      */       }
/*      */     });
/*      */   }
/*      */   
/*      */   public void onDisconnectCancel(final Path path, final Firebase.CompletionListener onComplete) {
/*  426 */     this.connection.onDisconnectCancel(path, new Firebase.CompletionListener()
/*      */     {
/*      */       public void onComplete(FirebaseError error, Firebase ref) {
/*  429 */         if (error == null) {
/*  430 */           Repo.this.onDisconnect.forget(path);
/*      */         }
/*  432 */         Repo.this.callOnComplete(onComplete, error, path);
/*      */       }
/*      */     });
/*      */   }
/*      */   
/*      */   public void onConnect() {
/*  438 */     onServerInfoUpdate(Constants.DOT_INFO_CONNECTED, Boolean.valueOf(true));
/*      */   }
/*      */   
/*      */   public void onDisconnect() {
/*  442 */     onServerInfoUpdate(Constants.DOT_INFO_CONNECTED, Boolean.valueOf(false));
/*  443 */     runOnDisconnectEvents();
/*      */   }
/*      */   
/*      */   public void onAuthStatus(boolean authOk) {
/*  447 */     onServerInfoUpdate(Constants.DOT_INFO_AUTHENTICATED, Boolean.valueOf(authOk));
/*      */   }
/*      */   
/*      */   public void onServerInfoUpdate(ChildKey key, Object value) {
/*  451 */     updateInfo(key, value);
/*      */   }
/*      */   
/*      */   public void onServerInfoUpdate(Map<ChildKey, Object> updates) {
/*  455 */     for (Map.Entry<ChildKey, Object> entry : updates.entrySet()) {
/*  456 */       updateInfo((ChildKey)entry.getKey(), entry.getValue());
/*      */     }
/*      */   }
/*      */   
/*      */   void interrupt() {
/*  461 */     this.connection.interrupt();
/*      */   }
/*      */   
/*      */   void resume() {
/*  465 */     this.connection.resume();
/*      */   }
/*      */   
/*      */   public void addEventCallback(@com.firebase.client.annotations.NotNull EventRegistration eventRegistration)
/*      */   {
/*  470 */     ChildKey front = eventRegistration.getQuerySpec().getPath().getFront();
/*  471 */     List<? extends Event> events; List<? extends Event> events; if ((front != null) && (front.equals(Constants.DOT_INFO))) {
/*  472 */       events = this.infoSyncTree.addEventRegistration(eventRegistration);
/*      */     } else {
/*  474 */       events = this.serverSyncTree.addEventRegistration(eventRegistration);
/*      */     }
/*  476 */     postEvents(events);
/*      */   }
/*      */   
/*      */   public void keepSynced(QuerySpec query, boolean keep) {
/*  480 */     assert ((query.getPath().isEmpty()) || (!query.getPath().getFront().equals(Constants.DOT_INFO)));
/*      */     
/*  482 */     this.serverSyncTree.keepSynced(query, keep);
/*      */   }
/*      */   
/*      */   PersistentConnection getConnection() {
/*  486 */     return this.connection;
/*      */   }
/*      */   
/*      */   private void updateInfo(ChildKey childKey, Object value) {
/*  490 */     if (childKey.equals(Constants.DOT_INFO_SERVERTIME_OFFSET)) {
/*  491 */       this.serverClock.setOffset(((Long)value).longValue());
/*      */     }
/*      */     
/*  494 */     Path path = new Path(new ChildKey[] { Constants.DOT_INFO, childKey });
/*      */     try {
/*  496 */       Node node = NodeUtilities.NodeFromJSON(value);
/*  497 */       this.infoData.update(path, node);
/*  498 */       List<? extends Event> events = this.infoSyncTree.applyServerOverwrite(path, node);
/*  499 */       postEvents(events);
/*      */     } catch (com.firebase.client.FirebaseException e) {
/*  501 */       this.operationLogger.error("Failed to parse info update", e);
/*      */     }
/*      */   }
/*      */   
/*      */   private long getNextWriteId() {
/*  506 */     return this.nextWriteId++;
/*      */   }
/*      */   
/*      */   private void runOnDisconnectEvents() {
/*  510 */     Map<String, Object> serverValues = ServerValues.generateServerValues(this.serverClock);
/*  511 */     SparseSnapshotTree resolvedTree = ServerValues.resolveDeferredValueTree(this.onDisconnect, serverValues);
/*  512 */     final List<Event> events = new ArrayList();
/*      */     
/*  514 */     resolvedTree.forEachTree(Path.getEmptyPath(), new SparseSnapshotTree.SparseSnapshotTreeVisitor()
/*      */     {
/*      */       public void visitTree(Path prefixPath, Node node) {
/*  517 */         events.addAll(Repo.this.serverSyncTree.applyServerOverwrite(prefixPath, node));
/*  518 */         Path affectedPath = Repo.this.abortTransactions(prefixPath, -9);
/*  519 */         Repo.this.rerunTransactions(affectedPath);
/*      */       }
/*  521 */     });
/*  522 */     this.onDisconnect = new SparseSnapshotTree();
/*  523 */     postEvents(events);
/*      */   }
/*      */   
/*      */   private void warnIfWriteFailed(String writeType, Path path, FirebaseError error)
/*      */   {
/*  528 */     if ((error != null) && (error.getCode() != -1) && (error.getCode() != -25)) {
/*  529 */       this.operationLogger.warn(writeType + " at " + path.toString() + " failed: " + error.toString());
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */ 
/*      */ 
/*      */   private static final String TRANSACTION_TOO_MANY_RETRIES = "maxretries";
/*      */   
/*      */ 
/*      */   private static final String TRANSACTION_OVERRIDE_BY_SET = "overriddenBySet";
/*      */   
/*      */ 
/*      */   private static enum TransactionStatus
/*      */   {
/*  544 */     INITIALIZING, 
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*  549 */     RUN, 
/*      */     
/*      */ 
/*  552 */     SENT, 
/*      */     
/*      */ 
/*  555 */     COMPLETED, 
/*      */     
/*      */ 
/*  558 */     SENT_NEEDS_ABORT, 
/*      */     
/*  560 */     NEEDS_ABORT;
/*      */     private TransactionStatus() {} }
/*  562 */   private long transactionOrder = 0L;
/*      */   
/*      */   private static class TransactionData implements Comparable<TransactionData>
/*      */   {
/*      */     private Path path;
/*      */     private Transaction.Handler handler;
/*      */     private com.firebase.client.ValueEventListener outstandingListener;
/*      */     private Repo.TransactionStatus status;
/*      */     private long order;
/*      */     private boolean applyLocally;
/*      */     private int retryCount;
/*      */     private FirebaseError abortReason;
/*      */     private long currentWriteId;
/*      */     private Node currentInputSnapshot;
/*      */     private Node currentOutputSnapshotRaw;
/*      */     private Node currentOutputSnapshotResolved;
/*      */     
/*      */     private TransactionData(Path path, Transaction.Handler handler, com.firebase.client.ValueEventListener outstandingListener, Repo.TransactionStatus status, boolean applyLocally, long order) {
/*  580 */       this.path = path;
/*  581 */       this.handler = handler;
/*  582 */       this.outstandingListener = outstandingListener;
/*  583 */       this.status = status;
/*  584 */       this.retryCount = 0;
/*  585 */       this.applyLocally = applyLocally;
/*  586 */       this.order = order;
/*  587 */       this.abortReason = null;
/*  588 */       this.currentInputSnapshot = null;
/*  589 */       this.currentOutputSnapshotRaw = null;
/*  590 */       this.currentOutputSnapshotResolved = null;
/*      */     }
/*      */     
/*      */     public int compareTo(TransactionData o)
/*      */     {
/*  595 */       if (this.order < o.order)
/*  596 */         return -1;
/*  597 */       if (this.order == o.order) {
/*  598 */         return 0;
/*      */       }
/*  600 */       return 1;
/*      */     }
/*      */   }
/*      */   
/*      */ 
/*      */   public void startTransaction(Path path, final Transaction.Handler handler, boolean applyLocally)
/*      */   {
/*  607 */     if (this.operationLogger.logsDebug()) this.operationLogger.debug("transaction: " + path);
/*  608 */     if (this.dataLogger.logsDebug()) { this.operationLogger.debug("transaction: " + path);
/*      */     }
/*  610 */     if ((this.ctx.isPersistenceEnabled()) && (!this.loggedTransactionPersistenceWarning)) {
/*  611 */       this.loggedTransactionPersistenceWarning = true;
/*  612 */       this.transactionLogger.info("runTransaction() usage detected while persistence is enabled. Please be aware that transactions *will not* be persisted across app restarts.  See https://www.firebase.com/docs/android/guide/offline-capabilities.html#section-handling-transactions-offline for more details.");
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*  619 */     Firebase watchRef = new Firebase(this, path);
/*  620 */     com.firebase.client.ValueEventListener listener = new com.firebase.client.ValueEventListener()
/*      */     {
/*      */       public void onDataChange(DataSnapshot snapshot) {}
/*      */       
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */ 
/*      */       public void onCancelled(FirebaseError error) {}
/*  630 */     };
/*  631 */     addEventCallback(new ValueEventRegistration(this, listener, watchRef.getSpec()));
/*      */     
/*  633 */     TransactionData transaction = new TransactionData(path, handler, listener, TransactionStatus.INITIALIZING, applyLocally, nextTransactionOrder(), null);
/*      */     
/*      */ 
/*      */ 
/*  637 */     Node currentState = getLatestState(path);
/*  638 */     transaction.currentInputSnapshot = currentState;
/*  639 */     com.firebase.client.MutableData mutableCurrent = new com.firebase.client.MutableData(currentState);
/*      */     
/*  641 */     FirebaseError error = null;
/*      */     Transaction.Result result;
/*      */     try {
/*  644 */       result = handler.doTransaction(mutableCurrent);
/*  645 */       if (result == null) {
/*  646 */         throw new NullPointerException("Transaction returned null as result");
/*      */       }
/*      */     } catch (Throwable e) {
/*  649 */       error = FirebaseError.fromException(e);
/*  650 */       result = com.firebase.client.Transaction.abort();
/*      */     }
/*  652 */     if (!result.isSuccess())
/*      */     {
/*  654 */       transaction.currentOutputSnapshotRaw = null;
/*  655 */       transaction.currentOutputSnapshotResolved = null;
/*  656 */       final FirebaseError innerClassError = error;
/*  657 */       final DataSnapshot snap = new DataSnapshot(watchRef, com.firebase.client.snapshot.IndexedNode.from(transaction.currentInputSnapshot));
/*  658 */       postEvent(new Runnable()
/*      */       {
/*      */         public void run() {
/*  661 */           handler.onComplete(innerClassError, false, snap);
/*      */         }
/*      */       });
/*      */     }
/*      */     else {
/*  666 */       transaction.status = TransactionStatus.RUN;
/*      */       
/*  668 */       Tree<List<TransactionData>> queueNode = this.transactionQueueTree.subTree(path);
/*  669 */       List<TransactionData> nodeQueue = (List)queueNode.getValue();
/*  670 */       if (nodeQueue == null) {
/*  671 */         nodeQueue = new ArrayList();
/*      */       }
/*  673 */       nodeQueue.add(transaction);
/*  674 */       queueNode.setValue(nodeQueue);
/*      */       
/*  676 */       Map<String, Object> serverValues = ServerValues.generateServerValues(this.serverClock);
/*  677 */       Node newNodeUnresolved = result.getNode();
/*  678 */       Node newNode = ServerValues.resolveDeferredValueSnapshot(newNodeUnresolved, serverValues);
/*      */       
/*  680 */       transaction.currentOutputSnapshotRaw = newNodeUnresolved;
/*  681 */       transaction.currentOutputSnapshotResolved = newNode;
/*  682 */       transaction.currentWriteId = getNextWriteId();
/*      */       
/*  684 */       List<? extends Event> events = this.serverSyncTree.applyUserOverwrite(path, newNodeUnresolved, newNode, transaction.currentWriteId, applyLocally, false);
/*      */       
/*  686 */       postEvents(events);
/*  687 */       sendAllReadyTransactions();
/*      */     }
/*      */   }
/*      */   
/*      */   private Node getLatestState(Path path) {
/*  692 */     return getLatestState(path, new ArrayList());
/*      */   }
/*      */   
/*      */   private Node getLatestState(Path path, List<Long> excudeSets) {
/*  696 */     Node state = this.serverSyncTree.calcCompleteEventCache(path, excudeSets);
/*  697 */     if (state == null) {
/*  698 */       state = com.firebase.client.snapshot.EmptyNode.Empty();
/*      */     }
/*  700 */     return state;
/*      */   }
/*      */   
/*      */   public void setHijackHash(boolean hijackHash) {
/*  704 */     this.hijackHash = hijackHash;
/*      */   }
/*      */   
/*      */   private void sendAllReadyTransactions() {
/*  708 */     Tree<List<TransactionData>> node = this.transactionQueueTree;
/*      */     
/*  710 */     pruneCompletedTransactions(node);
/*  711 */     sendReadyTransactions(node);
/*      */   }
/*      */   
/*      */   private void sendReadyTransactions(Tree<List<TransactionData>> node) {
/*  715 */     List<TransactionData> queue = (List)node.getValue();
/*  716 */     if (queue != null) {
/*  717 */       queue = buildTransactionQueue(node);
/*  718 */       assert (queue.size() > 0);
/*      */       
/*  720 */       Boolean allRun = Boolean.valueOf(true);
/*  721 */       for (TransactionData transaction : queue) {
/*  722 */         if (transaction.status != TransactionStatus.RUN) {
/*  723 */           allRun = Boolean.valueOf(false);
/*  724 */           break;
/*      */         }
/*      */       }
/*      */       
/*  728 */       if (allRun.booleanValue()) {
/*  729 */         sendTransactionQueue(queue, node.getPath());
/*      */       }
/*  731 */     } else if (node.hasChildren()) {
/*  732 */       node.forEachChild(new com.firebase.client.core.utilities.Tree.TreeVisitor()
/*      */       {
/*      */         public void visitTree(Tree<List<Repo.TransactionData>> tree) {
/*  735 */           Repo.this.sendReadyTransactions(tree);
/*      */         }
/*      */       });
/*      */     }
/*      */   }
/*      */   
/*      */   private void sendTransactionQueue(final List<TransactionData> queue, final Path path)
/*      */   {
/*  743 */     List<Long> setsToIgnore = new ArrayList();
/*  744 */     for (TransactionData txn : queue) {
/*  745 */       setsToIgnore.add(Long.valueOf(txn.currentWriteId));
/*      */     }
/*      */     
/*  748 */     Node latestState = getLatestState(path, setsToIgnore);
/*  749 */     Node snapToSend = latestState;
/*  750 */     String latestHash = "badhash";
/*  751 */     if (!this.hijackHash) {
/*  752 */       latestHash = latestState.getHash();
/*      */     }
/*      */     
/*  755 */     for (TransactionData txn : queue) {
/*  756 */       assert (txn.status == TransactionStatus.RUN);
/*  757 */       txn.status = TransactionStatus.SENT;
/*  758 */       TransactionData.access$1808(txn);
/*  759 */       Path relativePath = Path.getRelative(path, txn.path);
/*      */       
/*  761 */       snapToSend = snapToSend.updateChild(relativePath, txn.currentOutputSnapshotRaw);
/*      */     }
/*      */     
/*  764 */     Object dataToSend = snapToSend.getValue(true);
/*      */     
/*  766 */     final Repo repo = this;
/*  767 */     long writeId = getNextWriteId();
/*      */     
/*      */ 
/*  770 */     this.connection.put(path.toString(), dataToSend, latestHash, new Firebase.CompletionListener()
/*      */     {
/*      */       public void onComplete(FirebaseError error, Firebase ref) {
/*  773 */         Repo.this.warnIfWriteFailed("Transaction", path, error);
/*  774 */         List<Event> events = new ArrayList();
/*      */         
/*  776 */         if (error == null) {
/*  777 */           List<Runnable> callbacks = new ArrayList();
/*  778 */           for (final Repo.TransactionData txn : queue) {
/*  779 */             txn.status = Repo.TransactionStatus.COMPLETED;
/*  780 */             events.addAll(Repo.this.serverSyncTree.ackUserWrite(txn.currentWriteId, false, false, Repo.this.serverClock));
/*      */             
/*      */ 
/*  783 */             Node node = txn.currentOutputSnapshotResolved;
/*  784 */             final DataSnapshot snap = new DataSnapshot(new Firebase(repo, txn.path), com.firebase.client.snapshot.IndexedNode.from(node));
/*      */             
/*  786 */             callbacks.add(new Runnable()
/*      */             {
/*      */               public void run() {
/*  789 */                 txn.handler.onComplete(null, true, snap);
/*      */               }
/*      */               
/*  792 */             });
/*  793 */             Repo.this.removeEventCallback(new ValueEventRegistration(Repo.this, txn.outstandingListener, QuerySpec.defaultQueryAtPath(txn.path)));
/*      */           }
/*      */           
/*      */ 
/*      */ 
/*  798 */           Repo.this.pruneCompletedTransactions(Repo.this.transactionQueueTree.subTree(path));
/*      */           
/*      */ 
/*  801 */           Repo.this.sendAllReadyTransactions();
/*      */           
/*  803 */           repo.postEvents(events);
/*      */           
/*      */ 
/*  806 */           for (int i = 0; i < callbacks.size(); i++) {
/*  807 */             Repo.this.postEvent((Runnable)callbacks.get(i));
/*      */           }
/*      */         }
/*      */         else {
/*  811 */           if (error.getCode() == -1) {
/*  812 */             for (Repo.TransactionData transaction : queue) {
/*  813 */               if (transaction.status == Repo.TransactionStatus.SENT_NEEDS_ABORT) {
/*  814 */                 transaction.status = Repo.TransactionStatus.NEEDS_ABORT;
/*      */               } else {
/*  816 */                 transaction.status = Repo.TransactionStatus.RUN;
/*      */               }
/*      */             }
/*      */           } else {
/*  820 */             for (Repo.TransactionData transaction : queue) {
/*  821 */               transaction.status = Repo.TransactionStatus.NEEDS_ABORT;
/*  822 */               transaction.abortReason = error;
/*      */             }
/*      */           }
/*      */           
/*      */ 
/*  827 */           Repo.this.rerunTransactions(path);
/*      */         }
/*      */       }
/*      */     });
/*      */   }
/*      */   
/*      */   private void pruneCompletedTransactions(Tree<List<TransactionData>> node) {
/*  834 */     List<TransactionData> queue = (List)node.getValue();
/*  835 */     if (queue != null) {
/*  836 */       int i = 0;
/*  837 */       while (i < queue.size()) {
/*  838 */         TransactionData transaction = (TransactionData)queue.get(i);
/*  839 */         if (transaction.status == TransactionStatus.COMPLETED) {
/*  840 */           queue.remove(i);
/*      */         } else {
/*  842 */           i++;
/*      */         }
/*      */       }
/*  845 */       if (queue.size() > 0) {
/*  846 */         node.setValue(queue);
/*      */       } else {
/*  848 */         node.setValue(null);
/*      */       }
/*      */     }
/*      */     
/*  852 */     node.forEachChild(new com.firebase.client.core.utilities.Tree.TreeVisitor()
/*      */     {
/*      */       public void visitTree(Tree<List<Repo.TransactionData>> tree) {
/*  855 */         Repo.this.pruneCompletedTransactions(tree);
/*      */       }
/*      */     });
/*      */   }
/*      */   
/*      */   private long nextTransactionOrder() {
/*  861 */     return this.transactionOrder++;
/*      */   }
/*      */   
/*      */   private Path rerunTransactions(Path changedPath) {
/*  865 */     Tree<List<TransactionData>> rootMostTransactionNode = getAncestorTransactionNode(changedPath);
/*  866 */     Path path = rootMostTransactionNode.getPath();
/*      */     
/*  868 */     List<TransactionData> queue = buildTransactionQueue(rootMostTransactionNode);
/*  869 */     rerunTransactionQueue(queue, path);
/*      */     
/*  871 */     return path;
/*      */   }
/*      */   
/*      */   private void rerunTransactionQueue(List<TransactionData> queue, Path path) {
/*  875 */     if (queue.isEmpty()) {
/*  876 */       return;
/*      */     }
/*      */     
/*      */ 
/*      */ 
/*  881 */     List<Runnable> callbacks = new ArrayList();
/*      */     
/*      */ 
/*      */ 
/*      */ 
/*  886 */     List<Long> setsToIgnore = new ArrayList();
/*  887 */     for (TransactionData transaction : queue) {
/*  888 */       setsToIgnore.add(Long.valueOf(transaction.currentWriteId));
/*      */     }
/*      */     
/*  891 */     for (final TransactionData transaction : queue) {
/*  892 */       Path relativePath = Path.getRelative(path, transaction.path);
/*  893 */       boolean abortTransaction = false;
/*  894 */       FirebaseError abortReason = null;
/*  895 */       List<Event> events = new ArrayList();
/*      */       
/*  897 */       assert (relativePath != null);
/*      */       
/*  899 */       if (transaction.status == TransactionStatus.NEEDS_ABORT) {
/*  900 */         abortTransaction = true;
/*  901 */         abortReason = transaction.abortReason;
/*  902 */         if (abortReason.getCode() != -25) {
/*  903 */           events.addAll(this.serverSyncTree.ackUserWrite(transaction.currentWriteId, true, false, this.serverClock));
/*      */         }
/*  905 */       } else if (transaction.status == TransactionStatus.RUN) {
/*  906 */         if (transaction.retryCount >= 25) {
/*  907 */           abortTransaction = true;
/*  908 */           abortReason = FirebaseError.fromStatus("maxretries");
/*  909 */           events.addAll(this.serverSyncTree.ackUserWrite(transaction.currentWriteId, true, false, this.serverClock));
/*      */         }
/*      */         else {
/*  912 */           Node currentNode = getLatestState(transaction.path, setsToIgnore);
/*  913 */           transaction.currentInputSnapshot = currentNode;
/*  914 */           com.firebase.client.MutableData mutableCurrent = new com.firebase.client.MutableData(currentNode);
/*  915 */           FirebaseError error = null;
/*      */           Transaction.Result result;
/*      */           try {
/*  918 */             result = transaction.handler.doTransaction(mutableCurrent);
/*      */           } catch (Throwable e) {
/*  920 */             error = FirebaseError.fromException(e);
/*  921 */             result = com.firebase.client.Transaction.abort();
/*      */           }
/*  923 */           if (result.isSuccess()) {
/*  924 */             Long oldWriteId = Long.valueOf(transaction.currentWriteId);
/*  925 */             Map<String, Object> serverValues = ServerValues.generateServerValues(this.serverClock);
/*      */             
/*  927 */             Node newDataNode = result.getNode();
/*  928 */             Node newNodeResolved = ServerValues.resolveDeferredValueSnapshot(newDataNode, serverValues);
/*      */             
/*  930 */             transaction.currentOutputSnapshotRaw = newDataNode;
/*  931 */             transaction.currentOutputSnapshotResolved = newNodeResolved;
/*  932 */             transaction.currentWriteId = getNextWriteId();
/*      */             
/*      */ 
/*  935 */             setsToIgnore.remove(oldWriteId);
/*  936 */             events.addAll(this.serverSyncTree.applyUserOverwrite(transaction.path, newDataNode, newNodeResolved, transaction.currentWriteId, transaction.applyLocally, false));
/*      */             
/*  938 */             events.addAll(this.serverSyncTree.ackUserWrite(oldWriteId.longValue(), true, false, this.serverClock));
/*      */           }
/*      */           else {
/*  941 */             abortTransaction = true;
/*  942 */             abortReason = error;
/*  943 */             events.addAll(this.serverSyncTree.ackUserWrite(transaction.currentWriteId, true, false, this.serverClock));
/*      */           }
/*      */         }
/*      */       }
/*      */       
/*  948 */       postEvents(events);
/*      */       
/*  950 */       if (abortTransaction)
/*      */       {
/*  952 */         transaction.status = TransactionStatus.COMPLETED;
/*  953 */         Firebase ref = new Firebase(this, transaction.path);
/*      */         
/*      */ 
/*  956 */         Node lastInput = transaction.currentInputSnapshot;
/*      */         
/*  958 */         final DataSnapshot snapshot = new DataSnapshot(ref, com.firebase.client.snapshot.IndexedNode.from(lastInput));
/*      */         
/*      */ 
/*      */ 
/*  962 */         scheduleNow(new Runnable()
/*      */         {
/*      */           public void run() {
/*  965 */             Repo.this.removeEventCallback(new ValueEventRegistration(Repo.this, transaction.outstandingListener, QuerySpec.defaultQueryAtPath(transaction.path)));
/*      */           }
/*      */           
/*      */ 
/*  969 */         });
/*  970 */         final FirebaseError callbackError = abortReason;
/*  971 */         callbacks.add(new Runnable()
/*      */         {
/*      */           public void run() {
/*  974 */             transaction.handler.onComplete(callbackError, false, snapshot);
/*      */           }
/*      */         });
/*      */       }
/*      */     }
/*      */     
/*      */ 
/*  981 */     pruneCompletedTransactions(this.transactionQueueTree);
/*      */     
/*      */ 
/*  984 */     for (int i = 0; i < callbacks.size(); i++) {
/*  985 */       postEvent((Runnable)callbacks.get(i));
/*      */     }
/*      */     
/*      */ 
/*  989 */     sendAllReadyTransactions();
/*      */   }
/*      */   
/*      */   private Tree<List<TransactionData>> getAncestorTransactionNode(Path path) {
/*  993 */     Tree<List<TransactionData>> transactionNode = this.transactionQueueTree;
/*  994 */     while ((!path.isEmpty()) && (transactionNode.getValue() == null)) {
/*  995 */       transactionNode = transactionNode.subTree(new Path(new ChildKey[] { path.getFront() }));
/*  996 */       path = path.popFront();
/*      */     }
/*      */     
/*  999 */     return transactionNode;
/*      */   }
/*      */   
/*      */   private List<TransactionData> buildTransactionQueue(Tree<List<TransactionData>> transactionNode) {
/* 1003 */     List<TransactionData> queue = new ArrayList();
/* 1004 */     aggregateTransactionQueues(queue, transactionNode);
/*      */     
/* 1006 */     java.util.Collections.sort(queue);
/*      */     
/* 1008 */     return queue;
/*      */   }
/*      */   
/*      */   private void aggregateTransactionQueues(final List<TransactionData> queue, Tree<List<TransactionData>> node) {
/* 1012 */     List<TransactionData> childQueue = (List)node.getValue();
/* 1013 */     if (childQueue != null) {
/* 1014 */       queue.addAll(childQueue);
/*      */     }
/*      */     
/* 1017 */     node.forEachChild(new com.firebase.client.core.utilities.Tree.TreeVisitor()
/*      */     {
/*      */       public void visitTree(Tree<List<Repo.TransactionData>> tree) {
/* 1020 */         Repo.this.aggregateTransactionQueues(queue, tree);
/*      */       }
/*      */     });
/*      */   }
/*      */   
/*      */   private Path abortTransactions(Path path, final int reason) {
/* 1026 */     Path affectedPath = getAncestorTransactionNode(path).getPath();
/*      */     
/* 1028 */     if (this.transactionLogger.logsDebug()) { this.operationLogger.debug("Aborting transactions for path: " + path + ". Affected: " + affectedPath);
/*      */     }
/* 1030 */     Tree<List<TransactionData>> transactionNode = this.transactionQueueTree.subTree(path);
/* 1031 */     transactionNode.forEachAncestor(new com.firebase.client.core.utilities.Tree.TreeFilter()
/*      */     {
/*      */       public boolean filterTreeNode(Tree<List<Repo.TransactionData>> tree) {
/* 1034 */         Repo.this.abortTransactionsAtNode(tree, reason);
/* 1035 */         return false;
/*      */       }
/*      */       
/* 1038 */     });
/* 1039 */     abortTransactionsAtNode(transactionNode, reason);
/*      */     
/* 1041 */     transactionNode.forEachDescendant(new com.firebase.client.core.utilities.Tree.TreeVisitor()
/*      */     {
/*      */       public void visitTree(Tree<List<Repo.TransactionData>> tree) {
/* 1044 */         Repo.this.abortTransactionsAtNode(tree, reason);
/*      */       }
/*      */       
/* 1047 */     });
/* 1048 */     return affectedPath;
/*      */   }
/*      */   
/*      */   private void abortTransactionsAtNode(Tree<List<TransactionData>> node, int reason) {
/* 1052 */     List<TransactionData> queue = (List)node.getValue();
/* 1053 */     List<Event> events = new ArrayList();
/*      */     
/* 1055 */     if (queue != null) {
/* 1056 */       List<Runnable> callbacks = new ArrayList();
/*      */       FirebaseError abortError;
/* 1058 */       final FirebaseError abortError; if (reason == -9) {
/* 1059 */         abortError = FirebaseError.fromStatus("overriddenBySet");
/*      */       } else {
/* 1061 */         com.firebase.client.utilities.Utilities.hardAssert(reason == -25, "Unknown transaction abort reason: " + reason);
/* 1062 */         abortError = FirebaseError.fromCode(-25);
/*      */       }
/*      */       
/* 1065 */       int lastSent = -1;
/* 1066 */       for (int i = 0; i < queue.size(); i++) {
/* 1067 */         final TransactionData transaction = (TransactionData)queue.get(i);
/* 1068 */         if (transaction.status != TransactionStatus.SENT_NEEDS_ABORT)
/*      */         {
/* 1070 */           if (transaction.status == TransactionStatus.SENT) {
/* 1071 */             assert (lastSent == i - 1);
/* 1072 */             lastSent = i;
/*      */             
/* 1074 */             transaction.status = TransactionStatus.SENT_NEEDS_ABORT;
/* 1075 */             transaction.abortReason = abortError;
/*      */           } else {
/* 1077 */             assert (transaction.status == TransactionStatus.RUN);
/*      */             
/* 1079 */             removeEventCallback(new ValueEventRegistration(this, transaction.outstandingListener, QuerySpec.defaultQueryAtPath(transaction.path)));
/*      */             
/* 1081 */             if (reason == -9) {
/* 1082 */               events.addAll(this.serverSyncTree.ackUserWrite(transaction.currentWriteId, true, false, this.serverClock));
/*      */             } else {
/* 1084 */               com.firebase.client.utilities.Utilities.hardAssert(reason == -25, "Unknown transaction abort reason: " + reason);
/*      */             }
/*      */             
/* 1087 */             callbacks.add(new Runnable()
/*      */             {
/*      */               public void run() {
/* 1090 */                 transaction.handler.onComplete(abortError, false, null);
/*      */               }
/*      */             });
/*      */           }
/*      */         }
/*      */       }
/* 1096 */       if (lastSent == -1)
/*      */       {
/* 1098 */         node.setValue(null);
/*      */       }
/*      */       else {
/* 1101 */         node.setValue(queue.subList(0, lastSent + 1));
/*      */       }
/*      */       
/*      */ 
/* 1105 */       postEvents(events);
/* 1106 */       for (Runnable r : callbacks) {
/* 1107 */         postEvent(r);
/*      */       }
/*      */     }
/*      */   }
/*      */   
/*      */   SyncTree getServerSyncTree()
/*      */   {
/* 1114 */     return this.serverSyncTree;
/*      */   }
/*      */   
/*      */   SyncTree getInfoSyncTree()
/*      */   {
/* 1119 */     return this.infoSyncTree;
/*      */   }
/*      */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/Repo.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */