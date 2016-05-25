/*     */ package com.firebase.client.snapshot;
/*     */ 
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.utilities.Utilities;
/*     */ import java.util.Collections;
/*     */ import java.util.HashMap;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public abstract class LeafNode<T extends LeafNode>
/*     */   implements Node
/*     */ {
/*     */   protected final Node priority;
/*     */   private String lazyHash;
/*     */   
/*     */   protected static enum LeafType
/*     */   {
/*  22 */     DeferredValue,  Boolean,  Number,  String;
/*     */     
/*     */     private LeafType() {}
/*     */   }
/*     */   
/*     */   LeafNode(Node priority)
/*     */   {
/*  29 */     this.priority = priority;
/*     */   }
/*     */   
/*     */   public boolean hasChild(ChildKey childKey)
/*     */   {
/*  34 */     return false;
/*     */   }
/*     */   
/*     */   public boolean isLeafNode()
/*     */   {
/*  39 */     return true;
/*     */   }
/*     */   
/*     */   public Node getPriority()
/*     */   {
/*  44 */     return this.priority;
/*     */   }
/*     */   
/*     */   public Node getChild(Path path)
/*     */   {
/*  49 */     if (path.isEmpty())
/*  50 */       return this;
/*  51 */     if (path.getFront().isPriorityChildName()) {
/*  52 */       return this.priority;
/*     */     }
/*  54 */     return EmptyNode.Empty();
/*     */   }
/*     */   
/*     */ 
/*     */   public Node updateChild(Path path, Node node)
/*     */   {
/*  60 */     ChildKey front = path.getFront();
/*  61 */     if (front == null)
/*  62 */       return node;
/*  63 */     if ((node.isEmpty()) && (!front.isPriorityChildName())) {
/*  64 */       return this;
/*     */     }
/*  66 */     assert ((!path.getFront().isPriorityChildName()) || (path.size() == 1));
/*  67 */     return updateImmediateChild(front, EmptyNode.Empty().updateChild(path.popFront(), node));
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean isEmpty()
/*     */   {
/*  73 */     return false;
/*     */   }
/*     */   
/*     */   public int getChildCount()
/*     */   {
/*  78 */     return 0;
/*     */   }
/*     */   
/*     */   public ChildKey getPredecessorChildKey(ChildKey childKey)
/*     */   {
/*  83 */     return null;
/*     */   }
/*     */   
/*     */   public ChildKey getSuccessorChildKey(ChildKey childKey)
/*     */   {
/*  88 */     return null;
/*     */   }
/*     */   
/*     */   public Node getImmediateChild(ChildKey name)
/*     */   {
/*  93 */     if (name.isPriorityChildName()) {
/*  94 */       return this.priority;
/*     */     }
/*  96 */     return EmptyNode.Empty();
/*     */   }
/*     */   
/*     */ 
/*     */   public Object getValue(boolean useExportFormat)
/*     */   {
/* 102 */     if ((!useExportFormat) || (this.priority.isEmpty())) {
/* 103 */       return getValue();
/*     */     }
/* 105 */     Map<String, Object> result = new HashMap();
/* 106 */     result.put(".value", getValue());
/* 107 */     result.put(".priority", this.priority.getValue());
/* 108 */     return result;
/*     */   }
/*     */   
/*     */ 
/*     */   public Node updateImmediateChild(ChildKey name, Node node)
/*     */   {
/* 114 */     if (name.isPriorityChildName())
/* 115 */       return updatePriority(node);
/* 116 */     if (node.isEmpty()) {
/* 117 */       return this;
/*     */     }
/* 119 */     return EmptyNode.Empty().updateImmediateChild(name, node).updatePriority(this.priority);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public String getHash()
/*     */   {
/* 127 */     if (this.lazyHash == null) {
/* 128 */       this.lazyHash = Utilities.sha1HexDigest(getHashRepresentation(Node.HashVersion.V1));
/*     */     }
/* 130 */     return this.lazyHash;
/*     */   }
/*     */   
/*     */   protected String getPriorityHash(Node.HashVersion version) {
/* 134 */     switch (version) {
/*     */     case V1: 
/*     */     case V2: 
/* 137 */       if (this.priority.isEmpty()) {
/* 138 */         return "";
/*     */       }
/* 140 */       return "priority:" + this.priority.getHashRepresentation(version) + ":";
/*     */     }
/*     */     
/* 143 */     throw new IllegalArgumentException("Unknown hash version: " + version);
/*     */   }
/*     */   
/*     */ 
/*     */   protected abstract LeafType getLeafType();
/*     */   
/*     */   public Iterator<NamedNode> iterator()
/*     */   {
/* 151 */     return Collections.emptyList().iterator();
/*     */   }
/*     */   
/*     */   public Iterator<NamedNode> reverseIterator()
/*     */   {
/* 156 */     return Collections.emptyList().iterator();
/*     */   }
/*     */   
/*     */   private static int compareLongDoubleNodes(LongNode longNode, DoubleNode doubleNode) {
/* 160 */     Double longDoubleValue = Double.valueOf(((Long)longNode.getValue()).longValue());
/* 161 */     return longDoubleValue.compareTo((Double)doubleNode.getValue());
/*     */   }
/*     */   
/*     */   public int compareTo(Node other)
/*     */   {
/* 166 */     if (other.isEmpty())
/* 167 */       return 1;
/* 168 */     if ((other instanceof ChildrenNode)) {
/* 169 */       return -1;
/*     */     }
/* 171 */     assert (other.isLeafNode()) : "Node is not leaf node!";
/* 172 */     if (((this instanceof LongNode)) && ((other instanceof DoubleNode)))
/* 173 */       return compareLongDoubleNodes((LongNode)this, (DoubleNode)other);
/* 174 */     if (((this instanceof DoubleNode)) && ((other instanceof LongNode))) {
/* 175 */       return -1 * compareLongDoubleNodes((LongNode)other, (DoubleNode)this);
/*     */     }
/* 177 */     return leafCompare((LeafNode)other);
/*     */   }
/*     */   
/*     */ 
/*     */   protected abstract int compareLeafValues(T paramT);
/*     */   
/*     */   protected int leafCompare(LeafNode<?> other)
/*     */   {
/* 185 */     LeafType thisLeafType = getLeafType();
/* 186 */     LeafType otherLeafType = other.getLeafType();
/* 187 */     if (thisLeafType.equals(otherLeafType))
/*     */     {
/* 189 */       int value = compareLeafValues(other);
/* 190 */       return value;
/*     */     }
/* 192 */     return thisLeafType.compareTo(otherLeafType);
/*     */   }
/*     */   
/*     */ 
/*     */   public abstract boolean equals(Object paramObject);
/*     */   
/*     */ 
/*     */   public abstract int hashCode();
/*     */   
/*     */ 
/*     */   public String toString()
/*     */   {
/* 204 */     String str = getValue(true).toString();
/* 205 */     return str.substring(0, 100) + "...";
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/snapshot/LeafNode.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */