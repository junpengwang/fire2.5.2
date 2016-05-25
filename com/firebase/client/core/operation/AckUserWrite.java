/*    */ package com.firebase.client.core.operation;
/*    */ 
/*    */ import com.firebase.client.collection.ImmutableSortedMap;
/*    */ import com.firebase.client.core.Path;
/*    */ import com.firebase.client.core.utilities.ImmutableTree;
/*    */ import com.firebase.client.snapshot.ChildKey;
/*    */ import com.firebase.client.utilities.Utilities;
/*    */ 
/*    */ public class AckUserWrite extends Operation
/*    */ {
/*    */   private final boolean revert;
/*    */   private final ImmutableTree<Boolean> affectedTree;
/*    */   
/*    */   public AckUserWrite(Path path, ImmutableTree<Boolean> affectedTree, boolean revert)
/*    */   {
/* 16 */     super(Operation.OperationType.AckUserWrite, OperationSource.USER, path);
/* 17 */     this.affectedTree = affectedTree;
/* 18 */     this.revert = revert;
/*    */   }
/*    */   
/*    */   public ImmutableTree<Boolean> getAffectedTree() {
/* 22 */     return this.affectedTree;
/*    */   }
/*    */   
/*    */   public boolean isRevert() {
/* 26 */     return this.revert;
/*    */   }
/*    */   
/*    */   public Operation operationForChild(ChildKey childKey) {
/* 30 */     if (!this.path.isEmpty()) {
/* 31 */       Utilities.hardAssert(this.path.getFront().equals(childKey), "operationForChild called for unrelated child.");
/* 32 */       return new AckUserWrite(this.path.popFront(), this.affectedTree, this.revert); }
/* 33 */     if (this.affectedTree.getValue() != null) {
/* 34 */       Utilities.hardAssert(this.affectedTree.getChildren().isEmpty(), "affectedTree should not have overlapping affected paths.");
/*    */       
/* 36 */       return this;
/*    */     }
/* 38 */     ImmutableTree<Boolean> childTree = this.affectedTree.subtree(new Path(new ChildKey[] { childKey }));
/* 39 */     return new AckUserWrite(Path.getEmptyPath(), childTree, this.revert);
/*    */   }
/*    */   
/*    */ 
/*    */   public String toString()
/*    */   {
/* 45 */     return String.format("AckUserWrite { path=%s, revert=%s, affectedTree=%s }", new Object[] { getPath(), Boolean.valueOf(this.revert), this.affectedTree });
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/operation/AckUserWrite.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */