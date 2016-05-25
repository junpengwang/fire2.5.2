/*     */ package com.firebase.client.snapshot;
/*     */ 
/*     */ import com.firebase.client.collection.ImmutableSortedMap;
/*     */ import com.firebase.client.collection.LLRBNode.NodeVisitor;
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.utilities.Utilities;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.Comparator;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Map.Entry;
/*     */ 
/*     */ public class ChildrenNode implements Node
/*     */ {
/*  17 */   public static Comparator<ChildKey> NAME_ONLY_COMPARATOR = new Comparator()
/*     */   {
/*     */     public int compare(ChildKey o1, ChildKey o2) {
/*  20 */       return o1.compareTo(o2);
/*     */     }
/*     */   };
/*     */   
/*     */   private final ImmutableSortedMap<ChildKey, Node> children;
/*     */   
/*     */   private final Node priority;
/*  27 */   private String lazyHash = null;
/*     */   
/*     */   private static class NamedNodeIterator implements Iterator<NamedNode>
/*     */   {
/*     */     private final Iterator<Map.Entry<ChildKey, Node>> iterator;
/*     */     
/*     */     public NamedNodeIterator(Iterator<Map.Entry<ChildKey, Node>> iterator) {
/*  34 */       this.iterator = iterator;
/*     */     }
/*     */     
/*     */     public boolean hasNext()
/*     */     {
/*  39 */       return this.iterator.hasNext();
/*     */     }
/*     */     
/*     */     public NamedNode next()
/*     */     {
/*  44 */       Map.Entry<ChildKey, Node> entry = (Map.Entry)this.iterator.next();
/*  45 */       return new NamedNode((ChildKey)entry.getKey(), (Node)entry.getValue());
/*     */     }
/*     */     
/*     */     public void remove()
/*     */     {
/*  50 */       this.iterator.remove();
/*     */     }
/*     */   }
/*     */   
/*     */   public static abstract class ChildVisitor extends LLRBNode.NodeVisitor<ChildKey, Node>
/*     */   {
/*     */     public void visitEntry(ChildKey key, Node value)
/*     */     {
/*  58 */       visitChild(key, value);
/*     */     }
/*     */     
/*     */     public abstract void visitChild(ChildKey paramChildKey, Node paramNode);
/*     */   }
/*     */   
/*     */   protected ChildrenNode() {
/*  65 */     this.children = com.firebase.client.collection.ImmutableSortedMap.Builder.emptyMap(NAME_ONLY_COMPARATOR);
/*  66 */     this.priority = PriorityUtilities.NullPriority();
/*     */   }
/*     */   
/*     */   protected ChildrenNode(ImmutableSortedMap<ChildKey, Node> children, Node priority) {
/*  70 */     if ((children.isEmpty()) && (!priority.isEmpty())) {
/*  71 */       throw new IllegalArgumentException("Can't create empty ChildrenNode with priority!");
/*     */     }
/*  73 */     this.priority = priority;
/*  74 */     this.children = children;
/*     */   }
/*     */   
/*     */   public boolean hasChild(ChildKey name)
/*     */   {
/*  79 */     return !getImmediateChild(name).isEmpty();
/*     */   }
/*     */   
/*     */   public boolean isEmpty()
/*     */   {
/*  84 */     return this.children.isEmpty();
/*     */   }
/*     */   
/*     */   public int getChildCount()
/*     */   {
/*  89 */     return this.children.size();
/*     */   }
/*     */   
/*     */   public Object getValue()
/*     */   {
/*  94 */     return getValue(false);
/*     */   }
/*     */   
/*     */   public Object getValue(boolean useExportFormat)
/*     */   {
/*  99 */     if (isEmpty()) {
/* 100 */       return null;
/*     */     }
/*     */     
/* 103 */     int numKeys = 0;
/* 104 */     int maxKey = 0;
/* 105 */     boolean allIntegerKeys = true;
/* 106 */     Map<String, Object> result = new java.util.HashMap();
/* 107 */     for (Map.Entry<ChildKey, Node> entry : this.children) {
/* 108 */       String key = ((ChildKey)entry.getKey()).asString();
/* 109 */       result.put(key, ((Node)entry.getValue()).getValue(useExportFormat));
/* 110 */       numKeys++;
/*     */       
/* 112 */       if (allIntegerKeys) {
/* 113 */         if ((key.length() > 1) && (key.charAt(0) == '0')) {
/* 114 */           allIntegerKeys = false;
/*     */         } else {
/* 116 */           Integer keyAsInt = Utilities.tryParseInt(key);
/* 117 */           if ((keyAsInt != null) && (keyAsInt.intValue() >= 0)) {
/* 118 */             if (keyAsInt.intValue() > maxKey) {
/* 119 */               maxKey = keyAsInt.intValue();
/*     */             }
/*     */           } else {
/* 122 */             allIntegerKeys = false;
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */     
/* 128 */     if ((!useExportFormat) && (allIntegerKeys) && (maxKey < 2 * numKeys))
/*     */     {
/* 130 */       List<Object> arrayResult = new ArrayList(maxKey + 1);
/* 131 */       for (int i = 0; i <= maxKey; i++)
/*     */       {
/*     */ 
/* 134 */         arrayResult.add(result.get("" + i));
/*     */       }
/* 136 */       return arrayResult;
/*     */     }
/* 138 */     if ((useExportFormat) && (!this.priority.isEmpty())) {
/* 139 */       result.put(".priority", this.priority.getValue());
/*     */     }
/* 141 */     return result;
/*     */   }
/*     */   
/*     */ 
/*     */   public ChildKey getPredecessorChildKey(ChildKey childKey)
/*     */   {
/* 147 */     return (ChildKey)this.children.getPredecessorKey(childKey);
/*     */   }
/*     */   
/*     */   public ChildKey getSuccessorChildKey(ChildKey childKey)
/*     */   {
/* 152 */     return (ChildKey)this.children.getSuccessorKey(childKey);
/*     */   }
/*     */   
/*     */   public String getHashRepresentation(Node.HashVersion version)
/*     */   {
/* 157 */     if (version != Node.HashVersion.V1) {
/* 158 */       throw new IllegalArgumentException("Hashes on children nodes only supported for V1");
/*     */     }
/* 160 */     StringBuilder toHash = new StringBuilder();
/* 161 */     if (!this.priority.isEmpty()) {
/* 162 */       toHash.append("priority:");
/* 163 */       toHash.append(this.priority.getHashRepresentation(Node.HashVersion.V1));
/* 164 */       toHash.append(":");
/*     */     }
/* 166 */     List<NamedNode> nodes = new ArrayList();
/* 167 */     boolean sawPriority = false;
/* 168 */     for (NamedNode node : this) {
/* 169 */       nodes.add(node);
/* 170 */       sawPriority = (sawPriority) || (!node.getNode().getPriority().isEmpty());
/*     */     }
/* 172 */     if (sawPriority) {
/* 173 */       Collections.sort(nodes, PriorityIndex.getInstance());
/*     */     }
/* 175 */     for (NamedNode node : nodes) {
/* 176 */       String hashString = node.getNode().getHash();
/* 177 */       if (!hashString.equals("")) {
/* 178 */         toHash.append(":");
/* 179 */         toHash.append(node.getName().asString());
/* 180 */         toHash.append(":");
/* 181 */         toHash.append(hashString);
/*     */       }
/*     */     }
/* 184 */     return toHash.toString();
/*     */   }
/*     */   
/*     */   public String getHash()
/*     */   {
/* 189 */     if (this.lazyHash == null) {
/* 190 */       String hashString = getHashRepresentation(Node.HashVersion.V1);
/* 191 */       this.lazyHash = (hashString.isEmpty() ? "" : Utilities.sha1HexDigest(hashString));
/*     */     }
/* 193 */     return this.lazyHash;
/*     */   }
/*     */   
/*     */   public boolean isLeafNode()
/*     */   {
/* 198 */     return false;
/*     */   }
/*     */   
/*     */   public Node getPriority()
/*     */   {
/* 203 */     return this.priority;
/*     */   }
/*     */   
/*     */   public Node updatePriority(Node priority)
/*     */   {
/* 208 */     if (this.children.isEmpty()) {
/* 209 */       return EmptyNode.Empty();
/*     */     }
/* 211 */     return new ChildrenNode(this.children, priority);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public Node getImmediateChild(ChildKey name)
/*     */   {
/* 218 */     if ((name.isPriorityChildName()) && (!this.priority.isEmpty()))
/* 219 */       return this.priority;
/* 220 */     if (this.children.containsKey(name)) {
/* 221 */       return (Node)this.children.get(name);
/*     */     }
/* 223 */     return EmptyNode.Empty();
/*     */   }
/*     */   
/*     */ 
/*     */   public Node getChild(Path path)
/*     */   {
/* 229 */     ChildKey front = path.getFront();
/* 230 */     if (front == null) {
/* 231 */       return this;
/*     */     }
/* 233 */     return getImmediateChild(front).getChild(path.popFront());
/*     */   }
/*     */   
/*     */   public void forEachChild(ChildVisitor visitor)
/*     */   {
/* 238 */     forEachChild(visitor, false);
/*     */   }
/*     */   
/*     */   public void forEachChild(final ChildVisitor visitor, boolean includePriority) {
/* 242 */     if ((!includePriority) || (getPriority().isEmpty())) {
/* 243 */       this.children.inOrderTraversal(visitor);
/*     */     } else {
/* 245 */       this.children.inOrderTraversal(new LLRBNode.NodeVisitor() {
/* 246 */         boolean passedPriorityKey = false;
/*     */         
/*     */         public void visitEntry(ChildKey key, Node value) {
/* 249 */           if ((!this.passedPriorityKey) && (key.compareTo(ChildKey.getPriorityKey()) > 0)) {
/* 250 */             this.passedPriorityKey = true;
/* 251 */             visitor.visitChild(ChildKey.getPriorityKey(), ChildrenNode.this.getPriority());
/*     */           }
/* 253 */           visitor.visitChild(key, value);
/*     */         }
/*     */       });
/*     */     }
/*     */   }
/*     */   
/*     */   public ChildKey getFirstChildKey() {
/* 260 */     return (ChildKey)this.children.getMinKey();
/*     */   }
/*     */   
/*     */   public ChildKey getLastChildKey() {
/* 264 */     return (ChildKey)this.children.getMaxKey();
/*     */   }
/*     */   
/*     */   public Node updateChild(Path path, Node newChildNode)
/*     */   {
/* 269 */     ChildKey front = path.getFront();
/* 270 */     if (front == null)
/* 271 */       return newChildNode;
/* 272 */     if (front.isPriorityChildName()) {
/* 273 */       assert (PriorityUtilities.isValidPriority(newChildNode));
/* 274 */       return updatePriority(newChildNode);
/*     */     }
/* 276 */     Node newImmediateChild = getImmediateChild(front).updateChild(path.popFront(), newChildNode);
/* 277 */     return updateImmediateChild(front, newImmediateChild);
/*     */   }
/*     */   
/*     */ 
/*     */   public Iterator<NamedNode> iterator()
/*     */   {
/* 283 */     return new NamedNodeIterator(this.children.iterator());
/*     */   }
/*     */   
/*     */   public Iterator<NamedNode> reverseIterator() {
/* 287 */     return new NamedNodeIterator(this.children.reverseIterator());
/*     */   }
/*     */   
/*     */   public Node updateImmediateChild(ChildKey key, Node newChildNode)
/*     */   {
/* 292 */     if (key.isPriorityChildName()) {
/* 293 */       return updatePriority(newChildNode);
/*     */     }
/* 295 */     ImmutableSortedMap<ChildKey, Node> newChildren = this.children;
/* 296 */     if (newChildren.containsKey(key)) {
/* 297 */       newChildren = newChildren.remove(key);
/*     */     }
/* 299 */     if (!newChildNode.isEmpty()) {
/* 300 */       newChildren = newChildren.insert(key, newChildNode);
/*     */     }
/* 302 */     if (newChildren.isEmpty())
/*     */     {
/* 304 */       return EmptyNode.Empty();
/*     */     }
/* 306 */     return new ChildrenNode(newChildren, this.priority);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public int compareTo(Node o)
/*     */   {
/* 313 */     if (isEmpty()) {
/* 314 */       if (o.isEmpty()) {
/* 315 */         return 0;
/*     */       }
/* 317 */       return -1;
/*     */     }
/* 319 */     if (o.isLeafNode())
/*     */     {
/* 321 */       return 1; }
/* 322 */     if (o.isEmpty())
/* 323 */       return 1;
/* 324 */     if (o == Node.MAX_NODE) {
/* 325 */       return -1;
/*     */     }
/*     */     
/* 328 */     return 0;
/*     */   }
/*     */   
/*     */ 
/*     */   public boolean equals(Object otherObj)
/*     */   {
/* 334 */     if (otherObj == null) {
/* 335 */       return false;
/*     */     }
/* 337 */     if (otherObj == this) {
/* 338 */       return true;
/*     */     }
/* 340 */     if (!(otherObj instanceof ChildrenNode)) {
/* 341 */       return false;
/*     */     }
/* 343 */     ChildrenNode other = (ChildrenNode)otherObj;
/* 344 */     if (!getPriority().equals(other.getPriority()))
/* 345 */       return false;
/* 346 */     if (this.children.size() != other.children.size()) {
/* 347 */       return false;
/*     */     }
/* 349 */     Iterator<Map.Entry<ChildKey, Node>> thisIterator = this.children.iterator();
/* 350 */     Iterator<Map.Entry<ChildKey, Node>> otherIterator = other.children.iterator();
/* 351 */     while ((thisIterator.hasNext()) && (otherIterator.hasNext())) {
/* 352 */       Map.Entry<ChildKey, Node> thisNameNode = (Map.Entry)thisIterator.next();
/* 353 */       Map.Entry<ChildKey, Node> otherNamedNode = (Map.Entry)otherIterator.next();
/* 354 */       if ((!((ChildKey)thisNameNode.getKey()).equals(otherNamedNode.getKey())) || (!((Node)thisNameNode.getValue()).equals(otherNamedNode.getValue())))
/*     */       {
/* 356 */         return false;
/*     */       }
/*     */     }
/* 359 */     if ((thisIterator.hasNext()) || (otherIterator.hasNext())) {
/* 360 */       throw new IllegalStateException("Something went wrong internally.");
/*     */     }
/* 362 */     return true;
/*     */   }
/*     */   
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 368 */     int hashCode = 0;
/* 369 */     for (NamedNode entry : this) {
/* 370 */       hashCode = 31 * hashCode + entry.getName().hashCode();
/* 371 */       hashCode = 17 * hashCode + entry.getNode().hashCode();
/*     */     }
/* 373 */     return hashCode;
/*     */   }
/*     */   
/*     */   public String toString()
/*     */   {
/* 378 */     StringBuilder builder = new StringBuilder();
/* 379 */     toString(builder, 0);
/* 380 */     return builder.toString();
/*     */   }
/*     */   
/*     */   private static void addIndentation(StringBuilder builder, int indentation) {
/* 384 */     for (int i = 0; i < indentation; i++) {
/* 385 */       builder.append(" ");
/*     */     }
/*     */   }
/*     */   
/*     */   private void toString(StringBuilder builder, int indentation) {
/* 390 */     if ((this.children.isEmpty()) && (this.priority.isEmpty())) {
/* 391 */       builder.append("{ }");
/*     */     } else {
/* 393 */       builder.append("{\n");
/* 394 */       for (Map.Entry<ChildKey, Node> childEntry : this.children) {
/* 395 */         addIndentation(builder, indentation + 2);
/* 396 */         builder.append(((ChildKey)childEntry.getKey()).asString());
/* 397 */         builder.append("=");
/* 398 */         if ((childEntry.getValue() instanceof ChildrenNode)) {
/* 399 */           ChildrenNode childrenNode = (ChildrenNode)childEntry.getValue();
/* 400 */           childrenNode.toString(builder, indentation + 2);
/*     */         } else {
/* 402 */           builder.append(((Node)childEntry.getValue()).toString());
/*     */         }
/* 404 */         builder.append("\n");
/*     */       }
/* 406 */       if (!this.priority.isEmpty()) {
/* 407 */         addIndentation(builder, indentation + 2);
/* 408 */         builder.append(".priority=");
/* 409 */         builder.append(this.priority.toString());
/* 410 */         builder.append("\n");
/*     */       }
/* 412 */       addIndentation(builder, indentation);
/* 413 */       builder.append("}");
/*     */     }
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/snapshot/ChildrenNode.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */