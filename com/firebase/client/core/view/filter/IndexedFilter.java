/*     */ package com.firebase.client.core.view.filter;
/*     */ 
/*     */ import com.firebase.client.core.view.Change;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.Index;
/*     */ import com.firebase.client.snapshot.IndexedNode;
/*     */ import com.firebase.client.snapshot.NamedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ 
/*     */ public class IndexedFilter implements NodeFilter
/*     */ {
/*     */   private final Index index;
/*     */   
/*     */   public IndexedFilter(Index index)
/*     */   {
/*  16 */     this.index = index;
/*     */   }
/*     */   
/*     */ 
/*     */   public IndexedNode updateChild(IndexedNode indexedNode, ChildKey key, Node newChild, com.firebase.client.core.Path affectedPath, NodeFilter.CompleteChildSource source, ChildChangeAccumulator optChangeAccumulator)
/*     */   {
/*  22 */     assert (indexedNode.hasIndex(this.index)) : "The index must match the filter";
/*  23 */     Node snap = indexedNode.getNode();
/*  24 */     Node oldChild = snap.getImmediateChild(key);
/*     */     
/*  26 */     if (oldChild.getChild(affectedPath).equals(newChild.getChild(affectedPath)))
/*     */     {
/*     */ 
/*     */ 
/*  30 */       if (oldChild.isEmpty() == newChild.isEmpty())
/*     */       {
/*     */ 
/*     */ 
/*     */ 
/*  35 */         return indexedNode;
/*     */       }
/*     */     }
/*  38 */     if (optChangeAccumulator != null) {
/*  39 */       if (newChild.isEmpty()) {
/*  40 */         if (snap.hasChild(key)) {
/*  41 */           optChangeAccumulator.trackChildChange(Change.childRemovedChange(key, oldChild));
/*     */         }
/*  43 */         else if ((!$assertionsDisabled) && (!snap.isLeafNode())) throw new AssertionError("A child remove without an old child only makes sense on a leaf node");
/*     */       }
/*  45 */       else if (oldChild.isEmpty()) {
/*  46 */         optChangeAccumulator.trackChildChange(Change.childAddedChange(key, newChild));
/*     */       } else {
/*  48 */         optChangeAccumulator.trackChildChange(Change.childChangedChange(key, newChild, oldChild));
/*     */       }
/*     */     }
/*  51 */     if ((snap.isLeafNode()) && (newChild.isEmpty())) {
/*  52 */       return indexedNode;
/*     */     }
/*     */     
/*  55 */     return indexedNode.updateChild(key, newChild);
/*     */   }
/*     */   
/*     */ 
/*     */   public IndexedNode updateFullNode(IndexedNode oldSnap, IndexedNode newSnap, ChildChangeAccumulator optChangeAccumulator)
/*     */   {
/*  61 */     assert (newSnap.hasIndex(this.index)) : "Can't use IndexedNode that doesn't have filter's index";
/*  62 */     if (optChangeAccumulator != null) {
/*  63 */       for (NamedNode child : oldSnap.getNode()) {
/*  64 */         if (!newSnap.getNode().hasChild(child.getName())) {
/*  65 */           optChangeAccumulator.trackChildChange(Change.childRemovedChange(child.getName(), child.getNode()));
/*     */         }
/*     */       }
/*  68 */       if (!newSnap.getNode().isLeafNode()) {
/*  69 */         for (NamedNode child : newSnap.getNode()) {
/*  70 */           if (oldSnap.getNode().hasChild(child.getName())) {
/*  71 */             Node oldChild = oldSnap.getNode().getImmediateChild(child.getName());
/*  72 */             if (!oldChild.equals(child.getNode())) {
/*  73 */               optChangeAccumulator.trackChildChange(Change.childChangedChange(child.getName(), child.getNode(), oldChild));
/*     */             }
/*     */           } else {
/*  76 */             optChangeAccumulator.trackChildChange(Change.childAddedChange(child.getName(), child.getNode()));
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*  81 */     return newSnap;
/*     */   }
/*     */   
/*     */   public IndexedNode updatePriority(IndexedNode oldSnap, Node newPriority)
/*     */   {
/*  86 */     if (oldSnap.getNode().isEmpty()) {
/*  87 */       return oldSnap;
/*     */     }
/*  89 */     return oldSnap.updatePriority(newPriority);
/*     */   }
/*     */   
/*     */ 
/*     */   public NodeFilter getIndexedFilter()
/*     */   {
/*  95 */     return this;
/*     */   }
/*     */   
/*     */   public Index getIndex()
/*     */   {
/* 100 */     return this.index;
/*     */   }
/*     */   
/*     */   public boolean filtersNodes()
/*     */   {
/* 105 */     return false;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/view/filter/IndexedFilter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */