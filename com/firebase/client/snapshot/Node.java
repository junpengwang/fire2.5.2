/*    */ package com.firebase.client.snapshot;
/*    */ 
/*    */ import com.firebase.client.core.Path;
/*    */ 
/*    */ public abstract interface Node extends Comparable<Node>, Iterable<NamedNode>
/*    */ {
/*    */   public abstract boolean isLeafNode();
/*    */   
/*    */   public abstract Node getPriority();
/*    */   
/*    */   public abstract Node getChild(Path paramPath);
/*    */   
/*    */   public abstract Node getImmediateChild(ChildKey paramChildKey);
/*    */   
/*    */   public static enum HashVersion
/*    */   {
/* 17 */     V1, 
/*    */     
/* 19 */     V2;
/*    */     
/*    */ 
/*    */     private HashVersion() {}
/*    */   }
/*    */   
/*    */ 
/*    */   public abstract Node updateImmediateChild(ChildKey paramChildKey, Node paramNode);
/*    */   
/*    */ 
/*    */   public abstract ChildKey getPredecessorChildKey(ChildKey paramChildKey);
/*    */   
/*    */ 
/*    */   public abstract ChildKey getSuccessorChildKey(ChildKey paramChildKey);
/*    */   
/*    */ 
/*    */   public abstract Node updateChild(Path paramPath, Node paramNode);
/*    */   
/*    */ 
/*    */   public abstract Node updatePriority(Node paramNode);
/*    */   
/*    */   public abstract boolean hasChild(ChildKey paramChildKey);
/*    */   
/*    */   public abstract boolean isEmpty();
/*    */   
/*    */   public abstract int getChildCount();
/*    */   
/*    */   public abstract Object getValue();
/*    */   
/*    */   public abstract Object getValue(boolean paramBoolean);
/*    */   
/*    */   public abstract String getHash();
/*    */   
/*    */   public abstract String getHashRepresentation(HashVersion paramHashVersion);
/*    */   
/*    */   public abstract java.util.Iterator<NamedNode> reverseIterator();
/*    */   
/* 56 */   public static final ChildrenNode MAX_NODE = new ChildrenNode()
/*    */   {
/*    */     public int compareTo(Node other) {
/* 59 */       return other == this ? 0 : 1;
/*    */     }
/*    */     
/*    */     public boolean equals(Object other)
/*    */     {
/* 64 */       return other == this;
/*    */     }
/*    */     
/*    */     public Node getPriority()
/*    */     {
/* 69 */       return this;
/*    */     }
/*    */     
/*    */     public boolean isEmpty()
/*    */     {
/* 74 */       return false;
/*    */     }
/*    */     
/*    */     public boolean hasChild(ChildKey childKey)
/*    */     {
/* 79 */       return false;
/*    */     }
/*    */     
/*    */     public Node getImmediateChild(ChildKey name)
/*    */     {
/* 84 */       if (name.isPriorityChildName()) {
/* 85 */         return getPriority();
/*    */       }
/* 87 */       return EmptyNode.Empty();
/*    */     }
/*    */     
/*    */ 
/*    */     public String toString()
/*    */     {
/* 93 */       return "<Max Node>";
/*    */     }
/*    */   };
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/snapshot/Node.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */