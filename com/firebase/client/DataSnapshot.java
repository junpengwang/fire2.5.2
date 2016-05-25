/*     */ package com.firebase.client;
/*     */ 
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.IndexedNode;
/*     */ import com.firebase.client.snapshot.NamedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import com.firebase.client.utilities.Validation;
/*     */ import com.firebase.client.utilities.encoding.JsonHelpers;
/*     */ import com.shaded.fasterxml.jackson.databind.ObjectMapper;
/*     */ import java.io.IOException;
/*     */ import java.util.Iterator;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class DataSnapshot
/*     */ {
/*     */   private final IndexedNode node;
/*     */   private final Firebase query;
/*     */   
/*     */   public DataSnapshot(Firebase ref, IndexedNode node)
/*     */   {
/*  42 */     this.node = node;
/*  43 */     this.query = ref;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public DataSnapshot child(String path)
/*     */   {
/*  55 */     Firebase childRef = this.query.child(path);
/*  56 */     Node childNode = this.node.getNode().getChild(new Path(path));
/*  57 */     return new DataSnapshot(childRef, IndexedNode.from(childNode));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean hasChild(String path)
/*     */   {
/*  66 */     if (this.query.getParent() == null) {
/*  67 */       Validation.validateRootPathString(path);
/*     */     } else {
/*  69 */       Validation.validatePathString(path);
/*     */     }
/*  71 */     return !this.node.getNode().getChild(new Path(path)).isEmpty();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean hasChildren()
/*     */   {
/*  79 */     return this.node.getNode().getChildCount() > 0;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public boolean exists()
/*     */   {
/*  88 */     return !this.node.getNode().isEmpty();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Object getValue()
/*     */   {
/* 108 */     return this.node.getNode().getValue();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Object getValue(boolean useExportFormat)
/*     */   {
/* 136 */     return this.node.getNode().getValue(useExportFormat);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public <T> T getValue(Class<T> valueType)
/*     */   {
/* 180 */     Object value = this.node.getNode().getValue();
/*     */     try {
/* 182 */       String json = JsonHelpers.getMapper().writeValueAsString(value);
/* 183 */       return (T)JsonHelpers.getMapper().readValue(json, valueType);
/*     */     } catch (IOException e) {
/* 185 */       throw new FirebaseException("Failed to bounce to type", e);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public <T> T getValue(GenericTypeIndicator<T> t)
/*     */   {
/* 205 */     Object value = this.node.getNode().getValue();
/*     */     try {
/* 207 */       String json = JsonHelpers.getMapper().writeValueAsString(value);
/* 208 */       return (T)JsonHelpers.getMapper().readValue(json, t);
/*     */     } catch (IOException e) {
/* 210 */       throw new FirebaseException("Failed to bounce to type", e);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public long getChildrenCount()
/*     */   {
/* 218 */     return this.node.getNode().getChildCount();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Firebase getRef()
/*     */   {
/* 226 */     return this.query;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public String getKey()
/*     */   {
/* 233 */     return this.query.getKey();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Iterable<DataSnapshot> getChildren()
/*     */   {
/* 247 */     final Iterator<NamedNode> iter = this.node.iterator();
/* 248 */     new Iterable()
/*     */     {
/*     */       public Iterator<DataSnapshot> iterator()
/*     */       {
/* 252 */         new Iterator()
/*     */         {
/*     */           public boolean hasNext() {
/* 255 */             return DataSnapshot.1.this.val$iter.hasNext();
/*     */           }
/*     */           
/*     */           public DataSnapshot next()
/*     */           {
/* 260 */             NamedNode namedNode = (NamedNode)DataSnapshot.1.this.val$iter.next();
/* 261 */             return new DataSnapshot(DataSnapshot.this.query.child(namedNode.getName().asString()), IndexedNode.from(namedNode.getNode()));
/*     */           }
/*     */           
/*     */           public void remove()
/*     */           {
/* 266 */             throw new UnsupportedOperationException("remove called on immutable collection");
/*     */           }
/*     */         };
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Object getPriority()
/*     */   {
/* 284 */     Object priority = this.node.getNode().getPriority().getValue();
/* 285 */     if ((priority instanceof Long)) {
/* 286 */       return Double.valueOf(((Long)priority).longValue());
/*     */     }
/* 288 */     return priority;
/*     */   }
/*     */   
/*     */ 
/*     */   public String toString()
/*     */   {
/* 294 */     return "DataSnapshot { key = " + this.query.getKey() + ", value = " + this.node.getNode().getValue(true) + " }";
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/DataSnapshot.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */