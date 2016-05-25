/*    */ package com.firebase.client.snapshot;
/*    */ 
/*    */ import com.firebase.client.core.Path;
/*    */ 
/*    */ public class PathIndex extends Index
/*    */ {
/*    */   private final Path indexPath;
/*    */   
/*    */   public PathIndex(Path indexPath) {
/* 10 */     if ((indexPath.size() == 1) && (indexPath.getFront().isPriorityChildName())) {
/* 11 */       throw new IllegalArgumentException("Can't create PathIndex with '.priority' as key. Please use PriorityIndex instead!");
/*    */     }
/* 13 */     this.indexPath = indexPath;
/*    */   }
/*    */   
/*    */   public boolean isDefinedOn(Node snapshot)
/*    */   {
/* 18 */     return !snapshot.getChild(this.indexPath).isEmpty();
/*    */   }
/*    */   
/*    */   public int compare(NamedNode a, NamedNode b)
/*    */   {
/* 23 */     Node aChild = a.getNode().getChild(this.indexPath);
/* 24 */     Node bChild = b.getNode().getChild(this.indexPath);
/* 25 */     int indexCmp = aChild.compareTo(bChild);
/* 26 */     if (indexCmp == 0) {
/* 27 */       return a.getName().compareTo(b.getName());
/*    */     }
/* 29 */     return indexCmp;
/*    */   }
/*    */   
/*    */ 
/*    */   public NamedNode makePost(ChildKey name, Node value)
/*    */   {
/* 35 */     Node node = EmptyNode.Empty().updateChild(this.indexPath, value);
/* 36 */     return new NamedNode(name, node);
/*    */   }
/*    */   
/*    */   public NamedNode maxPost()
/*    */   {
/* 41 */     Node node = EmptyNode.Empty().updateChild(this.indexPath, Node.MAX_NODE);
/* 42 */     return new NamedNode(ChildKey.getMaxName(), node);
/*    */   }
/*    */   
/*    */   public String getQueryDefinition()
/*    */   {
/* 47 */     return this.indexPath.wireFormat();
/*    */   }
/*    */   
/*    */   public boolean equals(Object o)
/*    */   {
/* 52 */     if (this == o) return true;
/* 53 */     if ((o == null) || (getClass() != o.getClass())) { return false;
/*    */     }
/* 55 */     PathIndex that = (PathIndex)o;
/*    */     
/* 57 */     if (!this.indexPath.equals(that.indexPath)) { return false;
/*    */     }
/* 59 */     return true;
/*    */   }
/*    */   
/*    */   public int hashCode()
/*    */   {
/* 64 */     return this.indexPath.hashCode();
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/snapshot/PathIndex.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */