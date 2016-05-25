/*    */ package com.firebase.client.snapshot;
/*    */ 
/*    */ import com.firebase.client.core.Path;
/*    */ import java.util.Comparator;
/*    */ 
/*    */ public abstract class Index implements Comparator<NamedNode>
/*    */ {
/*    */   public abstract boolean isDefinedOn(Node paramNode);
/*    */   
/*    */   public boolean indexedValueChanged(Node oldNode, Node newNode)
/*    */   {
/* 12 */     NamedNode oldWrapped = new NamedNode(ChildKey.getMinName(), oldNode);
/* 13 */     NamedNode newWrapped = new NamedNode(ChildKey.getMinName(), newNode);
/* 14 */     return compare(oldWrapped, newWrapped) != 0;
/*    */   }
/*    */   
/*    */   public abstract NamedNode makePost(ChildKey paramChildKey, Node paramNode);
/*    */   
/*    */   public NamedNode minPost() {
/* 20 */     return NamedNode.getMinNode();
/*    */   }
/*    */   
/*    */   public abstract NamedNode maxPost();
/*    */   
/*    */   public abstract String getQueryDefinition();
/*    */   
/*    */   public static Index fromQueryDefinition(String str) {
/* 28 */     if (str.equals(".value"))
/* 29 */       return ValueIndex.getInstance();
/* 30 */     if (str.equals(".key"))
/* 31 */       return KeyIndex.getInstance();
/* 32 */     if (str.equals(".priority")) {
/* 33 */       throw new IllegalStateException("queryDefinition shouldn't ever be .priority since it's the default");
/*    */     }
/* 35 */     return new PathIndex(new Path(str));
/*    */   }
/*    */   
/*    */   public int compare(NamedNode one, NamedNode two, boolean reverse)
/*    */   {
/* 40 */     if (reverse) {
/* 41 */       return compare(two, one);
/*    */     }
/* 43 */     return compare(one, two);
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/snapshot/Index.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */