/*     */ package com.firebase.client.core;
/*     */ 
/*     */ import com.firebase.client.FirebaseError;
/*     */ import com.firebase.client.annotations.NotNull;
/*     */ import com.firebase.client.core.operation.Merge;
/*     */ import com.firebase.client.core.operation.Operation;
/*     */ import com.firebase.client.core.operation.OperationSource;
/*     */ import com.firebase.client.core.operation.Overwrite;
/*     */ import com.firebase.client.core.persistence.PersistenceManager;
/*     */ import com.firebase.client.core.utilities.ImmutableTree;
/*     */ import com.firebase.client.core.view.CacheNode;
/*     */ import com.firebase.client.core.view.Event;
/*     */ import com.firebase.client.core.view.QuerySpec;
/*     */ import com.firebase.client.core.view.View;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.NamedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import com.firebase.client.utilities.Clock;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Map.Entry;
/*     */ import java.util.Set;
/*     */ import java.util.concurrent.Callable;
/*     */ 
/*     */ public class SyncTree
/*     */ {
/*     */   private static final long SIZE_THRESHOLD_FOR_COMPOUND_HASH = 1024L;
/*     */   private ImmutableTree<SyncPoint> syncPointTree;
/*     */   private final WriteTree pendingWriteTree;
/*     */   private final Map<Tag, QuerySpec> tagToQueryMap;
/*     */   private final Map<QuerySpec, Tag> queryToTagMap;
/*     */   private final Set<QuerySpec> keepSyncedQueries;
/*     */   private final ListenProvider listenProvider;
/*     */   private final PersistenceManager persistenceManager;
/*     */   private final com.firebase.client.utilities.LogWrapper logger;
/*     */   
/*     */   public static abstract interface SyncTreeHash
/*     */   {
/*     */     public abstract CompoundHash getCompoundHash();
/*     */     
/*     */     public abstract String getSimpleHash();
/*     */     
/*     */     public abstract boolean shouldIncludeCompoundHash();
/*     */   }
/*     */   
/*     */   public static abstract interface CompletionListener
/*     */   {
/*     */     public abstract List<? extends Event> onListenComplete(FirebaseError paramFirebaseError);
/*     */   }
/*     */   
/*     */   public static abstract interface ListenProvider
/*     */   {
/*     */     public abstract void startListening(QuerySpec paramQuerySpec, Tag paramTag, SyncTree.SyncTreeHash paramSyncTreeHash, SyncTree.CompletionListener paramCompletionListener);
/*     */     
/*     */     public abstract void stopListening(QuerySpec paramQuerySpec, Tag paramTag);
/*     */   }
/*     */   
/*     */   private class ListenContainer implements SyncTree.SyncTreeHash, SyncTree.CompletionListener
/*     */   {
/*     */     private final View view;
/*     */     private final Tag tag;
/*     */     
/*     */     public ListenContainer(View view)
/*     */     {
/*  67 */       this.view = view;
/*  68 */       this.tag = SyncTree.this.tagForQuery(view.getQuery());
/*     */     }
/*     */     
/*     */     public CompoundHash getCompoundHash()
/*     */     {
/*  73 */       return CompoundHash.fromNode(this.view.getServerCache());
/*     */     }
/*     */     
/*     */     public String getSimpleHash()
/*     */     {
/*  78 */       return this.view.getServerCache().getHash();
/*     */     }
/*     */     
/*     */     public boolean shouldIncludeCompoundHash()
/*     */     {
/*  83 */       return com.firebase.client.utilities.NodeSizeEstimator.estimateSerializedNodeSize(this.view.getServerCache()) > 1024L;
/*     */     }
/*     */     
/*     */     public List<? extends Event> onListenComplete(FirebaseError error)
/*     */     {
/*  88 */       if (error == null) {
/*  89 */         QuerySpec query = this.view.getQuery();
/*  90 */         if (this.tag != null) {
/*  91 */           return SyncTree.this.applyTaggedListenComplete(this.tag);
/*     */         }
/*  93 */         return SyncTree.this.applyListenComplete(query.getPath());
/*     */       }
/*     */       
/*  96 */       SyncTree.this.logger.warn("Listen at " + this.view.getQuery().getPath() + " failed: " + error.toString());
/*     */       
/*     */ 
/*     */ 
/* 100 */       return SyncTree.this.removeAllEventRegistrations(this.view.getQuery(), error);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public SyncTree(Context context, PersistenceManager persistenceManager, ListenProvider listenProvider)
/*     */   {
/* 122 */     this.syncPointTree = ImmutableTree.emptyInstance();
/* 123 */     this.pendingWriteTree = new WriteTree();
/* 124 */     this.tagToQueryMap = new java.util.HashMap();
/* 125 */     this.queryToTagMap = new java.util.HashMap();
/* 126 */     this.keepSyncedQueries = new java.util.HashSet();
/* 127 */     this.listenProvider = listenProvider;
/* 128 */     this.persistenceManager = persistenceManager;
/* 129 */     this.logger = context.getLogger("SyncTree");
/*     */   }
/*     */   
/*     */   public boolean isEmpty() {
/* 133 */     return this.syncPointTree.isEmpty();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<? extends Event> applyUserOverwrite(final Path path, final Node newDataUnresolved, Node newData, final long writeId, final boolean visible, final boolean persist)
/*     */   {
/* 141 */     com.firebase.client.utilities.Utilities.hardAssert((visible) || (!persist), "We shouldn't be persisting non-visible writes.");
/* 142 */     (List)this.persistenceManager.runInTransaction(new Callable() {
/*     */       public List<? extends Event> call() {
/* 144 */         if (persist) {
/* 145 */           SyncTree.this.persistenceManager.saveUserOverwrite(path, newDataUnresolved, writeId);
/*     */         }
/*     */         
/* 148 */         SyncTree.this.pendingWriteTree.addOverwrite(path, visible, Long.valueOf(writeId), this.val$visible);
/* 149 */         if (!this.val$visible) {
/* 150 */           return Collections.emptyList();
/*     */         }
/* 152 */         return SyncTree.this.applyOperationToSyncPoints(new Overwrite(OperationSource.USER, path, visible));
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<? extends Event> applyUserMerge(final Path path, final CompoundWrite unresolvedChildren, CompoundWrite children, final long writeId, final boolean persist)
/*     */   {
/* 163 */     (List)this.persistenceManager.runInTransaction(new Callable()
/*     */     {
/*     */       public List<? extends Event> call() throws Exception {
/* 166 */         if (persist) {
/* 167 */           SyncTree.this.persistenceManager.saveUserMerge(path, unresolvedChildren, writeId);
/*     */         }
/* 169 */         SyncTree.this.pendingWriteTree.addMerge(path, this.val$children, Long.valueOf(writeId));
/*     */         
/* 171 */         return SyncTree.this.applyOperationToSyncPoints(new Merge(OperationSource.USER, path, this.val$children));
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<? extends Event> ackUserWrite(final long writeId, boolean revert, final boolean persist, final Clock serverClock)
/*     */   {
/* 181 */     (List)this.persistenceManager.runInTransaction(new Callable() {
/*     */       public List<? extends Event> call() {
/* 183 */         if (persist) {
/* 184 */           SyncTree.this.persistenceManager.removeUserWrite(writeId);
/*     */         }
/* 186 */         UserWriteRecord write = SyncTree.this.pendingWriteTree.getWrite(writeId);
/* 187 */         boolean needToReevaluate = SyncTree.this.pendingWriteTree.removeWrite(writeId);
/* 188 */         if ((write.isVisible()) && 
/* 189 */           (!serverClock)) {
/* 190 */           Map<String, Object> serverValues = ServerValues.generateServerValues(this.val$serverClock);
/* 191 */           if (write.isOverwrite()) {
/* 192 */             Node resolvedNode = ServerValues.resolveDeferredValueSnapshot(write.getOverwrite(), serverValues);
/* 193 */             SyncTree.this.persistenceManager.applyUserWriteToServerCache(write.getPath(), resolvedNode);
/*     */           } else {
/* 195 */             CompoundWrite resolvedMerge = ServerValues.resolveDeferredValueMerge(write.getMerge(), serverValues);
/* 196 */             SyncTree.this.persistenceManager.applyUserWriteToServerCache(write.getPath(), resolvedMerge);
/*     */           }
/*     */         }
/*     */         
/* 200 */         if (!needToReevaluate) {
/* 201 */           return Collections.emptyList();
/*     */         }
/* 203 */         ImmutableTree<Boolean> affectedTree = ImmutableTree.emptyInstance();
/* 204 */         if (write.isOverwrite()) {
/* 205 */           affectedTree = affectedTree.set(Path.getEmptyPath(), Boolean.valueOf(true));
/*     */         } else {
/* 207 */           for (Map.Entry<Path, Node> entry : write.getMerge()) {
/* 208 */             affectedTree = affectedTree.set((Path)entry.getKey(), Boolean.valueOf(true));
/*     */           }
/*     */         }
/* 211 */         return SyncTree.this.applyOperationToSyncPoints(new com.firebase.client.core.operation.AckUserWrite(write.getPath(), affectedTree, serverClock));
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<? extends Event> removeAllWrites()
/*     */   {
/* 221 */     (List)this.persistenceManager.runInTransaction(new Callable()
/*     */     {
/*     */       public List<? extends Event> call() throws Exception {
/* 224 */         SyncTree.this.persistenceManager.removeAllUserWrites();
/* 225 */         List<UserWriteRecord> purgedWrites = SyncTree.this.pendingWriteTree.purgeAllWrites();
/* 226 */         if (purgedWrites.isEmpty()) {
/* 227 */           return Collections.emptyList();
/*     */         }
/* 229 */         ImmutableTree<Boolean> affectedTree = new ImmutableTree(Boolean.valueOf(true));
/* 230 */         return SyncTree.this.applyOperationToSyncPoints(new com.firebase.client.core.operation.AckUserWrite(Path.getEmptyPath(), affectedTree, true));
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<? extends Event> applyServerOverwrite(final Path path, final Node newData)
/*     */   {
/* 240 */     (List)this.persistenceManager.runInTransaction(new Callable() {
/*     */       public List<? extends Event> call() {
/* 242 */         SyncTree.this.persistenceManager.updateServerCache(QuerySpec.defaultQueryAtPath(path), newData);
/* 243 */         return SyncTree.this.applyOperationToSyncPoints(new Overwrite(OperationSource.SERVER, path, newData));
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public List<? extends Event> applyServerMerge(final Path path, final Map<Path, Node> changedChildren)
/*     */   {
/* 252 */     (List)this.persistenceManager.runInTransaction(new Callable() {
/*     */       public List<? extends Event> call() {
/* 254 */         CompoundWrite merge = CompoundWrite.fromPathMerge(changedChildren);
/* 255 */         SyncTree.this.persistenceManager.updateServerCache(path, merge);
/* 256 */         return SyncTree.this.applyOperationToSyncPoints(new Merge(OperationSource.SERVER, path, merge));
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public List<? extends Event> applyServerRangeMerges(Path path, List<RangeMerge> rangeMerges)
/*     */   {
/* 265 */     SyncPoint syncPoint = (SyncPoint)this.syncPointTree.get(path);
/* 266 */     if (syncPoint == null)
/*     */     {
/* 268 */       return Collections.emptyList();
/*     */     }
/*     */     
/*     */ 
/* 272 */     View view = syncPoint.getCompleteView();
/* 273 */     if (view != null) {
/* 274 */       Node serverNode = view.getServerCache();
/* 275 */       for (RangeMerge merge : rangeMerges) {
/* 276 */         serverNode = merge.applyTo(serverNode);
/*     */       }
/* 278 */       return applyServerOverwrite(path, serverNode);
/*     */     }
/*     */     
/*     */ 
/* 282 */     return Collections.emptyList();
/*     */   }
/*     */   
/*     */ 
/*     */   public List<? extends Event> applyTaggedRangeMerges(Path path, List<RangeMerge> rangeMerges, Tag tag)
/*     */   {
/* 288 */     QuerySpec query = queryForTag(tag);
/* 289 */     if (query != null) {
/* 290 */       assert (path.equals(query.getPath()));
/* 291 */       SyncPoint syncPoint = (SyncPoint)this.syncPointTree.get(query.getPath());
/* 292 */       assert (syncPoint != null) : "Missing sync point for query tag that we're tracking";
/* 293 */       View view = syncPoint.viewForQuery(query);
/* 294 */       assert (view != null) : "Missing view for query tag that we're tracking";
/* 295 */       Node serverNode = view.getServerCache();
/* 296 */       for (RangeMerge merge : rangeMerges) {
/* 297 */         serverNode = merge.applyTo(serverNode);
/*     */       }
/* 299 */       return applyTaggedQueryOverwrite(path, serverNode, tag);
/*     */     }
/*     */     
/* 302 */     return Collections.emptyList();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<? extends Event> applyListenComplete(final Path path)
/*     */   {
/* 311 */     (List)this.persistenceManager.runInTransaction(new Callable() {
/*     */       public List<? extends Event> call() {
/* 313 */         SyncTree.this.persistenceManager.setQueryComplete(QuerySpec.defaultQueryAtPath(path));
/* 314 */         return SyncTree.this.applyOperationToSyncPoints(new com.firebase.client.core.operation.ListenComplete(OperationSource.SERVER, path));
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public List<? extends Event> applyTaggedListenComplete(final Tag tag)
/*     */   {
/* 323 */     (List)this.persistenceManager.runInTransaction(new Callable() {
/*     */       public List<? extends Event> call() {
/* 325 */         QuerySpec query = SyncTree.this.queryForTag(tag);
/* 326 */         if (query != null) {
/* 327 */           SyncTree.this.persistenceManager.setQueryComplete(query);
/* 328 */           Operation op = new com.firebase.client.core.operation.ListenComplete(OperationSource.forServerTaggedQuery(query.getParams()), Path.getEmptyPath());
/* 329 */           return SyncTree.this.applyTaggedOperation(query, op);
/*     */         }
/*     */         
/* 332 */         return Collections.emptyList();
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private List<? extends Event> applyTaggedOperation(QuerySpec query, Operation operation)
/*     */   {
/* 339 */     Path queryPath = query.getPath();
/* 340 */     SyncPoint syncPoint = (SyncPoint)this.syncPointTree.get(queryPath);
/* 341 */     assert (syncPoint != null) : "Missing sync point for query tag that we're tracking";
/* 342 */     WriteTreeRef writesCache = this.pendingWriteTree.childWrites(queryPath);
/* 343 */     return syncPoint.applyOperation(operation, writesCache, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public List<? extends Event> applyTaggedQueryOverwrite(final Path path, final Node snap, final Tag tag)
/*     */   {
/* 350 */     (List)this.persistenceManager.runInTransaction(new Callable() {
/*     */       public List<? extends Event> call() {
/* 352 */         QuerySpec query = SyncTree.this.queryForTag(tag);
/* 353 */         if (query != null) {
/* 354 */           Path relativePath = Path.getRelative(query.getPath(), path);
/* 355 */           QuerySpec queryToOverwrite = relativePath.isEmpty() ? query : QuerySpec.defaultQueryAtPath(path);
/* 356 */           SyncTree.this.persistenceManager.updateServerCache(queryToOverwrite, snap);
/* 357 */           Operation op = new Overwrite(OperationSource.forServerTaggedQuery(query.getParams()), relativePath, snap);
/* 358 */           return SyncTree.this.applyTaggedOperation(query, op);
/*     */         }
/*     */         
/* 361 */         return Collections.emptyList();
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<? extends Event> applyTaggedQueryMerge(final Path path, final Map<Path, Node> changedChildren, final Tag tag)
/*     */   {
/* 371 */     (List)this.persistenceManager.runInTransaction(new Callable() {
/*     */       public List<? extends Event> call() {
/* 373 */         QuerySpec query = SyncTree.this.queryForTag(tag);
/* 374 */         if (query != null) {
/* 375 */           Path relativePath = Path.getRelative(query.getPath(), path);
/* 376 */           CompoundWrite merge = CompoundWrite.fromPathMerge(changedChildren);
/* 377 */           SyncTree.this.persistenceManager.updateServerCache(path, merge);
/* 378 */           Operation op = new Merge(OperationSource.forServerTaggedQuery(query.getParams()), relativePath, merge);
/* 379 */           return SyncTree.this.applyTaggedOperation(query, op);
/*     */         }
/*     */         
/* 382 */         return Collections.emptyList();
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<? extends Event> addEventRegistration(@NotNull final EventRegistration eventRegistration)
/*     */   {
/* 392 */     (List)this.persistenceManager.runInTransaction(new Callable() {
/*     */       public List<? extends Event> call() {
/* 394 */         QuerySpec query = eventRegistration.getQuerySpec();
/* 395 */         Path path = query.getPath();
/*     */         
/* 397 */         Node serverCacheNode = null;
/* 398 */         boolean foundAncestorDefaultView = false;
/*     */         
/*     */ 
/*     */ 
/*     */ 
/* 403 */         ImmutableTree<SyncPoint> tree = SyncTree.this.syncPointTree;
/* 404 */         Path currentPath = path;
/* 405 */         while (!tree.isEmpty()) {
/* 406 */           SyncPoint currentSyncPoint = (SyncPoint)tree.getValue();
/* 407 */           if (currentSyncPoint != null) {
/* 408 */             serverCacheNode = serverCacheNode != null ? serverCacheNode : currentSyncPoint.getCompleteServerCache(currentPath);
/*     */             
/* 410 */             foundAncestorDefaultView = (foundAncestorDefaultView) || (currentSyncPoint.hasCompleteView());
/*     */           }
/* 412 */           ChildKey front = currentPath.isEmpty() ? ChildKey.fromString("") : currentPath.getFront();
/* 413 */           tree = tree.getChild(front);
/* 414 */           currentPath = currentPath.popFront();
/*     */         }
/*     */         
/*     */ 
/* 418 */         SyncPoint syncPoint = (SyncPoint)SyncTree.this.syncPointTree.get(path);
/* 419 */         if (syncPoint == null) {
/* 420 */           syncPoint = new SyncPoint(SyncTree.this.persistenceManager);
/* 421 */           SyncTree.this.syncPointTree = SyncTree.this.syncPointTree.set(path, syncPoint);
/*     */         } else {
/* 423 */           foundAncestorDefaultView = (foundAncestorDefaultView) || (syncPoint.hasCompleteView());
/* 424 */           serverCacheNode = serverCacheNode != null ? serverCacheNode : syncPoint.getCompleteServerCache(Path.getEmptyPath());
/*     */         }
/*     */         
/* 427 */         SyncTree.this.persistenceManager.setQueryActive(query);
/*     */         CacheNode serverCache;
/*     */         CacheNode serverCache;
/* 430 */         if (serverCacheNode != null) {
/* 431 */           serverCache = new CacheNode(com.firebase.client.snapshot.IndexedNode.from(serverCacheNode, query.getIndex()), true, false);
/*     */         }
/*     */         else {
/* 434 */           CacheNode persistentServerCache = SyncTree.this.persistenceManager.serverCache(query);
/* 435 */           CacheNode serverCache; if (persistentServerCache.isFullyInitialized()) {
/* 436 */             serverCache = persistentServerCache;
/*     */           } else {
/* 438 */             serverCacheNode = com.firebase.client.snapshot.EmptyNode.Empty();
/* 439 */             ImmutableTree<SyncPoint> subtree = SyncTree.this.syncPointTree.subtree(path);
/* 440 */             for (Map.Entry<ChildKey, ImmutableTree<SyncPoint>> child : subtree.getChildren()) {
/* 441 */               SyncPoint childSyncPoint = (SyncPoint)((ImmutableTree)child.getValue()).getValue();
/* 442 */               if (childSyncPoint != null) {
/* 443 */                 Node completeCache = childSyncPoint.getCompleteServerCache(Path.getEmptyPath());
/* 444 */                 if (completeCache != null) {
/* 445 */                   serverCacheNode = serverCacheNode.updateImmediateChild((ChildKey)child.getKey(), completeCache);
/*     */                 }
/*     */               }
/*     */             }
/*     */             
/* 450 */             for (NamedNode child : persistentServerCache.getNode()) {
/* 451 */               if (!serverCacheNode.hasChild(child.getName())) {
/* 452 */                 serverCacheNode = serverCacheNode.updateImmediateChild(child.getName(), child.getNode());
/*     */               }
/*     */             }
/* 455 */             serverCache = new CacheNode(com.firebase.client.snapshot.IndexedNode.from(serverCacheNode, query.getIndex()), false, false);
/*     */           }
/*     */         }
/*     */         
/* 459 */         boolean viewAlreadyExists = syncPoint.viewExistsForQuery(query);
/* 460 */         if ((!viewAlreadyExists) && (!query.loadsAllData()))
/*     */         {
/* 462 */           assert (!SyncTree.this.queryToTagMap.containsKey(query)) : "View does not exist but we have a tag";
/* 463 */           Tag tag = SyncTree.this.getNextQueryTag();
/* 464 */           SyncTree.this.queryToTagMap.put(query, tag);
/* 465 */           SyncTree.this.tagToQueryMap.put(tag, query);
/*     */         }
/* 467 */         WriteTreeRef writesCache = SyncTree.this.pendingWriteTree.childWrites(path);
/* 468 */         List<? extends Event> events = syncPoint.addEventRegistration(eventRegistration, writesCache, serverCache);
/* 469 */         if ((!viewAlreadyExists) && (!foundAncestorDefaultView)) {
/* 470 */           View view = syncPoint.viewForQuery(query);
/* 471 */           SyncTree.this.setupListener(query, view);
/*     */         }
/* 473 */         return events;
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<Event> removeEventRegistration(@NotNull EventRegistration eventRegistration)
/*     */   {
/* 484 */     return removeEventRegistration(eventRegistration.getQuerySpec(), eventRegistration, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<Event> removeAllEventRegistrations(@NotNull QuerySpec query, @NotNull FirebaseError error)
/*     */   {
/* 493 */     return removeEventRegistration(query, null, error);
/*     */   }
/*     */   
/*     */ 
/*     */   private List<Event> removeEventRegistration(@NotNull final QuerySpec query, @com.firebase.client.annotations.Nullable final EventRegistration eventRegistration, @com.firebase.client.annotations.Nullable final FirebaseError cancelError)
/*     */   {
/* 499 */     (List)this.persistenceManager.runInTransaction(new Callable()
/*     */     {
/*     */       public List<Event> call() {
/* 502 */         Path path = query.getPath();
/* 503 */         SyncPoint maybeSyncPoint = (SyncPoint)SyncTree.this.syncPointTree.get(path);
/* 504 */         List<Event> cancelEvents = new ArrayList();
/*     */         
/*     */ 
/*     */ 
/* 508 */         if ((maybeSyncPoint != null) && ((query.isDefault()) || (maybeSyncPoint.viewExistsForQuery(query))))
/*     */         {
/*     */ 
/* 511 */           com.firebase.client.utilities.Pair<List<QuerySpec>, List<Event>> removedAndEvents = maybeSyncPoint.removeEventRegistration(query, eventRegistration, cancelError);
/*     */           
/* 513 */           if (maybeSyncPoint.isEmpty()) {
/* 514 */             SyncTree.this.syncPointTree = SyncTree.this.syncPointTree.remove(path);
/*     */           }
/* 516 */           List<QuerySpec> removed = (List)removedAndEvents.getFirst();
/* 517 */           cancelEvents = (List)removedAndEvents.getSecond();
/*     */           
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 524 */           boolean removingDefault = false;
/* 525 */           for (QuerySpec queryRemoved : removed) {
/* 526 */             SyncTree.this.persistenceManager.setQueryInactive(query);
/* 527 */             removingDefault = (removingDefault) || (queryRemoved.loadsAllData());
/*     */           }
/* 529 */           ImmutableTree<SyncPoint> currentTree = SyncTree.this.syncPointTree;
/* 530 */           boolean covered = (currentTree.getValue() != null) && (((SyncPoint)currentTree.getValue()).hasCompleteView());
/* 531 */           for (ChildKey component : path) {
/* 532 */             currentTree = currentTree.getChild(component);
/* 533 */             covered = (covered) || ((currentTree.getValue() != null) && (((SyncPoint)currentTree.getValue()).hasCompleteView()));
/* 534 */             if ((covered) || (currentTree.isEmpty())) {
/*     */               break;
/*     */             }
/*     */           }
/*     */           
/* 539 */           if ((removingDefault) && (!covered)) {
/* 540 */             ImmutableTree<SyncPoint> subtree = SyncTree.this.syncPointTree.subtree(path);
/*     */             
/*     */ 
/* 543 */             if (!subtree.isEmpty())
/*     */             {
/* 545 */               List<View> newViews = SyncTree.this.collectDistinctViewsForSubTree(subtree);
/*     */               
/*     */ 
/* 548 */               for (View view : newViews) {
/* 549 */                 SyncTree.ListenContainer container = new SyncTree.ListenContainer(SyncTree.this, view);
/* 550 */                 QuerySpec newQuery = view.getQuery();
/* 551 */                 SyncTree.this.listenProvider.startListening(SyncTree.this.queryForListening(newQuery), container.tag, container, container);
/*     */               }
/*     */             }
/*     */           }
/*     */           
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 560 */           if ((!covered) && (!removed.isEmpty()) && (cancelError == null))
/*     */           {
/*     */ 
/* 563 */             if (removingDefault) {
/* 564 */               SyncTree.this.listenProvider.stopListening(SyncTree.this.queryForListening(query), null);
/*     */             } else {
/* 566 */               for (QuerySpec queryToRemove : removed) {
/* 567 */                 Tag tag = SyncTree.this.tagForQuery(queryToRemove);
/* 568 */                 assert (tag != null);
/* 569 */                 SyncTree.this.listenProvider.stopListening(SyncTree.this.queryForListening(queryToRemove), tag);
/*     */               }
/*     */             }
/*     */           }
/*     */           
/* 574 */           SyncTree.this.removeTags(removed);
/*     */         }
/*     */         
/*     */ 
/* 578 */         return cancelEvents;
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private static class KeepSyncedEventRegistration extends EventRegistration {
/*     */     private QuerySpec spec;
/*     */     
/* 586 */     public KeepSyncedEventRegistration(@NotNull QuerySpec spec) { this.spec = spec; }
/*     */     
/* 588 */     public boolean respondsTo(com.firebase.client.core.view.Event.EventType eventType) { return false; }
/* 589 */     public com.firebase.client.core.view.DataEvent createEvent(com.firebase.client.core.view.Change change, QuerySpec query) { return null; }
/*     */     
/*     */     public void fireEvent(com.firebase.client.core.view.DataEvent dataEvent) {}
/*     */     
/*     */     public void fireCancelEvent(FirebaseError error) {}
/* 594 */     public EventRegistration clone(QuerySpec newQuery) { return new KeepSyncedEventRegistration(newQuery); }
/*     */     
/* 596 */     public boolean isSameListener(EventRegistration other) { return other instanceof KeepSyncedEventRegistration; } @NotNull
/* 597 */     public QuerySpec getQuerySpec() { return this.spec; }
/*     */     
/* 599 */     public boolean equals(Object other) { return ((other instanceof KeepSyncedEventRegistration)) && (((KeepSyncedEventRegistration)other).spec.equals(this.spec)); }
/*     */     
/*     */     public int hashCode()
/*     */     {
/* 603 */       return this.spec.hashCode();
/*     */     }
/*     */   }
/*     */   
/*     */   public void keepSynced(QuerySpec query, boolean keep) {
/* 608 */     if ((keep) && (!this.keepSyncedQueries.contains(query)))
/*     */     {
/* 610 */       addEventRegistration(new KeepSyncedEventRegistration(query));
/* 611 */       this.keepSyncedQueries.add(query);
/* 612 */     } else if ((!keep) && (this.keepSyncedQueries.contains(query))) {
/* 613 */       removeEventRegistration(new KeepSyncedEventRegistration(query));
/* 614 */       this.keepSyncedQueries.remove(query);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private List<View> collectDistinctViewsForSubTree(ImmutableTree<SyncPoint> subtree)
/*     */   {
/* 623 */     ArrayList<View> accumulator = new ArrayList();
/* 624 */     collectDistinctViewsForSubTree(subtree, accumulator);
/* 625 */     return accumulator;
/*     */   }
/*     */   
/*     */   private void collectDistinctViewsForSubTree(ImmutableTree<SyncPoint> subtree, List<View> accumulator) {
/* 629 */     SyncPoint maybeSyncPoint = (SyncPoint)subtree.getValue();
/* 630 */     if ((maybeSyncPoint != null) && (maybeSyncPoint.hasCompleteView())) {
/* 631 */       accumulator.add(maybeSyncPoint.getCompleteView());
/*     */     } else {
/* 633 */       if (maybeSyncPoint != null) {
/* 634 */         accumulator.addAll(maybeSyncPoint.getQueryViews());
/*     */       }
/* 636 */       for (Map.Entry<ChildKey, ImmutableTree<SyncPoint>> entry : subtree.getChildren()) {
/* 637 */         collectDistinctViewsForSubTree((ImmutableTree)entry.getValue(), accumulator);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void removeTags(List<QuerySpec> queries)
/*     */   {
/* 644 */     for (QuerySpec removedQuery : queries) {
/* 645 */       if (!removedQuery.loadsAllData())
/*     */       {
/* 647 */         Tag tag = tagForQuery(removedQuery);
/* 648 */         assert (tag != null);
/* 649 */         this.queryToTagMap.remove(removedQuery);
/* 650 */         this.tagToQueryMap.remove(tag);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private QuerySpec queryForListening(QuerySpec query) {
/* 656 */     if ((query.loadsAllData()) && (!query.isDefault()))
/*     */     {
/* 658 */       return QuerySpec.defaultQueryAtPath(query.getPath());
/*     */     }
/* 660 */     return query;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private void setupListener(QuerySpec query, View view)
/*     */   {
/* 668 */     Path path = query.getPath();
/* 669 */     Tag tag = tagForQuery(query);
/* 670 */     ListenContainer container = new ListenContainer(view);
/*     */     
/* 672 */     this.listenProvider.startListening(queryForListening(query), tag, container, container);
/*     */     
/* 674 */     ImmutableTree<SyncPoint> subtree = this.syncPointTree.subtree(path);
/*     */     
/*     */ 
/* 677 */     if (tag != null) {
/* 678 */       if ((!$assertionsDisabled) && (((SyncPoint)subtree.getValue()).hasCompleteView())) throw new AssertionError("If we're adding a query, it shouldn't be shadowed");
/*     */     }
/*     */     else {
/* 681 */       subtree.foreach(new com.firebase.client.core.utilities.ImmutableTree.TreeVisitor()
/*     */       {
/*     */         public Void onNodeValue(Path relativePath, SyncPoint maybeChildSyncPoint, Void accum) {
/* 684 */           if ((!relativePath.isEmpty()) && (maybeChildSyncPoint.hasCompleteView())) {
/* 685 */             QuerySpec query = maybeChildSyncPoint.getCompleteView().getQuery();
/* 686 */             SyncTree.this.listenProvider.stopListening(SyncTree.this.queryForListening(query), SyncTree.this.tagForQuery(query));
/*     */           }
/*     */           else {
/* 689 */             for (View syncPointView : maybeChildSyncPoint.getQueryViews()) {
/* 690 */               QuerySpec childQuery = syncPointView.getQuery();
/* 691 */               SyncTree.this.listenProvider.stopListening(SyncTree.this.queryForListening(childQuery), SyncTree.this.tagForQuery(childQuery));
/*     */             }
/*     */           }
/* 694 */           return null;
/*     */         }
/*     */       });
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private QuerySpec queryForTag(Tag tag)
/*     */   {
/* 704 */     return (QuerySpec)this.tagToQueryMap.get(tag);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private Tag tagForQuery(QuerySpec query)
/*     */   {
/* 711 */     return (Tag)this.queryToTagMap.get(query);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Node calcCompleteEventCache(Path path, List<Long> writeIdsToExclude)
/*     */   {
/* 721 */     ImmutableTree<SyncPoint> tree = this.syncPointTree;
/* 722 */     SyncPoint currentSyncPoint = (SyncPoint)tree.getValue();
/* 723 */     Node serverCache = null;
/* 724 */     Path pathToFollow = path;
/* 725 */     Path pathSoFar = Path.getEmptyPath();
/*     */     do {
/* 727 */       ChildKey front = pathToFollow.getFront();
/* 728 */       pathToFollow = pathToFollow.popFront();
/* 729 */       pathSoFar = pathSoFar.child(front);
/* 730 */       Path relativePath = Path.getRelative(pathSoFar, path);
/* 731 */       tree = front != null ? tree.getChild(front) : ImmutableTree.emptyInstance();
/* 732 */       currentSyncPoint = (SyncPoint)tree.getValue();
/* 733 */       if (currentSyncPoint != null) {
/* 734 */         serverCache = currentSyncPoint.getCompleteServerCache(relativePath);
/*     */       }
/* 736 */     } while ((!pathToFollow.isEmpty()) && (serverCache == null));
/* 737 */     return this.pendingWriteTree.calcCompleteEventCache(path, serverCache, writeIdsToExclude, true);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/* 743 */   private long nextQueryTag = 1L;
/*     */   
/*     */ 
/*     */ 
/*     */   private Tag getNextQueryTag()
/*     */   {
/* 749 */     return new Tag(this.nextQueryTag++);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private List<Event> applyOperationToSyncPoints(Operation operation)
/*     */   {
/* 766 */     return applyOperationHelper(operation, this.syncPointTree, null, this.pendingWriteTree.childWrites(Path.getEmptyPath()));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private List<Event> applyOperationHelper(Operation operation, ImmutableTree<SyncPoint> syncPointTree, Node serverCache, WriteTreeRef writesCache)
/*     */   {
/* 775 */     if (operation.getPath().isEmpty()) {
/* 776 */       return applyOperationDescendantsHelper(operation, syncPointTree, serverCache, writesCache);
/*     */     }
/* 778 */     SyncPoint syncPoint = (SyncPoint)syncPointTree.getValue();
/*     */     
/*     */ 
/* 781 */     if ((serverCache == null) && (syncPoint != null)) {
/* 782 */       serverCache = syncPoint.getCompleteServerCache(Path.getEmptyPath());
/*     */     }
/*     */     
/* 785 */     List<Event> events = new ArrayList();
/* 786 */     ChildKey childKey = operation.getPath().getFront();
/* 787 */     Operation childOperation = operation.operationForChild(childKey);
/* 788 */     ImmutableTree<SyncPoint> childTree = (ImmutableTree)syncPointTree.getChildren().get(childKey);
/* 789 */     if ((childTree != null) && (childOperation != null)) {
/* 790 */       Node childServerCache = serverCache != null ? serverCache.getImmediateChild(childKey) : null;
/* 791 */       WriteTreeRef childWritesCache = writesCache.child(childKey);
/* 792 */       events.addAll(applyOperationHelper(childOperation, childTree, childServerCache, childWritesCache));
/*     */     }
/*     */     
/* 795 */     if (syncPoint != null) {
/* 796 */       events.addAll(syncPoint.applyOperation(operation, writesCache, serverCache));
/*     */     }
/*     */     
/* 799 */     return events;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private List<Event> applyOperationDescendantsHelper(final Operation operation, ImmutableTree<SyncPoint> syncPointTree, Node serverCache, final WriteTreeRef writesCache)
/*     */   {
/* 808 */     SyncPoint syncPoint = (SyncPoint)syncPointTree.getValue();
/*     */     
/*     */     Node resolvedServerCache;
/*     */     final Node resolvedServerCache;
/* 812 */     if ((serverCache == null) && (syncPoint != null)) {
/* 813 */       resolvedServerCache = syncPoint.getCompleteServerCache(Path.getEmptyPath());
/*     */     } else {
/* 815 */       resolvedServerCache = serverCache;
/*     */     }
/*     */     
/* 818 */     final List<Event> events = new ArrayList();
/* 819 */     syncPointTree.getChildren().inOrderTraversal(new com.firebase.client.collection.LLRBNode.NodeVisitor()
/*     */     {
/*     */       public void visitEntry(ChildKey key, ImmutableTree<SyncPoint> childTree) {
/* 822 */         Node childServerCache = null;
/* 823 */         if (resolvedServerCache != null) {
/* 824 */           childServerCache = resolvedServerCache.getImmediateChild(key);
/*     */         }
/* 826 */         WriteTreeRef childWritesCache = writesCache.child(key);
/* 827 */         Operation childOperation = operation.operationForChild(key);
/* 828 */         if (childOperation != null) {
/* 829 */           events.addAll(SyncTree.this.applyOperationDescendantsHelper(childOperation, childTree, childServerCache, childWritesCache));
/*     */         }
/*     */       }
/*     */     });
/*     */     
/* 834 */     if (syncPoint != null) {
/* 835 */       events.addAll(syncPoint.applyOperation(operation, writesCache, resolvedServerCache));
/*     */     }
/*     */     
/* 838 */     return events;
/*     */   }
/*     */   
/*     */   ImmutableTree<SyncPoint> getSyncPointTree()
/*     */   {
/* 843 */     return this.syncPointTree;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/SyncTree.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */