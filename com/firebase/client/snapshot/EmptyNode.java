/*     */ package com.firebase.client.snapshot;
/*     */ 
/*     */ import com.firebase.client.core.Path;
/*     */ import java.util.Collections;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ 
/*     */ public class EmptyNode
/*     */   extends ChildrenNode implements Node
/*     */ {
/*  11 */   private static final EmptyNode empty = new EmptyNode();
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static EmptyNode Empty()
/*     */   {
/*  18 */     return empty;
/*     */   }
/*     */   
/*     */   public boolean isLeafNode()
/*     */   {
/*  23 */     return false;
/*     */   }
/*     */   
/*     */   public Node getPriority()
/*     */   {
/*  28 */     return this;
/*     */   }
/*     */   
/*     */   public Node getChild(Path path)
/*     */   {
/*  33 */     return this;
/*     */   }
/*     */   
/*     */   public Node getImmediateChild(ChildKey name)
/*     */   {
/*  38 */     return this;
/*     */   }
/*     */   
/*     */   public Node updateImmediateChild(ChildKey name, Node node)
/*     */   {
/*  43 */     if (node.isEmpty())
/*  44 */       return this;
/*  45 */     if (name.isPriorityChildName())
/*     */     {
/*  47 */       return this;
/*     */     }
/*  49 */     return new ChildrenNode().updateImmediateChild(name, node);
/*     */   }
/*     */   
/*     */ 
/*     */   public Node updateChild(Path path, Node node)
/*     */   {
/*  55 */     if (path.isEmpty()) {
/*  56 */       return node;
/*     */     }
/*  58 */     ChildKey name = path.getFront();
/*  59 */     Node newImmediateChild = getImmediateChild(name).updateChild(path.popFront(), node);
/*  60 */     return updateImmediateChild(name, newImmediateChild);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public EmptyNode updatePriority(Node priority)
/*     */   {
/*  67 */     return this;
/*     */   }
/*     */   
/*     */   public boolean hasChild(ChildKey name)
/*     */   {
/*  72 */     return false;
/*     */   }
/*     */   
/*     */   public boolean isEmpty()
/*     */   {
/*  77 */     return true;
/*     */   }
/*     */   
/*     */   public int getChildCount()
/*     */   {
/*  82 */     return 0;
/*     */   }
/*     */   
/*     */   public Object getValue()
/*     */   {
/*  87 */     return null;
/*     */   }
/*     */   
/*     */   public Object getValue(boolean useExportFormat)
/*     */   {
/*  92 */     return null;
/*     */   }
/*     */   
/*     */   public ChildKey getPredecessorChildKey(ChildKey childKey)
/*     */   {
/*  97 */     return null;
/*     */   }
/*     */   
/*     */   public ChildKey getSuccessorChildKey(ChildKey childKey)
/*     */   {
/* 102 */     return null;
/*     */   }
/*     */   
/*     */   public String getHash()
/*     */   {
/* 107 */     return "";
/*     */   }
/*     */   
/*     */   public String getHashRepresentation(Node.HashVersion version)
/*     */   {
/* 112 */     return "";
/*     */   }
/*     */   
/*     */   public Iterator<NamedNode> iterator()
/*     */   {
/* 117 */     return Collections.emptyList().iterator();
/*     */   }
/*     */   
/*     */   public Iterator<NamedNode> reverseIterator()
/*     */   {
/* 122 */     return Collections.emptyList().iterator();
/*     */   }
/*     */   
/*     */   public int compareTo(Node o)
/*     */   {
/* 127 */     return o.isEmpty() ? 0 : -1;
/*     */   }
/*     */   
/*     */   public boolean equals(Object o)
/*     */   {
/* 132 */     if ((o instanceof EmptyNode))
/*     */     {
/* 134 */       return true;
/*     */     }
/*     */     
/* 137 */     return ((o instanceof Node)) && (((Node)o).isEmpty()) && (getPriority().equals(((Node)o).getPriority()));
/*     */   }
/*     */   
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 143 */     return 0;
/*     */   }
/*     */   
/*     */   public String toString()
/*     */   {
/* 148 */     return "<Empty Node>";
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/snapshot/EmptyNode.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */