/*     */ package com.firebase.client.core;
/*     */ 
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.ChildrenNode;
/*     */ import com.firebase.client.snapshot.ChildrenNode.ChildVisitor;
/*     */ import com.firebase.client.snapshot.LeafNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import com.firebase.client.snapshot.Node.HashVersion;
/*     */ import com.firebase.client.utilities.NodeSizeEstimator;
/*     */ import com.firebase.client.utilities.Utilities;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.List;
/*     */ import java.util.Stack;
/*     */ 
/*     */ public class CompoundHash
/*     */ {
/*     */   private final List<Path> posts;
/*     */   private final List<String> hashes;
/*     */   
/*     */   private CompoundHash(List<Path> posts, List<String> hashes)
/*     */   {
/*  23 */     if (posts.size() != hashes.size() - 1) {
/*  24 */       throw new IllegalArgumentException("Number of posts need to be n-1 for n hashes in CompoundHash");
/*     */     }
/*  26 */     this.posts = posts;
/*  27 */     this.hashes = hashes;
/*     */   }
/*     */   
/*     */   public List<Path> getPosts() {
/*  31 */     return Collections.unmodifiableList(this.posts);
/*     */   }
/*     */   
/*     */   public List<String> getHashes() {
/*  35 */     return Collections.unmodifiableList(this.hashes);
/*     */   }
/*     */   
/*     */   public static abstract interface SplitStrategy
/*     */   {
/*     */     public abstract boolean shouldSplit(CompoundHash.CompoundHashBuilder paramCompoundHashBuilder);
/*     */   }
/*     */   
/*     */   private static class SimpleSizeSplitStrategy implements CompoundHash.SplitStrategy {
/*     */     private final long splitThreshold;
/*     */     
/*     */     public SimpleSizeSplitStrategy(Node node) {
/*  47 */       long estimatedNodeSize = NodeSizeEstimator.estimateSerializedNodeSize(node);
/*     */       
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  54 */       this.splitThreshold = Math.max(512L, Math.sqrt(estimatedNodeSize * 100L));
/*     */     }
/*     */     
/*     */     public boolean shouldSplit(CompoundHash.CompoundHashBuilder state)
/*     */     {
/*  59 */       return (state.currentHashLength() > this.splitThreshold) && ((state.currentPath().isEmpty()) || (!state.currentPath().getBack().equals(ChildKey.getPriorityKey())));
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   static class CompoundHashBuilder
/*     */   {
/*  66 */     private StringBuilder optHashValueBuilder = null;
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  72 */     private Stack<ChildKey> currentPath = new Stack();
/*  73 */     private int lastLeafDepth = -1;
/*     */     
/*     */     private int currentPathDepth;
/*  76 */     private boolean needsComma = true;
/*     */     
/*  78 */     private final List<Path> currentPaths = new ArrayList();
/*  79 */     private final List<String> currentHashes = new ArrayList();
/*     */     private final CompoundHash.SplitStrategy splitStrategy;
/*     */     
/*     */     public CompoundHashBuilder(CompoundHash.SplitStrategy strategy) {
/*  83 */       this.splitStrategy = strategy;
/*     */     }
/*     */     
/*     */     public boolean buildingRange() {
/*  87 */       return this.optHashValueBuilder != null;
/*     */     }
/*     */     
/*     */     public int currentHashLength() {
/*  91 */       return this.optHashValueBuilder.length();
/*     */     }
/*     */     
/*     */     public Path currentPath() {
/*  95 */       return currentPath(this.currentPathDepth);
/*     */     }
/*     */     
/*     */     private Path currentPath(int depth) {
/*  99 */       ChildKey[] segments = new ChildKey[depth];
/* 100 */       for (int i = 0; i < depth; i++) {
/* 101 */         segments[i] = ((ChildKey)this.currentPath.get(i));
/*     */       }
/* 103 */       return new Path(segments);
/*     */     }
/*     */     
/*     */     private void ensureRange() {
/* 107 */       if (!buildingRange()) {
/* 108 */         this.optHashValueBuilder = new StringBuilder();
/* 109 */         this.optHashValueBuilder.append("(");
/* 110 */         for (ChildKey key : currentPath(this.currentPathDepth)) {
/* 111 */           appendKey(this.optHashValueBuilder, key);
/* 112 */           this.optHashValueBuilder.append(":(");
/*     */         }
/* 114 */         this.needsComma = false;
/*     */       }
/*     */     }
/*     */     
/*     */     private void appendKey(StringBuilder builder, ChildKey key) {
/* 119 */       builder.append(Utilities.stringHashV2Representation(key.asString()));
/*     */     }
/*     */     
/*     */     private void processLeaf(LeafNode<?> node) {
/* 123 */       ensureRange();
/*     */       
/* 125 */       this.lastLeafDepth = this.currentPathDepth;
/* 126 */       this.optHashValueBuilder.append(node.getHashRepresentation(Node.HashVersion.V2));
/* 127 */       this.needsComma = true;
/* 128 */       if (this.splitStrategy.shouldSplit(this)) {
/* 129 */         endRange();
/*     */       }
/*     */     }
/*     */     
/*     */     private void startChild(ChildKey key) {
/* 134 */       ensureRange();
/*     */       
/* 136 */       if (this.needsComma) {
/* 137 */         this.optHashValueBuilder.append(",");
/*     */       }
/* 139 */       appendKey(this.optHashValueBuilder, key);
/* 140 */       this.optHashValueBuilder.append(":(");
/*     */       
/* 142 */       if (this.currentPathDepth == this.currentPath.size()) {
/* 143 */         this.currentPath.add(key);
/*     */       } else {
/* 145 */         this.currentPath.set(this.currentPathDepth, key);
/*     */       }
/* 147 */       this.currentPathDepth += 1;
/* 148 */       this.needsComma = false;
/*     */     }
/*     */     
/*     */     private void endChild() {
/* 152 */       this.currentPathDepth -= 1;
/* 153 */       if (buildingRange()) {
/* 154 */         this.optHashValueBuilder.append(")");
/*     */       }
/* 156 */       this.needsComma = true;
/*     */     }
/*     */     
/*     */     private void finishHashing() {
/* 160 */       Utilities.hardAssert(this.currentPathDepth == 0, "Can't finish hashing in the middle processing a child");
/* 161 */       if (buildingRange()) {
/* 162 */         endRange();
/*     */       }
/*     */       
/* 165 */       this.currentHashes.add("");
/*     */     }
/*     */     
/*     */     private void endRange() {
/* 169 */       Utilities.hardAssert(buildingRange(), "Can't end range without starting a range!");
/*     */       
/* 171 */       for (int i = 0; i < this.currentPathDepth; i++) {
/* 172 */         this.optHashValueBuilder.append(")");
/*     */       }
/* 174 */       this.optHashValueBuilder.append(")");
/*     */       
/* 176 */       Path lastLeafPath = currentPath(this.lastLeafDepth);
/* 177 */       String hash = Utilities.sha1HexDigest(this.optHashValueBuilder.toString());
/* 178 */       this.currentHashes.add(hash);
/* 179 */       this.currentPaths.add(lastLeafPath);
/*     */       
/* 181 */       this.optHashValueBuilder = null;
/*     */     }
/*     */   }
/*     */   
/*     */   public static CompoundHash fromNode(Node node) {
/* 186 */     return fromNode(node, new SimpleSizeSplitStrategy(node));
/*     */   }
/*     */   
/*     */   public static CompoundHash fromNode(Node node, SplitStrategy strategy) {
/* 190 */     if (node.isEmpty()) {
/* 191 */       return new CompoundHash(Collections.emptyList(), Collections.singletonList(""));
/*     */     }
/* 193 */     CompoundHashBuilder state = new CompoundHashBuilder(strategy);
/* 194 */     processNode(node, state);
/* 195 */     state.finishHashing();
/* 196 */     return new CompoundHash(state.currentPaths, state.currentHashes);
/*     */   }
/*     */   
/*     */   private static void processNode(Node node, CompoundHashBuilder state)
/*     */   {
/* 201 */     if (node.isLeafNode()) {
/* 202 */       state.processLeaf((LeafNode)node);
/* 203 */     } else { if (node.isEmpty()) {
/* 204 */         throw new IllegalArgumentException("Can't calculate hash on empty node!");
/*     */       }
/* 206 */       if (!(node instanceof ChildrenNode)) {
/* 207 */         throw new IllegalStateException("Expected children node, but got: " + node);
/*     */       }
/* 209 */       ChildrenNode childrenNode = (ChildrenNode)node;
/* 210 */       ChildrenNode.ChildVisitor visitor = new ChildrenNode.ChildVisitor()
/*     */       {
/*     */         public void visitChild(ChildKey name, Node child) {
/* 213 */           this.val$state.startChild(name);
/* 214 */           CompoundHash.processNode(child, this.val$state);
/* 215 */           this.val$state.endChild();
/*     */         }
/* 217 */       };
/* 218 */       childrenNode.forEachChild(visitor, true);
/*     */     }
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/CompoundHash.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */