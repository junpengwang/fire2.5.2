/*     */ package com.firebase.client.core;
/*     */ 
/*     */ import com.firebase.client.core.utilities.Predicate;
/*     */ import com.firebase.client.core.view.CacheNode;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.EmptyNode;
/*     */ import com.firebase.client.snapshot.Index;
/*     */ import com.firebase.client.snapshot.NamedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import java.util.Map.Entry;
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
/*     */ public class WriteTree
/*     */ {
/*     */   private CompoundWrite visibleWrites;
/*     */   private List<UserWriteRecord> allWrites;
/*     */   private Long lastWriteId;
/*     */   
/*     */   public WriteTree()
/*     */   {
/*  35 */     this.visibleWrites = CompoundWrite.emptyWrite();
/*  36 */     this.allWrites = new ArrayList();
/*  37 */     this.lastWriteId = Long.valueOf(-1L);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public WriteTreeRef childWrites(Path path)
/*     */   {
/*  45 */     return new WriteTreeRef(path, this);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void addOverwrite(Path path, Node snap, Long writeId, boolean visible)
/*     */   {
/*  52 */     assert (writeId.longValue() > this.lastWriteId.longValue());
/*  53 */     this.allWrites.add(new UserWriteRecord(writeId.longValue(), path, snap, visible));
/*  54 */     if (visible) {
/*  55 */       this.visibleWrites = this.visibleWrites.addWrite(path, snap);
/*     */     }
/*  57 */     this.lastWriteId = writeId;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void addMerge(Path path, CompoundWrite changedChildren, Long writeId)
/*     */   {
/*  64 */     assert (writeId.longValue() > this.lastWriteId.longValue());
/*  65 */     this.allWrites.add(new UserWriteRecord(writeId.longValue(), path, changedChildren));
/*  66 */     this.visibleWrites = this.visibleWrites.addWrites(path, changedChildren);
/*  67 */     this.lastWriteId = writeId;
/*     */   }
/*     */   
/*     */   public UserWriteRecord getWrite(long writeId) {
/*  71 */     for (UserWriteRecord record : this.allWrites) {
/*  72 */       if (record.getWriteId() == writeId) {
/*  73 */         return record;
/*     */       }
/*     */     }
/*  76 */     return null;
/*     */   }
/*     */   
/*     */   public List<UserWriteRecord> purgeAllWrites() {
/*  80 */     List<UserWriteRecord> purgedWrites = new ArrayList(this.allWrites);
/*     */     
/*  82 */     this.visibleWrites = CompoundWrite.emptyWrite();
/*  83 */     this.allWrites = new ArrayList();
/*  84 */     return purgedWrites;
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
/*     */   public boolean removeWrite(long writeId)
/*     */   {
/* 100 */     UserWriteRecord writeToRemove = null;
/* 101 */     int idx = 0;
/* 102 */     for (UserWriteRecord record : this.allWrites) {
/* 103 */       if (record.getWriteId() == writeId) {
/* 104 */         writeToRemove = record;
/* 105 */         break;
/*     */       }
/* 107 */       idx++;
/*     */     }
/* 109 */     assert (writeToRemove != null) : "removeWrite called with nonexistent writeId";
/*     */     
/* 111 */     this.allWrites.remove(writeToRemove);
/*     */     
/* 113 */     boolean removedWriteWasVisible = writeToRemove.isVisible();
/* 114 */     boolean removedWriteOverlapsWithOtherWrites = false;
/* 115 */     int i = this.allWrites.size() - 1;
/*     */     
/* 117 */     while ((removedWriteWasVisible) && (i >= 0)) {
/* 118 */       UserWriteRecord currentWrite = (UserWriteRecord)this.allWrites.get(i);
/* 119 */       if (currentWrite.isVisible()) {
/* 120 */         if ((i >= idx) && (recordContainsPath(currentWrite, writeToRemove.getPath())))
/*     */         {
/* 122 */           removedWriteWasVisible = false;
/* 123 */         } else if (writeToRemove.getPath().contains(currentWrite.getPath()))
/*     */         {
/* 125 */           removedWriteOverlapsWithOtherWrites = true;
/*     */         }
/*     */       }
/* 128 */       i--;
/*     */     }
/*     */     
/* 131 */     if (!removedWriteWasVisible)
/* 132 */       return false;
/* 133 */     if (removedWriteOverlapsWithOtherWrites)
/*     */     {
/* 135 */       resetTree();
/* 136 */       return true;
/*     */     }
/*     */     
/* 139 */     if (writeToRemove.isOverwrite()) {
/* 140 */       this.visibleWrites = this.visibleWrites.removeWrite(writeToRemove.getPath());
/*     */     } else {
/* 142 */       for (Map.Entry<Path, Node> entry : writeToRemove.getMerge()) {
/* 143 */         Path path = (Path)entry.getKey();
/* 144 */         this.visibleWrites = this.visibleWrites.removeWrite(writeToRemove.getPath().child(path));
/*     */       }
/*     */     }
/* 147 */     return true;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Node getCompleteWriteData(Path path)
/*     */   {
/* 156 */     return this.visibleWrites.getCompleteNode(path);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Node calcCompleteEventCache(Path treePath, Node completeServerCache)
/*     */   {
/* 164 */     return calcCompleteEventCache(treePath, completeServerCache, new ArrayList());
/*     */   }
/*     */   
/*     */   public Node calcCompleteEventCache(Path treePath, Node completeServerCache, List<Long> writeIdsToExclude) {
/* 168 */     return calcCompleteEventCache(treePath, completeServerCache, writeIdsToExclude, false);
/*     */   }
/*     */   
/*     */   public Node calcCompleteEventCache(final Path treePath, Node completeServerCache, final List<Long> writeIdsToExclude, final boolean includeHiddenWrites)
/*     */   {
/* 173 */     if ((writeIdsToExclude.isEmpty()) && (!includeHiddenWrites)) {
/* 174 */       Node shadowingNode = this.visibleWrites.getCompleteNode(treePath);
/* 175 */       if (shadowingNode != null) {
/* 176 */         return shadowingNode;
/*     */       }
/* 178 */       CompoundWrite subMerge = this.visibleWrites.childCompoundWrite(treePath);
/* 179 */       if (subMerge.isEmpty())
/* 180 */         return completeServerCache;
/* 181 */       if ((completeServerCache == null) && (!subMerge.hasCompleteWrite(Path.getEmptyPath())))
/*     */       {
/* 183 */         return null; }
/*     */       Node layeredCache;
/*     */       Node layeredCache;
/* 186 */       if (completeServerCache != null) {
/* 187 */         layeredCache = completeServerCache;
/*     */       } else {
/* 189 */         layeredCache = EmptyNode.Empty();
/*     */       }
/* 191 */       return subMerge.apply(layeredCache);
/*     */     }
/*     */     
/*     */ 
/* 195 */     CompoundWrite merge = this.visibleWrites.childCompoundWrite(treePath);
/* 196 */     if ((!includeHiddenWrites) && (merge.isEmpty())) {
/* 197 */       return completeServerCache;
/*     */     }
/*     */     
/* 200 */     if ((!includeHiddenWrites) && (completeServerCache == null) && (!merge.hasCompleteWrite(Path.getEmptyPath()))) {
/* 201 */       return null;
/*     */     }
/* 203 */     Predicate<UserWriteRecord> filter = new Predicate()
/*     */     {
/*     */       public boolean evaluate(UserWriteRecord write) {
/* 206 */         return ((write.isVisible()) || (includeHiddenWrites)) && (!writeIdsToExclude.contains(Long.valueOf(write.getWriteId()))) && ((write.getPath().contains(treePath)) || (treePath.contains(write.getPath())));
/*     */ 
/*     */       }
/*     */       
/*     */ 
/* 211 */     };
/* 212 */     CompoundWrite mergeAtPath = layerTree(this.allWrites, filter, treePath);
/* 213 */     Node layeredCache = completeServerCache != null ? completeServerCache : EmptyNode.Empty();
/* 214 */     return mergeAtPath.apply(layeredCache);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Node calcCompleteEventChildren(Path treePath, Node completeServerChildren)
/*     */   {
/* 225 */     Node completeChildren = EmptyNode.Empty();
/* 226 */     Node topLevelSet = this.visibleWrites.getCompleteNode(treePath);
/* 227 */     if (topLevelSet != null) {
/* 228 */       if (!topLevelSet.isLeafNode())
/*     */       {
/* 230 */         for (NamedNode childEntry : topLevelSet) {
/* 231 */           completeChildren = completeChildren.updateImmediateChild(childEntry.getName(), childEntry.getNode());
/*     */         }
/*     */       }
/* 234 */       return completeChildren;
/*     */     }
/*     */     
/*     */ 
/* 238 */     CompoundWrite merge = this.visibleWrites.childCompoundWrite(treePath);
/* 239 */     for (NamedNode entry : completeServerChildren) {
/* 240 */       Node node = merge.childCompoundWrite(new Path(new ChildKey[] { entry.getName() })).apply(entry.getNode());
/* 241 */       completeChildren = completeChildren.updateImmediateChild(entry.getName(), node);
/*     */     }
/*     */     
/* 244 */     for (NamedNode node : merge.getCompleteChildren()) {
/* 245 */       completeChildren = completeChildren.updateImmediateChild(node.getName(), node.getNode());
/*     */     }
/* 247 */     return completeChildren;
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
/*     */   public Node calcEventCacheAfterServerOverwrite(Path treePath, Path childPath, Node existingEventSnap, Node existingServerSnap)
/*     */   {
/* 267 */     assert ((existingEventSnap != null) || (existingServerSnap != null)) : "Either existingEventSnap or existingServerSnap must exist";
/* 268 */     Path path = treePath.child(childPath);
/* 269 */     if (this.visibleWrites.hasCompleteWrite(path))
/*     */     {
/*     */ 
/* 272 */       return null;
/*     */     }
/*     */     
/* 275 */     CompoundWrite childMerge = this.visibleWrites.childCompoundWrite(path);
/* 276 */     if (childMerge.isEmpty())
/*     */     {
/* 278 */       return existingServerSnap.getChild(childPath);
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 286 */     return childMerge.apply(existingServerSnap.getChild(childPath));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Node calcCompleteChild(Path treePath, ChildKey childKey, CacheNode existingServerSnap)
/*     */   {
/* 296 */     Path path = treePath.child(childKey);
/* 297 */     Node shadowingNode = this.visibleWrites.getCompleteNode(path);
/* 298 */     if (shadowingNode != null) {
/* 299 */       return shadowingNode;
/*     */     }
/* 301 */     if (existingServerSnap.isCompleteForChild(childKey)) {
/* 302 */       CompoundWrite childMerge = this.visibleWrites.childCompoundWrite(path);
/* 303 */       return childMerge.apply(existingServerSnap.getNode().getImmediateChild(childKey));
/*     */     }
/* 305 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Node shadowingWrite(Path path)
/*     */   {
/* 316 */     return this.visibleWrites.getCompleteNode(path);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public NamedNode calcNextNodeAfterPost(Path treePath, Node completeServerData, NamedNode post, boolean reverse, Index index)
/*     */   {
/* 325 */     CompoundWrite merge = this.visibleWrites.childCompoundWrite(treePath);
/* 326 */     Node shadowingNode = merge.getCompleteNode(Path.getEmptyPath());
/* 327 */     Node toIterate; if (shadowingNode != null) {
/* 328 */       toIterate = shadowingNode; } else { Node toIterate;
/* 329 */       if (completeServerData != null) {
/* 330 */         toIterate = merge.apply(completeServerData);
/*     */       }
/*     */       else
/* 333 */         return null; }
/*     */     Node toIterate;
/* 335 */     NamedNode currentNext = null;
/* 336 */     for (NamedNode node : toIterate) {
/* 337 */       if ((index.compare(node, post, reverse) > 0) && ((currentNext == null) || (index.compare(node, currentNext, reverse) < 0))) {
/* 338 */         currentNext = node;
/*     */       }
/*     */     }
/* 341 */     return currentNext;
/*     */   }
/*     */   
/*     */   private boolean recordContainsPath(UserWriteRecord writeRecord, Path path) {
/* 345 */     if (writeRecord.isOverwrite()) {
/* 346 */       return writeRecord.getPath().contains(path);
/*     */     }
/* 348 */     for (Map.Entry<Path, Node> entry : writeRecord.getMerge()) {
/* 349 */       if (writeRecord.getPath().child((Path)entry.getKey()).contains(path)) {
/* 350 */         return true;
/*     */       }
/*     */     }
/* 353 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private void resetTree()
/*     */   {
/* 361 */     this.visibleWrites = layerTree(this.allWrites, DEFAULT_FILTER, Path.getEmptyPath());
/* 362 */     if (this.allWrites.size() > 0) {
/* 363 */       this.lastWriteId = Long.valueOf(((UserWriteRecord)this.allWrites.get(this.allWrites.size() - 1)).getWriteId());
/*     */     } else {
/* 365 */       this.lastWriteId = Long.valueOf(-1L);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/* 372 */   private static final Predicate<UserWriteRecord> DEFAULT_FILTER = new Predicate()
/*     */   {
/*     */     public boolean evaluate(UserWriteRecord write) {
/* 375 */       return write.isVisible();
/*     */     }
/*     */   };
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private static CompoundWrite layerTree(List<UserWriteRecord> writes, Predicate<UserWriteRecord> filter, Path treeRoot)
/*     */   {
/* 384 */     CompoundWrite compoundWrite = CompoundWrite.emptyWrite();
/* 385 */     for (UserWriteRecord write : writes)
/*     */     {
/*     */ 
/*     */ 
/* 389 */       if (filter.evaluate(write)) {
/* 390 */         Path writePath = write.getPath();
/* 391 */         if (write.isOverwrite()) {
/* 392 */           if (treeRoot.contains(writePath)) {
/* 393 */             Path relativePath = Path.getRelative(treeRoot, writePath);
/* 394 */             compoundWrite = compoundWrite.addWrite(relativePath, write.getOverwrite());
/* 395 */           } else if (writePath.contains(treeRoot)) {
/* 396 */             compoundWrite = compoundWrite.addWrite(Path.getEmptyPath(), write.getOverwrite().getChild(Path.getRelative(writePath, treeRoot)));
/*     */           }
/*     */           
/*     */ 
/*     */         }
/* 401 */         else if (treeRoot.contains(writePath)) {
/* 402 */           Path relativePath = Path.getRelative(treeRoot, writePath);
/* 403 */           compoundWrite = compoundWrite.addWrites(relativePath, write.getMerge());
/* 404 */         } else if (writePath.contains(treeRoot)) {
/* 405 */           Path relativePath = Path.getRelative(writePath, treeRoot);
/* 406 */           if (relativePath.isEmpty()) {
/* 407 */             compoundWrite = compoundWrite.addWrites(Path.getEmptyPath(), write.getMerge());
/*     */           } else {
/* 409 */             Node deepNode = write.getMerge().getCompleteNode(relativePath);
/* 410 */             if (deepNode != null) {
/* 411 */               compoundWrite = compoundWrite.addWrite(Path.getEmptyPath(), deepNode);
/*     */             }
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */     
/*     */ 
/*     */ 
/* 420 */     return compoundWrite;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/WriteTree.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */