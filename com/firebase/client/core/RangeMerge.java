/*    */ package com.firebase.client.core;
/*    */ 
/*    */ import com.firebase.client.snapshot.ChildKey;
/*    */ import com.firebase.client.snapshot.EmptyNode;
/*    */ import com.firebase.client.snapshot.NamedNode;
/*    */ import com.firebase.client.snapshot.Node;
/*    */ import java.util.ArrayList;
/*    */ import java.util.HashSet;
/*    */ import java.util.List;
/*    */ import java.util.Set;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class RangeMerge
/*    */ {
/*    */   private final Path optExclusiveStart;
/*    */   private final Path optInclusiveEnd;
/*    */   private final Node snap;
/*    */   
/*    */   public RangeMerge(Path optExclusiveStart, Path optInclusiveEnd, Node snap)
/*    */   {
/* 25 */     this.optExclusiveStart = optExclusiveStart;
/* 26 */     this.optInclusiveEnd = optInclusiveEnd;
/* 27 */     this.snap = snap;
/*    */   }
/*    */   
/*    */   public Node applyTo(Node node) {
/* 31 */     return updateRangeInNode(Path.getEmptyPath(), node, this.snap);
/*    */   }
/*    */   
/*    */   Path getStart() {
/* 35 */     return this.optExclusiveStart;
/*    */   }
/*    */   
/*    */   Path getEnd() {
/* 39 */     return this.optInclusiveEnd;
/*    */   }
/*    */   
/*    */   private Node updateRangeInNode(Path currentPath, Node node, Node updateNode) {
/* 43 */     int startComparison = this.optExclusiveStart == null ? 1 : currentPath.compareTo(this.optExclusiveStart);
/* 44 */     int endComparison = this.optInclusiveEnd == null ? -1 : currentPath.compareTo(this.optInclusiveEnd);
/* 45 */     boolean startInNode = (this.optExclusiveStart != null) && (currentPath.contains(this.optExclusiveStart));
/* 46 */     boolean endInNode = (this.optInclusiveEnd != null) && (currentPath.contains(this.optInclusiveEnd));
/* 47 */     if ((startComparison > 0) && (endComparison < 0) && (!endInNode))
/*    */     {
/* 49 */       return updateNode; }
/* 50 */     if ((startComparison > 0) && (endInNode) && (updateNode.isLeafNode()))
/* 51 */       return updateNode;
/* 52 */     if ((startComparison > 0) && (endComparison == 0)) {
/* 53 */       assert (endInNode);
/* 54 */       assert (!updateNode.isLeafNode());
/* 55 */       if (node.isLeafNode())
/*    */       {
/* 57 */         return EmptyNode.Empty();
/*    */       }
/*    */       
/* 60 */       return node;
/*    */     }
/* 62 */     if ((startInNode) || (endInNode))
/*    */     {
/*    */ 
/* 65 */       Set<ChildKey> allChildren = new HashSet();
/* 66 */       for (NamedNode child : node) {
/* 67 */         allChildren.add(child.getName());
/*    */       }
/* 69 */       for (NamedNode child : updateNode) {
/* 70 */         allChildren.add(child.getName());
/*    */       }
/* 72 */       List<ChildKey> inOrder = new ArrayList(allChildren.size() + 1);
/* 73 */       inOrder.addAll(allChildren);
/*    */       
/* 75 */       if ((!updateNode.getPriority().isEmpty()) || (!node.getPriority().isEmpty())) {
/* 76 */         inOrder.add(ChildKey.getPriorityKey());
/*    */       }
/* 78 */       Node newNode = node;
/* 79 */       for (ChildKey key : inOrder) {
/* 80 */         Node currentChild = node.getImmediateChild(key);
/* 81 */         Node updatedChild = updateRangeInNode(currentPath.child(key), node.getImmediateChild(key), updateNode.getImmediateChild(key));
/*    */         
/* 83 */         if (updatedChild != currentChild) {
/* 84 */           newNode = newNode.updateImmediateChild(key, updatedChild);
/*    */         }
/*    */       }
/* 87 */       return newNode;
/*    */     }
/*    */     
/* 90 */     assert ((endComparison > 0) || (startComparison <= 0));
/* 91 */     return node;
/*    */   }
/*    */   
/*    */ 
/*    */   public String toString()
/*    */   {
/* 97 */     return "RangeMerge{optExclusiveStart=" + this.optExclusiveStart + ", optInclusiveEnd=" + this.optInclusiveEnd + ", snap=" + this.snap + '}';
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/RangeMerge.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */