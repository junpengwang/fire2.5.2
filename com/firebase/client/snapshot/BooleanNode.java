/*    */ package com.firebase.client.snapshot;
/*    */ 
/*    */ public class BooleanNode
/*    */   extends LeafNode<BooleanNode>
/*    */ {
/*    */   private final boolean value;
/*    */   
/*    */   public BooleanNode(Boolean value, Node priority)
/*    */   {
/* 10 */     super(priority);
/* 11 */     this.value = value.booleanValue();
/*    */   }
/*    */   
/*    */   public Object getValue()
/*    */   {
/* 16 */     return Boolean.valueOf(this.value);
/*    */   }
/*    */   
/*    */   public String getHashRepresentation(Node.HashVersion version)
/*    */   {
/* 21 */     return getPriorityHash(version) + "boolean:" + this.value;
/*    */   }
/*    */   
/*    */   public BooleanNode updatePriority(Node priority)
/*    */   {
/* 26 */     return new BooleanNode(Boolean.valueOf(this.value), priority);
/*    */   }
/*    */   
/*    */   protected LeafNode.LeafType getLeafType()
/*    */   {
/* 31 */     return LeafNode.LeafType.Boolean;
/*    */   }
/*    */   
/*    */   protected int compareLeafValues(BooleanNode other)
/*    */   {
/* 36 */     return this.value ? 1 : this.value == other.value ? 0 : -1;
/*    */   }
/*    */   
/*    */   public boolean equals(Object other)
/*    */   {
/* 41 */     if (!(other instanceof BooleanNode)) {
/* 42 */       return false;
/*    */     }
/* 44 */     BooleanNode otherBooleanNode = (BooleanNode)other;
/* 45 */     return (this.value == otherBooleanNode.value) && (this.priority.equals(otherBooleanNode.priority));
/*    */   }
/*    */   
/*    */   public int hashCode()
/*    */   {
/* 50 */     return (this.value ? 1 : 0) + this.priority.hashCode();
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/snapshot/BooleanNode.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */