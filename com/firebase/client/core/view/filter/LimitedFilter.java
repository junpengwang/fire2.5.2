/*     */ package com.firebase.client.core.view.filter;
/*     */ 
/*     */ import com.firebase.client.core.view.QueryParams;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.EmptyNode;
/*     */ import com.firebase.client.snapshot.Index;
/*     */ import com.firebase.client.snapshot.IndexedNode;
/*     */ import com.firebase.client.snapshot.NamedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ 
/*     */ public class LimitedFilter implements NodeFilter
/*     */ {
/*     */   private final RangedFilter rangedFilter;
/*     */   private final Index index;
/*     */   private final int limit;
/*     */   private final boolean reverse;
/*     */   
/*     */   public LimitedFilter(QueryParams params)
/*     */   {
/*  20 */     this.rangedFilter = new RangedFilter(params);
/*  21 */     this.index = params.getIndex();
/*  22 */     this.limit = params.getLimit();
/*  23 */     this.reverse = (!params.isViewFromLeft());
/*     */   }
/*     */   
/*     */ 
/*     */   public IndexedNode updateChild(IndexedNode snap, ChildKey key, Node newChild, com.firebase.client.core.Path affectedPath, NodeFilter.CompleteChildSource source, ChildChangeAccumulator optChangeAccumulator)
/*     */   {
/*  29 */     if (!this.rangedFilter.matches(new NamedNode(key, newChild))) {
/*  30 */       newChild = EmptyNode.Empty();
/*     */     }
/*  32 */     if (snap.getNode().getImmediateChild(key).equals(newChild))
/*     */     {
/*  34 */       return snap; }
/*  35 */     if (snap.getNode().getChildCount() < this.limit) {
/*  36 */       return this.rangedFilter.getIndexedFilter().updateChild(snap, key, newChild, affectedPath, source, optChangeAccumulator);
/*     */     }
/*  38 */     return fullLimitUpdateChild(snap, key, newChild, source, optChangeAccumulator);
/*     */   }
/*     */   
/*     */ 
/*     */   private IndexedNode fullLimitUpdateChild(IndexedNode oldIndexed, ChildKey childKey, Node childSnap, NodeFilter.CompleteChildSource source, ChildChangeAccumulator optChangeAccumulator)
/*     */   {
/*  44 */     assert (oldIndexed.getNode().getChildCount() == this.limit);
/*  45 */     NamedNode newChildNamedNode = new NamedNode(childKey, childSnap);
/*  46 */     NamedNode windowBoundary = this.reverse ? oldIndexed.getFirstChild() : oldIndexed.getLastChild();
/*  47 */     boolean inRange = this.rangedFilter.matches(newChildNamedNode);
/*  48 */     if (oldIndexed.getNode().hasChild(childKey)) {
/*  49 */       Node oldChildSnap = oldIndexed.getNode().getImmediateChild(childKey);
/*  50 */       NamedNode nextChild = source.getChildAfterChild(this.index, windowBoundary, this.reverse);
/*  51 */       while ((nextChild != null) && ((nextChild.getName().equals(childKey)) || (oldIndexed.getNode().hasChild(nextChild.getName()))))
/*     */       {
/*     */ 
/*     */ 
/*     */ 
/*  56 */         nextChild = source.getChildAfterChild(this.index, nextChild, this.reverse);
/*     */       }
/*  58 */       int compareNext = nextChild == null ? 1 : this.index.compare(nextChild, newChildNamedNode, this.reverse);
/*  59 */       boolean remainsInWindow = (inRange) && (!childSnap.isEmpty()) && (compareNext >= 0);
/*  60 */       if (remainsInWindow) {
/*  61 */         if (optChangeAccumulator != null) {
/*  62 */           optChangeAccumulator.trackChildChange(com.firebase.client.core.view.Change.childChangedChange(childKey, childSnap, oldChildSnap));
/*     */         }
/*  64 */         return oldIndexed.updateChild(childKey, childSnap);
/*     */       }
/*  66 */       if (optChangeAccumulator != null) {
/*  67 */         optChangeAccumulator.trackChildChange(com.firebase.client.core.view.Change.childRemovedChange(childKey, oldChildSnap));
/*     */       }
/*  69 */       IndexedNode newIndexed = oldIndexed.updateChild(childKey, EmptyNode.Empty());
/*  70 */       boolean nextChildInRange = (nextChild != null) && (this.rangedFilter.matches(nextChild));
/*  71 */       if (nextChildInRange) {
/*  72 */         if (optChangeAccumulator != null) {
/*  73 */           optChangeAccumulator.trackChildChange(com.firebase.client.core.view.Change.childAddedChange(nextChild.getName(), nextChild.getNode()));
/*     */         }
/*  75 */         return newIndexed.updateChild(nextChild.getName(), nextChild.getNode());
/*     */       }
/*  77 */       return newIndexed;
/*     */     }
/*     */     
/*  80 */     if (childSnap.isEmpty())
/*     */     {
/*  82 */       return oldIndexed; }
/*  83 */     if (inRange) {
/*  84 */       if (this.index.compare(windowBoundary, newChildNamedNode, this.reverse) >= 0) {
/*  85 */         if (optChangeAccumulator != null) {
/*  86 */           optChangeAccumulator.trackChildChange(com.firebase.client.core.view.Change.childRemovedChange(windowBoundary.getName(), windowBoundary.getNode()));
/*  87 */           optChangeAccumulator.trackChildChange(com.firebase.client.core.view.Change.childAddedChange(childKey, childSnap));
/*     */         }
/*  89 */         return oldIndexed.updateChild(childKey, childSnap).updateChild(windowBoundary.getName(), EmptyNode.Empty());
/*     */       }
/*  91 */       return oldIndexed;
/*     */     }
/*     */     
/*  94 */     return oldIndexed;
/*     */   }
/*     */   
/*     */   public IndexedNode updateFullNode(IndexedNode oldSnap, IndexedNode newSnap, ChildChangeAccumulator optChangeAccumulator)
/*     */   {
/*     */     IndexedNode filtered;
/*     */     IndexedNode filtered;
/* 101 */     if ((newSnap.getNode().isLeafNode()) || (newSnap.getNode().isEmpty()))
/*     */     {
/* 103 */       filtered = IndexedNode.from(EmptyNode.Empty(), this.index);
/*     */     } else {
/* 105 */       filtered = newSnap;
/*     */       
/* 107 */       filtered = filtered.updatePriority(com.firebase.client.snapshot.PriorityUtilities.NullPriority());
/*     */       int sign;
/*     */       java.util.Iterator<NamedNode> iterator;
/*     */       NamedNode startPost;
/*     */       NamedNode endPost;
/* 112 */       int sign; if (this.reverse) {
/* 113 */         java.util.Iterator<NamedNode> iterator = newSnap.reverseIterator();
/* 114 */         NamedNode startPost = this.rangedFilter.getEndPost();
/* 115 */         NamedNode endPost = this.rangedFilter.getStartPost();
/* 116 */         sign = -1;
/*     */       } else {
/* 118 */         iterator = newSnap.iterator();
/* 119 */         startPost = this.rangedFilter.getStartPost();
/* 120 */         endPost = this.rangedFilter.getEndPost();
/* 121 */         sign = 1;
/*     */       }
/*     */       
/* 124 */       int count = 0;
/* 125 */       boolean foundStartPost = false;
/* 126 */       while (iterator.hasNext()) {
/* 127 */         NamedNode next = (NamedNode)iterator.next();
/* 128 */         if ((!foundStartPost) && (this.index.compare(startPost, next) * sign <= 0))
/*     */         {
/* 130 */           foundStartPost = true;
/*     */         }
/* 132 */         boolean inRange = (foundStartPost) && (count < this.limit) && (this.index.compare(next, endPost) * sign <= 0);
/* 133 */         if (inRange) {
/* 134 */           count++;
/*     */         } else {
/* 136 */           filtered = filtered.updateChild(next.getName(), EmptyNode.Empty());
/*     */         }
/*     */       }
/*     */     }
/* 140 */     return this.rangedFilter.getIndexedFilter().updateFullNode(oldSnap, filtered, optChangeAccumulator);
/*     */   }
/*     */   
/*     */ 
/*     */   public IndexedNode updatePriority(IndexedNode oldSnap, Node newPriority)
/*     */   {
/* 146 */     return oldSnap;
/*     */   }
/*     */   
/*     */   public NodeFilter getIndexedFilter()
/*     */   {
/* 151 */     return this.rangedFilter.getIndexedFilter();
/*     */   }
/*     */   
/*     */   public Index getIndex()
/*     */   {
/* 156 */     return this.index;
/*     */   }
/*     */   
/*     */   public boolean filtersNodes()
/*     */   {
/* 161 */     return true;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/view/filter/LimitedFilter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */