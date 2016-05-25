/*     */ package com.firebase.client;
/*     */ 
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.core.Repo;
/*     */ import com.firebase.client.core.ValidationPath;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import com.firebase.client.snapshot.NodeUtilities;
/*     */ import com.firebase.client.snapshot.PriorityUtilities;
/*     */ import com.firebase.client.utilities.Validation;
/*     */ import com.firebase.client.utilities.encoding.JsonHelpers;
/*     */ import com.shaded.fasterxml.jackson.databind.ObjectMapper;
/*     */ import java.util.Map;
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
/*     */ public class OnDisconnect
/*     */ {
/*     */   private Repo repo;
/*     */   private Path path;
/*     */   
/*     */   OnDisconnect(Repo repo, Path path)
/*     */   {
/*  39 */     this.repo = repo;
/*  40 */     this.path = path;
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
/*     */   public void setValue(Object value)
/*     */   {
/*  54 */     onDisconnectSetInternal(value, PriorityUtilities.NullPriority(), null);
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
/*     */   public void setValue(Object value, String priority)
/*     */   {
/*  69 */     onDisconnectSetInternal(value, PriorityUtilities.parsePriority(priority), null);
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
/*     */   public void setValue(Object value, double priority)
/*     */   {
/*  84 */     onDisconnectSetInternal(value, PriorityUtilities.parsePriority(Double.valueOf(priority)), null);
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
/*     */   public void setValue(Object value, Firebase.CompletionListener listener)
/*     */   {
/*  99 */     onDisconnectSetInternal(value, PriorityUtilities.NullPriority(), listener);
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
/*     */   public void setValue(Object value, String priority, Firebase.CompletionListener listener)
/*     */   {
/* 115 */     onDisconnectSetInternal(value, PriorityUtilities.parsePriority(priority), listener);
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
/*     */   public void setValue(Object value, double priority, Firebase.CompletionListener listener)
/*     */   {
/* 131 */     onDisconnectSetInternal(value, PriorityUtilities.parsePriority(Double.valueOf(priority)), listener);
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
/*     */   public void setValue(Object value, Map priority, Firebase.CompletionListener listener)
/*     */   {
/* 147 */     onDisconnectSetInternal(value, PriorityUtilities.parsePriority(priority), listener);
/*     */   }
/*     */   
/*     */   private void onDisconnectSetInternal(Object value, Node priority, final Firebase.CompletionListener onComplete) {
/* 151 */     Validation.validateWritablePath(this.path);
/* 152 */     ValidationPath.validateWithObject(this.path, value);
/*     */     try {
/* 154 */       Object bouncedValue = JsonHelpers.getMapper().convertValue(value, Object.class);
/* 155 */       Validation.validateWritableObject(bouncedValue);
/* 156 */       final Node node = NodeUtilities.NodeFromJSON(bouncedValue, priority);
/* 157 */       this.repo.scheduleNow(new Runnable()
/*     */       {
/*     */         public void run() {
/* 160 */           OnDisconnect.this.repo.onDisconnectSetValue(OnDisconnect.this.path, node, onComplete);
/*     */         }
/*     */       });
/*     */     } catch (IllegalArgumentException e) {
/* 164 */       throw new FirebaseException("Failed to parse to snapshot", e);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void updateChildren(Map<String, Object> update)
/*     */   {
/* 176 */     updateChildren(update, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void updateChildren(final Map<String, Object> update, final Firebase.CompletionListener listener)
/*     */   {
/* 186 */     final Map<Path, Node> parsedUpdate = Validation.parseAndValidateUpdate(this.path, update);
/* 187 */     this.repo.scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 190 */         OnDisconnect.this.repo.onDisconnectUpdate(OnDisconnect.this.path, parsedUpdate, listener, update);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void removeValue()
/*     */   {
/* 201 */     setValue(null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void removeValue(Firebase.CompletionListener listener)
/*     */   {
/* 209 */     setValue(null, listener);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void cancel()
/*     */   {
/* 218 */     cancel(null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void cancel(final Firebase.CompletionListener listener)
/*     */   {
/* 226 */     this.repo.scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 229 */         OnDisconnect.this.repo.onDisconnectCancel(OnDisconnect.this.path, listener);
/*     */       }
/*     */     });
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/OnDisconnect.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */