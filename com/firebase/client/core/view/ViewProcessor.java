/*     */ package com.firebase.client.core.view;
/*     */ 
/*     */ import com.firebase.client.core.CompoundWrite;
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.core.WriteTreeRef;
/*     */ import com.firebase.client.core.operation.Merge;
/*     */ import com.firebase.client.core.operation.Overwrite;
/*     */ import com.firebase.client.core.view.filter.ChildChangeAccumulator;
/*     */ import com.firebase.client.core.view.filter.NodeFilter;
/*     */ import com.firebase.client.core.view.filter.NodeFilter.CompleteChildSource;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.IndexedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import java.util.Map.Entry;
/*     */ 
/*     */ public class ViewProcessor
/*     */ {
/*     */   private final NodeFilter filter;
/*     */   
/*     */   public ViewProcessor(NodeFilter filter)
/*     */   {
/*  22 */     this.filter = filter;
/*     */   }
/*     */   
/*     */   public static class ProcessorResult {
/*     */     public final ViewCache viewCache;
/*     */     public final java.util.List<Change> changes;
/*     */     
/*     */     public ProcessorResult(ViewCache viewCache, java.util.List<Change> changes) {
/*  30 */       this.viewCache = viewCache;
/*  31 */       this.changes = changes;
/*     */     }
/*     */   }
/*     */   
/*     */   public ProcessorResult applyOperation(ViewCache oldViewCache, com.firebase.client.core.operation.Operation operation, WriteTreeRef writesCache, Node optCompleteCache) {
/*  36 */     ChildChangeAccumulator accumulator = new ChildChangeAccumulator();
/*     */     ViewCache newViewCache;
/*  38 */     ViewCache newViewCache; ViewCache newViewCache; switch (operation.getType()) {
/*     */     case Overwrite: 
/*  40 */       Overwrite overwrite = (Overwrite)operation;
/*  41 */       ViewCache newViewCache; if (overwrite.getSource().isFromUser()) {
/*  42 */         newViewCache = applyUserOverwrite(oldViewCache, overwrite.getPath(), overwrite.getSnapshot(), writesCache, optCompleteCache, accumulator);
/*     */       } else {
/*  44 */         assert (overwrite.getSource().isFromServer());
/*     */         
/*     */ 
/*     */ 
/*  48 */         boolean filterServerNode = (overwrite.getSource().isTagged()) || ((oldViewCache.getServerCache().isFiltered()) && (!overwrite.getPath().isEmpty()));
/*     */         
/*  50 */         newViewCache = applyServerOverwrite(oldViewCache, overwrite.getPath(), overwrite.getSnapshot(), writesCache, optCompleteCache, filterServerNode, accumulator);
/*     */       }
/*     */       
/*  53 */       break;
/*     */     
/*     */     case Merge: 
/*  56 */       Merge merge = (Merge)operation;
/*  57 */       if (merge.getSource().isFromUser()) {
/*  58 */         newViewCache = applyUserMerge(oldViewCache, merge.getPath(), merge.getChildren(), writesCache, optCompleteCache, accumulator);
/*     */       } else {
/*  60 */         assert (merge.getSource().isFromServer());
/*     */         
/*  62 */         boolean filterServerNode = (merge.getSource().isTagged()) || (oldViewCache.getServerCache().isFiltered());
/*     */         
/*  64 */         newViewCache = applyServerMerge(oldViewCache, merge.getPath(), merge.getChildren(), writesCache, optCompleteCache, filterServerNode, accumulator);
/*     */       }
/*     */       
/*  67 */       break;
/*     */     
/*     */     case AckUserWrite: 
/*  70 */       com.firebase.client.core.operation.AckUserWrite ackUserWrite = (com.firebase.client.core.operation.AckUserWrite)operation;
/*  71 */       if (!ackUserWrite.isRevert()) {
/*  72 */         newViewCache = ackUserWrite(oldViewCache, ackUserWrite.getPath(), ackUserWrite.getAffectedTree(), writesCache, optCompleteCache, accumulator);
/*     */       }
/*     */       else {
/*  75 */         newViewCache = revertUserWrite(oldViewCache, ackUserWrite.getPath(), writesCache, optCompleteCache, accumulator);
/*     */       }
/*  77 */       break;
/*     */     
/*     */     case ListenComplete: 
/*  80 */       newViewCache = listenComplete(oldViewCache, operation.getPath(), writesCache, optCompleteCache, accumulator);
/*  81 */       break;
/*     */     
/*     */     default: 
/*  84 */       throw new AssertionError("Unknown operation: " + operation.getType());
/*     */     }
/*     */     
/*  87 */     java.util.List<Change> changes = new java.util.ArrayList(accumulator.getChanges());
/*  88 */     maybeAddValueEvent(oldViewCache, newViewCache, changes);
/*  89 */     return new ProcessorResult(newViewCache, changes);
/*     */   }
/*     */   
/*     */   private void maybeAddValueEvent(ViewCache oldViewCache, ViewCache newViewCache, java.util.List<Change> accumulator) {
/*  93 */     CacheNode eventSnap = newViewCache.getEventCache();
/*  94 */     if (eventSnap.isFullyInitialized()) {
/*  95 */       boolean isLeafOrEmpty = (eventSnap.getNode().isLeafNode()) || (eventSnap.getNode().isEmpty());
/*  96 */       if ((!accumulator.isEmpty()) || (!oldViewCache.getEventCache().isFullyInitialized()) || ((isLeafOrEmpty) && (!eventSnap.getNode().equals(oldViewCache.getCompleteEventSnap()))) || (!eventSnap.getNode().getPriority().equals(oldViewCache.getCompleteEventSnap().getPriority())))
/*     */       {
/*     */ 
/*     */ 
/* 100 */         accumulator.add(Change.valueChange(eventSnap.getIndexedNode()));
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private ViewCache generateEventCacheAfterServerEvent(ViewCache viewCache, Path changePath, WriteTreeRef writesCache, NodeFilter.CompleteChildSource source, ChildChangeAccumulator accumulator) {
/* 106 */     CacheNode oldEventSnap = viewCache.getEventCache();
/* 107 */     if (writesCache.shadowingWrite(changePath) != null)
/*     */     {
/* 109 */       return viewCache; }
/*     */     IndexedNode newEventCache;
/*     */     IndexedNode newEventCache;
/* 112 */     if (changePath.isEmpty())
/*     */     {
/* 114 */       assert (viewCache.getServerCache().isFullyInitialized()) : "If change path is empty, we must have complete server data";
/*     */       Node nodeWithLocalWrites;
/* 116 */       Node nodeWithLocalWrites; if (viewCache.getServerCache().isFiltered())
/*     */       {
/*     */ 
/*     */ 
/* 120 */         Node serverCache = viewCache.getCompleteServerSnap();
/* 121 */         Node completeChildren = (serverCache instanceof com.firebase.client.snapshot.ChildrenNode) ? serverCache : com.firebase.client.snapshot.EmptyNode.Empty();
/* 122 */         nodeWithLocalWrites = writesCache.calcCompleteEventChildren(completeChildren);
/*     */       } else {
/* 124 */         nodeWithLocalWrites = writesCache.calcCompleteEventCache(viewCache.getCompleteServerSnap());
/*     */       }
/* 126 */       IndexedNode indexedNode = IndexedNode.from(nodeWithLocalWrites, this.filter.getIndex());
/* 127 */       newEventCache = this.filter.updateFullNode(viewCache.getEventCache().getIndexedNode(), indexedNode, accumulator);
/*     */     } else {
/* 129 */       ChildKey childKey = changePath.getFront();
/* 130 */       IndexedNode newEventCache; if (childKey.isPriorityChildName()) {
/* 131 */         assert (changePath.size() == 1) : "Can't have a priority with additional path components";
/* 132 */         Node oldEventNode = oldEventSnap.getNode();
/* 133 */         Node serverNode = viewCache.getServerCache().getNode();
/*     */         
/* 135 */         Node updatedPriority = writesCache.calcEventCacheAfterServerOverwrite(changePath, oldEventNode, serverNode);
/* 136 */         IndexedNode newEventCache; if (updatedPriority != null) {
/* 137 */           newEventCache = this.filter.updatePriority(oldEventSnap.getIndexedNode(), updatedPriority);
/*     */         }
/*     */         else {
/* 140 */           newEventCache = oldEventSnap.getIndexedNode();
/*     */         }
/*     */       } else {
/* 143 */         Path childChangePath = changePath.popFront();
/*     */         Node newEventChild;
/*     */         Node newEventChild;
/* 146 */         if (oldEventSnap.isCompleteForChild(childKey)) {
/* 147 */           Node serverNode = viewCache.getServerCache().getNode();
/* 148 */           Node eventChildUpdate = writesCache.calcEventCacheAfterServerOverwrite(changePath, oldEventSnap.getNode(), serverNode);
/* 149 */           Node newEventChild; if (eventChildUpdate != null) {
/* 150 */             newEventChild = oldEventSnap.getNode().getImmediateChild(childKey).updateChild(childChangePath, eventChildUpdate);
/*     */           }
/*     */           else {
/* 153 */             newEventChild = oldEventSnap.getNode().getImmediateChild(childKey);
/*     */           }
/*     */         } else {
/* 156 */           newEventChild = writesCache.calcCompleteChild(childKey, viewCache.getServerCache()); }
/*     */         IndexedNode newEventCache;
/* 158 */         if (newEventChild != null) {
/* 159 */           newEventCache = this.filter.updateChild(oldEventSnap.getIndexedNode(), childKey, newEventChild, childChangePath, source, accumulator);
/*     */         }
/*     */         else
/*     */         {
/* 163 */           newEventCache = oldEventSnap.getIndexedNode();
/*     */         }
/*     */       }
/*     */     }
/* 167 */     return viewCache.updateEventSnap(newEventCache, (oldEventSnap.isFullyInitialized()) || (changePath.isEmpty()), this.filter.filtersNodes());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private ViewCache applyServerOverwrite(ViewCache oldViewCache, Path changePath, Node changedSnap, WriteTreeRef writesCache, Node optCompleteCache, boolean filterServerNode, ChildChangeAccumulator accumulator)
/*     */   {
/* 174 */     CacheNode oldServerSnap = oldViewCache.getServerCache();
/*     */     
/* 176 */     NodeFilter serverFilter = filterServerNode ? this.filter : this.filter.getIndexedFilter();
/* 177 */     IndexedNode newServerCache; IndexedNode newServerCache; if (changePath.isEmpty()) {
/* 178 */       newServerCache = serverFilter.updateFullNode(oldServerSnap.getIndexedNode(), IndexedNode.from(changedSnap, serverFilter.getIndex()), null); } else { IndexedNode newServerCache;
/* 179 */       if ((serverFilter.filtersNodes()) && (!oldServerSnap.isFiltered()))
/*     */       {
/* 181 */         assert (!changePath.isEmpty()) : "An empty path should have been caught in the other branch";
/* 182 */         ChildKey childKey = changePath.getFront();
/* 183 */         Path updatePath = changePath.popFront();
/* 184 */         Node newChild = oldServerSnap.getNode().getImmediateChild(childKey).updateChild(updatePath, changedSnap);
/* 185 */         IndexedNode newServerNode = oldServerSnap.getIndexedNode().updateChild(childKey, newChild);
/* 186 */         newServerCache = serverFilter.updateFullNode(oldServerSnap.getIndexedNode(), newServerNode, null);
/*     */       } else {
/* 188 */         ChildKey childKey = changePath.getFront();
/* 189 */         if ((!oldServerSnap.isCompleteForPath(changePath)) && (changePath.size() > 1))
/*     */         {
/* 191 */           return oldViewCache;
/*     */         }
/* 193 */         Path childChangePath = changePath.popFront();
/* 194 */         Node childNode = oldServerSnap.getNode().getImmediateChild(childKey);
/* 195 */         Node newChildNode = childNode.updateChild(childChangePath, changedSnap);
/* 196 */         IndexedNode newServerCache; if (childKey.isPriorityChildName()) {
/* 197 */           newServerCache = serverFilter.updatePriority(oldServerSnap.getIndexedNode(), newChildNode);
/*     */         } else {
/* 199 */           newServerCache = serverFilter.updateChild(oldServerSnap.getIndexedNode(), childKey, newChildNode, childChangePath, NO_COMPLETE_SOURCE, null);
/*     */         }
/*     */       }
/*     */     }
/* 203 */     ViewCache newViewCache = oldViewCache.updateServerSnap(newServerCache, (oldServerSnap.isFullyInitialized()) || (changePath.isEmpty()), serverFilter.filtersNodes());
/* 204 */     NodeFilter.CompleteChildSource source = new WriteTreeCompleteChildSource(writesCache, newViewCache, optCompleteCache);
/* 205 */     return generateEventCacheAfterServerEvent(newViewCache, changePath, writesCache, source, accumulator);
/*     */   }
/*     */   
/*     */   private ViewCache applyUserOverwrite(ViewCache oldViewCache, Path changePath, Node changedSnap, WriteTreeRef writesCache, Node optCompleteCache, ChildChangeAccumulator accumulator) {
/* 209 */     CacheNode oldEventSnap = oldViewCache.getEventCache();
/*     */     
/* 211 */     NodeFilter.CompleteChildSource source = new WriteTreeCompleteChildSource(writesCache, oldViewCache, optCompleteCache);
/* 212 */     ViewCache newViewCache; ViewCache newViewCache; if (changePath.isEmpty()) {
/* 213 */       IndexedNode newIndexed = IndexedNode.from(changedSnap, this.filter.getIndex());
/* 214 */       IndexedNode newEventCache = this.filter.updateFullNode(oldViewCache.getEventCache().getIndexedNode(), newIndexed, accumulator);
/* 215 */       newViewCache = oldViewCache.updateEventSnap(newEventCache, true, this.filter.filtersNodes());
/*     */     } else {
/* 217 */       ChildKey childKey = changePath.getFront();
/* 218 */       ViewCache newViewCache; if (childKey.isPriorityChildName()) {
/* 219 */         IndexedNode newEventCache = this.filter.updatePriority(oldViewCache.getEventCache().getIndexedNode(), changedSnap);
/* 220 */         newViewCache = oldViewCache.updateEventSnap(newEventCache, oldEventSnap.isFullyInitialized(), oldEventSnap.isFiltered());
/*     */       } else {
/* 222 */         Path childChangePath = changePath.popFront();
/* 223 */         Node oldChild = oldEventSnap.getNode().getImmediateChild(childKey);
/*     */         Node newChild;
/* 225 */         Node newChild; if (childChangePath.isEmpty())
/*     */         {
/* 227 */           newChild = changedSnap;
/*     */         } else {
/* 229 */           Node childNode = source.getCompleteChild(childKey);
/* 230 */           Node newChild; if (childNode != null) { Node newChild;
/* 231 */             if ((childChangePath.getBack().isPriorityChildName()) && (childNode.getChild(childChangePath.getParent()).isEmpty()))
/*     */             {
/*     */ 
/* 234 */               newChild = childNode;
/*     */             } else {
/* 236 */               newChild = childNode.updateChild(childChangePath, changedSnap);
/*     */             }
/*     */           }
/*     */           else {
/* 240 */             newChild = com.firebase.client.snapshot.EmptyNode.Empty();
/*     */           } }
/*     */         ViewCache newViewCache;
/* 243 */         if (!oldChild.equals(newChild)) {
/* 244 */           IndexedNode newEventSnap = this.filter.updateChild(oldEventSnap.getIndexedNode(), childKey, newChild, childChangePath, source, accumulator);
/*     */           
/* 246 */           newViewCache = oldViewCache.updateEventSnap(newEventSnap, oldEventSnap.isFullyInitialized(), this.filter.filtersNodes());
/*     */         } else {
/* 248 */           newViewCache = oldViewCache;
/*     */         }
/*     */       }
/*     */     }
/* 252 */     return newViewCache;
/*     */   }
/*     */   
/*     */   private static boolean cacheHasChild(ViewCache viewCache, ChildKey childKey) {
/* 256 */     return viewCache.getEventCache().isCompleteForChild(childKey);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private ViewCache applyUserMerge(ViewCache viewCache, Path path, CompoundWrite changedChildren, WriteTreeRef writesCache, Node serverCache, ChildChangeAccumulator accumulator)
/*     */   {
/* 266 */     assert (changedChildren.rootWrite() == null) : "Can't have a merge that is an overwrite";
/* 267 */     ViewCache currentViewCache = viewCache;
/* 268 */     for (Map.Entry<Path, Node> entry : changedChildren) {
/* 269 */       Path writePath = path.child((Path)entry.getKey());
/* 270 */       if (cacheHasChild(viewCache, writePath.getFront())) {
/* 271 */         currentViewCache = applyUserOverwrite(currentViewCache, writePath, (Node)entry.getValue(), writesCache, serverCache, accumulator);
/*     */       }
/*     */     }
/*     */     
/* 275 */     for (Map.Entry<Path, Node> entry : changedChildren) {
/* 276 */       Path writePath = path.child((Path)entry.getKey());
/* 277 */       if (!cacheHasChild(viewCache, writePath.getFront())) {
/* 278 */         currentViewCache = applyUserOverwrite(currentViewCache, writePath, (Node)entry.getValue(), writesCache, serverCache, accumulator);
/*     */       }
/*     */     }
/* 281 */     return currentViewCache;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private ViewCache applyServerMerge(ViewCache viewCache, Path path, CompoundWrite changedChildren, WriteTreeRef writesCache, Node serverCache, boolean filterServerNode, ChildChangeAccumulator accumulator)
/*     */   {
/* 289 */     if ((viewCache.getServerCache().getNode().isEmpty()) && (!viewCache.getServerCache().isFullyInitialized())) {
/* 290 */       return viewCache;
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 299 */     ViewCache curViewCache = viewCache;
/* 300 */     assert (changedChildren.rootWrite() == null) : "Can't have a merge that is an overwrite";
/*     */     CompoundWrite actualMerge;
/* 302 */     CompoundWrite actualMerge; if (path.isEmpty()) {
/* 303 */       actualMerge = changedChildren;
/*     */     } else {
/* 305 */       actualMerge = CompoundWrite.emptyWrite().addWrites(path, changedChildren);
/*     */     }
/* 307 */     Node serverNode = viewCache.getServerCache().getNode();
/* 308 */     java.util.Map<ChildKey, CompoundWrite> childCompoundWrites = actualMerge.childCompoundWrites();
/* 309 */     for (Map.Entry<ChildKey, CompoundWrite> childMerge : childCompoundWrites.entrySet()) {
/* 310 */       ChildKey childKey = (ChildKey)childMerge.getKey();
/* 311 */       if (serverNode.hasChild(childKey)) {
/* 312 */         Node serverChild = serverNode.getImmediateChild(childKey);
/* 313 */         Node newChild = ((CompoundWrite)childMerge.getValue()).apply(serverChild);
/* 314 */         curViewCache = applyServerOverwrite(curViewCache, new Path(new ChildKey[] { childKey }), newChild, writesCache, serverCache, filterServerNode, accumulator);
/*     */       }
/*     */     }
/* 317 */     for (Map.Entry<ChildKey, CompoundWrite> childMerge : childCompoundWrites.entrySet()) {
/* 318 */       ChildKey childKey = (ChildKey)childMerge.getKey();
/* 319 */       CompoundWrite childCompoundWrite = (CompoundWrite)childMerge.getValue();
/* 320 */       boolean isUnknownDeepMerge = (!viewCache.getServerCache().isCompleteForChild(childKey)) && (childCompoundWrite.rootWrite() == null);
/* 321 */       if ((!serverNode.hasChild(childKey)) && (!isUnknownDeepMerge)) {
/* 322 */         Node serverChild = serverNode.getImmediateChild(childKey);
/* 323 */         Node newChild = ((CompoundWrite)childMerge.getValue()).apply(serverChild);
/* 324 */         curViewCache = applyServerOverwrite(curViewCache, new Path(new ChildKey[] { childKey }), newChild, writesCache, serverCache, filterServerNode, accumulator);
/*     */       }
/*     */     }
/*     */     
/* 328 */     return curViewCache;
/*     */   }
/*     */   
/*     */ 
/*     */   private ViewCache ackUserWrite(ViewCache viewCache, Path ackPath, com.firebase.client.core.utilities.ImmutableTree<Boolean> affectedTree, WriteTreeRef writesCache, Node optCompleteCache, ChildChangeAccumulator accumulator)
/*     */   {
/* 334 */     if (writesCache.shadowingWrite(ackPath) != null) {
/* 335 */       return viewCache;
/*     */     }
/*     */     
/*     */ 
/* 339 */     boolean filterServerNode = viewCache.getServerCache().isFiltered();
/*     */     
/*     */ 
/*     */ 
/* 343 */     CacheNode serverCache = viewCache.getServerCache();
/* 344 */     if (affectedTree.getValue() != null)
/*     */     {
/* 346 */       if (((ackPath.isEmpty()) && (serverCache.isFullyInitialized())) || (serverCache.isCompleteForPath(ackPath))) {
/* 347 */         return applyServerOverwrite(viewCache, ackPath, serverCache.getNode().getChild(ackPath), writesCache, optCompleteCache, filterServerNode, accumulator);
/*     */       }
/* 349 */       if (ackPath.isEmpty())
/*     */       {
/*     */ 
/* 352 */         CompoundWrite changedChildren = CompoundWrite.emptyWrite();
/* 353 */         for (com.firebase.client.snapshot.NamedNode child : serverCache.getNode()) {
/* 354 */           changedChildren = changedChildren.addWrite(child.getName(), child.getNode());
/*     */         }
/* 356 */         return applyServerMerge(viewCache, ackPath, changedChildren, writesCache, optCompleteCache, filterServerNode, accumulator);
/*     */       }
/*     */       
/* 359 */       return viewCache;
/*     */     }
/*     */     
/*     */ 
/* 363 */     CompoundWrite changedChildren = CompoundWrite.emptyWrite();
/* 364 */     for (Map.Entry<Path, Boolean> entry : affectedTree) {
/* 365 */       Path mergePath = (Path)entry.getKey();
/* 366 */       Path serverCachePath = ackPath.child(mergePath);
/* 367 */       if (serverCache.isCompleteForPath(serverCachePath)) {
/* 368 */         changedChildren = changedChildren.addWrite(mergePath, serverCache.getNode().getChild(serverCachePath));
/*     */       }
/*     */     }
/* 371 */     return applyServerMerge(viewCache, ackPath, changedChildren, writesCache, optCompleteCache, filterServerNode, accumulator);
/*     */   }
/*     */   
/*     */ 
/*     */   public ViewCache revertUserWrite(ViewCache viewCache, Path path, WriteTreeRef writesCache, Node optCompleteServerCache, ChildChangeAccumulator accumulator)
/*     */   {
/* 377 */     if (writesCache.shadowingWrite(path) != null) {
/* 378 */       return viewCache;
/*     */     }
/* 380 */     NodeFilter.CompleteChildSource source = new WriteTreeCompleteChildSource(writesCache, viewCache, optCompleteServerCache);
/* 381 */     IndexedNode oldEventCache = viewCache.getEventCache().getIndexedNode();
/*     */     IndexedNode newEventCache;
/* 383 */     IndexedNode newEventCache; if ((path.isEmpty()) || (path.getFront().isPriorityChildName())) { Node newNode;
/*     */       Node newNode;
/* 385 */       if (viewCache.getServerCache().isFullyInitialized()) {
/* 386 */         newNode = writesCache.calcCompleteEventCache(viewCache.getCompleteServerSnap());
/*     */       } else {
/* 388 */         newNode = writesCache.calcCompleteEventChildren(viewCache.getServerCache().getNode());
/*     */       }
/* 390 */       IndexedNode indexedNode = IndexedNode.from(newNode, this.filter.getIndex());
/* 391 */       newEventCache = this.filter.updateFullNode(oldEventCache, indexedNode, accumulator);
/*     */     } else {
/* 393 */       ChildKey childKey = path.getFront();
/* 394 */       Node newChild = writesCache.calcCompleteChild(childKey, viewCache.getServerCache());
/* 395 */       if ((newChild == null) && (viewCache.getServerCache().isCompleteForChild(childKey)))
/* 396 */         newChild = oldEventCache.getNode().getImmediateChild(childKey);
/*     */       IndexedNode newEventCache;
/* 398 */       if (newChild != null) {
/* 399 */         newEventCache = this.filter.updateChild(oldEventCache, childKey, newChild, path.popFront(), source, accumulator);
/*     */       } else { IndexedNode newEventCache;
/* 401 */         if ((newChild == null) && (viewCache.getEventCache().getNode().hasChild(childKey)))
/*     */         {
/* 403 */           newEventCache = this.filter.updateChild(oldEventCache, childKey, com.firebase.client.snapshot.EmptyNode.Empty(), path.popFront(), source, accumulator);
/*     */         }
/*     */         else
/* 406 */           newEventCache = oldEventCache;
/*     */       }
/* 408 */       if ((newEventCache.getNode().isEmpty()) && (viewCache.getServerCache().isFullyInitialized()))
/*     */       {
/* 410 */         Node complete = writesCache.calcCompleteEventCache(viewCache.getCompleteServerSnap());
/* 411 */         if (complete.isLeafNode()) {
/* 412 */           IndexedNode indexedNode = IndexedNode.from(complete, this.filter.getIndex());
/* 413 */           newEventCache = this.filter.updateFullNode(newEventCache, indexedNode, accumulator);
/*     */         }
/*     */       }
/*     */     }
/* 417 */     boolean complete = (viewCache.getServerCache().isFullyInitialized()) || (writesCache.shadowingWrite(Path.getEmptyPath()) != null);
/* 418 */     return viewCache.updateEventSnap(newEventCache, complete, this.filter.filtersNodes());
/*     */   }
/*     */   
/*     */   private ViewCache listenComplete(ViewCache viewCache, Path path, WriteTreeRef writesCache, Node serverCache, ChildChangeAccumulator accumulator)
/*     */   {
/* 423 */     CacheNode oldServerNode = viewCache.getServerCache();
/* 424 */     ViewCache newViewCache = viewCache.updateServerSnap(oldServerNode.getIndexedNode(), (oldServerNode.isFullyInitialized()) || (path.isEmpty()), oldServerNode.isFiltered());
/* 425 */     return generateEventCacheAfterServerEvent(newViewCache, path, writesCache, NO_COMPLETE_SOURCE, accumulator);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/* 431 */   private static NodeFilter.CompleteChildSource NO_COMPLETE_SOURCE = new NodeFilter.CompleteChildSource()
/*     */   {
/*     */     public Node getCompleteChild(ChildKey childKey) {
/* 434 */       return null;
/*     */     }
/*     */     
/*     */     public com.firebase.client.snapshot.NamedNode getChildAfterChild(com.firebase.client.snapshot.Index index, com.firebase.client.snapshot.NamedNode child, boolean reverse)
/*     */     {
/* 439 */       return null;
/*     */     }
/*     */   };
/*     */   
/*     */ 
/*     */   private static class WriteTreeCompleteChildSource
/*     */     implements NodeFilter.CompleteChildSource
/*     */   {
/*     */     private final WriteTreeRef writes;
/*     */     private final ViewCache viewCache;
/*     */     private final Node optCompleteServerCache;
/*     */     
/*     */     public WriteTreeCompleteChildSource(WriteTreeRef writes, ViewCache viewCache, Node optCompleteServerCache)
/*     */     {
/* 453 */       this.writes = writes;
/* 454 */       this.viewCache = viewCache;
/* 455 */       this.optCompleteServerCache = optCompleteServerCache;
/*     */     }
/*     */     
/*     */     public Node getCompleteChild(ChildKey childKey)
/*     */     {
/* 460 */       CacheNode node = this.viewCache.getEventCache();
/* 461 */       if (node.isCompleteForChild(childKey))
/* 462 */         return node.getNode().getImmediateChild(childKey);
/*     */       CacheNode serverNode;
/*     */       CacheNode serverNode;
/* 465 */       if (this.optCompleteServerCache != null)
/*     */       {
/* 467 */         serverNode = new CacheNode(IndexedNode.from(this.optCompleteServerCache, com.firebase.client.snapshot.KeyIndex.getInstance()), true, false);
/*     */       } else {
/* 469 */         serverNode = this.viewCache.getServerCache();
/*     */       }
/* 471 */       return this.writes.calcCompleteChild(childKey, serverNode);
/*     */     }
/*     */     
/*     */ 
/*     */     public com.firebase.client.snapshot.NamedNode getChildAfterChild(com.firebase.client.snapshot.Index index, com.firebase.client.snapshot.NamedNode child, boolean reverse)
/*     */     {
/* 477 */       Node completeServerData = this.optCompleteServerCache != null ? this.optCompleteServerCache : this.viewCache.getCompleteServerSnap();
/* 478 */       return this.writes.calcNextNodeAfterPost(completeServerData, child, reverse, index);
/*     */     }
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/view/ViewProcessor.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */