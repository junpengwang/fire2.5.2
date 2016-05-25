/*     */ package com.firebase.client.core.view.filter;
/*     */ 
/*     */ import com.firebase.client.core.view.QueryParams;
/*     */ import com.firebase.client.snapshot.Index;
/*     */ import com.firebase.client.snapshot.IndexedNode;
/*     */ import com.firebase.client.snapshot.NamedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ 
/*     */ public class RangedFilter implements NodeFilter
/*     */ {
/*     */   private final IndexedFilter indexedFilter;
/*     */   private final Index index;
/*     */   private final NamedNode startPost;
/*     */   private final NamedNode endPost;
/*     */   
/*     */   public RangedFilter(QueryParams params)
/*     */   {
/*  18 */     this.indexedFilter = new IndexedFilter(params.getIndex());
/*  19 */     this.index = params.getIndex();
/*  20 */     this.startPost = getStartPost(params);
/*  21 */     this.endPost = getEndPost(params);
/*     */   }
/*     */   
/*     */   public NamedNode getStartPost() {
/*  25 */     return this.startPost;
/*     */   }
/*     */   
/*     */   public NamedNode getEndPost() {
/*  29 */     return this.endPost;
/*     */   }
/*     */   
/*     */   private static NamedNode getStartPost(QueryParams params) {
/*  33 */     if (params.hasStart()) {
/*  34 */       com.firebase.client.snapshot.ChildKey startName = params.getIndexStartName();
/*  35 */       return params.getIndex().makePost(startName, params.getIndexStartValue());
/*     */     }
/*  37 */     return params.getIndex().minPost();
/*     */   }
/*     */   
/*     */   private static NamedNode getEndPost(QueryParams params)
/*     */   {
/*  42 */     if (params.hasEnd()) {
/*  43 */       com.firebase.client.snapshot.ChildKey endName = params.getIndexEndName();
/*  44 */       return params.getIndex().makePost(endName, params.getIndexEndValue());
/*     */     }
/*  46 */     return params.getIndex().maxPost();
/*     */   }
/*     */   
/*     */   public boolean matches(NamedNode node)
/*     */   {
/*  51 */     if ((this.index.compare(getStartPost(), node) <= 0) && (this.index.compare(node, getEndPost()) <= 0)) {
/*  52 */       return true;
/*     */     }
/*  54 */     return false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public IndexedNode updateChild(IndexedNode snap, com.firebase.client.snapshot.ChildKey key, Node newChild, com.firebase.client.core.Path affectedPath, NodeFilter.CompleteChildSource source, ChildChangeAccumulator optChangeAccumulator)
/*     */   {
/*  61 */     if (!matches(new NamedNode(key, newChild))) {
/*  62 */       newChild = com.firebase.client.snapshot.EmptyNode.Empty();
/*     */     }
/*  64 */     return this.indexedFilter.updateChild(snap, key, newChild, affectedPath, source, optChangeAccumulator);
/*     */   }
/*     */   
/*     */   public IndexedNode updateFullNode(IndexedNode oldSnap, IndexedNode newSnap, ChildChangeAccumulator optChangeAccumulator) {
/*     */     IndexedNode filtered;
/*     */     IndexedNode filtered;
/*  70 */     if (newSnap.getNode().isLeafNode())
/*     */     {
/*  72 */       filtered = IndexedNode.from(com.firebase.client.snapshot.EmptyNode.Empty(), this.index);
/*     */     }
/*     */     else {
/*  75 */       filtered = newSnap.updatePriority(com.firebase.client.snapshot.PriorityUtilities.NullPriority());
/*  76 */       for (NamedNode child : newSnap) {
/*  77 */         if (!matches(child)) {
/*  78 */           filtered = filtered.updateChild(child.getName(), com.firebase.client.snapshot.EmptyNode.Empty());
/*     */         }
/*     */       }
/*     */     }
/*  82 */     return this.indexedFilter.updateFullNode(oldSnap, filtered, optChangeAccumulator);
/*     */   }
/*     */   
/*     */ 
/*     */   public IndexedNode updatePriority(IndexedNode oldSnap, Node newPriority)
/*     */   {
/*  88 */     return oldSnap;
/*     */   }
/*     */   
/*     */   public NodeFilter getIndexedFilter()
/*     */   {
/*  93 */     return this.indexedFilter;
/*     */   }
/*     */   
/*     */   public Index getIndex()
/*     */   {
/*  98 */     return this.index;
/*     */   }
/*     */   
/*     */   public boolean filtersNodes()
/*     */   {
/* 103 */     return true;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/view/filter/RangedFilter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */