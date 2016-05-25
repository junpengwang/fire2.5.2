/*    */ package com.firebase.client.snapshot;
/*    */ 
/*    */ import com.firebase.client.utilities.Utilities;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class StringNode
/*    */   extends LeafNode<StringNode>
/*    */ {
/*    */   private final String value;
/*    */   
/*    */   public StringNode(String value, Node priority)
/*    */   {
/* 15 */     super(priority);
/* 16 */     this.value = value;
/*    */   }
/*    */   
/*    */   public Object getValue()
/*    */   {
/* 21 */     return this.value;
/*    */   }
/*    */   
/*    */   public String getHashRepresentation(Node.HashVersion version)
/*    */   {
/* 26 */     switch (version) {
/*    */     case V1: 
/* 28 */       return getPriorityHash(version) + "string:" + this.value;
/*    */     case V2: 
/* 30 */       return getPriorityHash(version) + "string:" + Utilities.stringHashV2Representation(this.value);
/*    */     }
/*    */     
/* 33 */     throw new IllegalArgumentException("Invalid hash version for string node: " + version);
/*    */   }
/*    */   
/*    */ 
/*    */   public StringNode updatePriority(Node priority)
/*    */   {
/* 39 */     return new StringNode(this.value, priority);
/*    */   }
/*    */   
/*    */   protected LeafNode.LeafType getLeafType()
/*    */   {
/* 44 */     return LeafNode.LeafType.String;
/*    */   }
/*    */   
/*    */   protected int compareLeafValues(StringNode other)
/*    */   {
/* 49 */     return this.value.compareTo(other.value);
/*    */   }
/*    */   
/*    */   public boolean equals(Object other)
/*    */   {
/* 54 */     if (!(other instanceof StringNode)) {
/* 55 */       return false;
/*    */     }
/* 57 */     StringNode otherStringNode = (StringNode)other;
/* 58 */     return (this.value.equals(otherStringNode.value)) && (this.priority.equals(otherStringNode.priority));
/*    */   }
/*    */   
/*    */   public int hashCode()
/*    */   {
/* 63 */     return this.value.hashCode() + this.priority.hashCode();
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/snapshot/StringNode.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */