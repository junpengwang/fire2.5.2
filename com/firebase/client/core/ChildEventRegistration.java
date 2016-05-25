/*     */ package com.firebase.client.core;
/*     */ 
/*     */ import com.firebase.client.ChildEventListener;
/*     */ import com.firebase.client.FirebaseError;
/*     */ import com.firebase.client.annotations.NotNull;
/*     */ import com.firebase.client.core.view.Change;
/*     */ import com.firebase.client.core.view.DataEvent;
/*     */ import com.firebase.client.core.view.Event.EventType;
/*     */ import com.firebase.client.core.view.QuerySpec;
/*     */ 
/*     */ public class ChildEventRegistration extends EventRegistration
/*     */ {
/*     */   private final Repo repo;
/*     */   private final ChildEventListener eventListener;
/*     */   private final QuerySpec spec;
/*     */   
/*     */   public ChildEventRegistration(@NotNull Repo repo, @NotNull ChildEventListener eventListener, @NotNull QuerySpec spec)
/*     */   {
/*  19 */     this.repo = repo;
/*  20 */     this.eventListener = eventListener;
/*  21 */     this.spec = spec;
/*     */   }
/*     */   
/*     */   public boolean respondsTo(Event.EventType eventType)
/*     */   {
/*  26 */     return eventType != Event.EventType.VALUE;
/*     */   }
/*     */   
/*     */   public boolean equals(Object other)
/*     */   {
/*  31 */     return ((other instanceof ChildEventRegistration)) && (((ChildEventRegistration)other).eventListener.equals(this.eventListener)) && (((ChildEventRegistration)other).repo.equals(this.repo)) && (((ChildEventRegistration)other).spec.equals(this.spec));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public int hashCode()
/*     */   {
/*  39 */     int result = this.eventListener.hashCode();
/*  40 */     result = 31 * result + this.repo.hashCode();
/*  41 */     result = 31 * result + this.spec.hashCode();
/*  42 */     return result;
/*     */   }
/*     */   
/*     */   public DataEvent createEvent(Change change, QuerySpec query)
/*     */   {
/*  47 */     com.firebase.client.Firebase ref = new com.firebase.client.Firebase(this.repo, query.getPath().child(change.getChildKey()));
/*     */     
/*  49 */     com.firebase.client.DataSnapshot snapshot = new com.firebase.client.DataSnapshot(ref, change.getIndexedNode());
/*  50 */     String prevName = change.getPrevName() != null ? change.getPrevName().asString() : null;
/*  51 */     return new DataEvent(change.getEventType(), this, snapshot, prevName);
/*     */   }
/*     */   
/*     */   public void fireEvent(DataEvent eventData)
/*     */   {
/*  56 */     if (isZombied()) {
/*  57 */       return;
/*     */     }
/*  59 */     switch (eventData.getEventType()) {
/*     */     case CHILD_ADDED: 
/*  61 */       this.eventListener.onChildAdded(eventData.getSnapshot(), eventData.getPreviousName());
/*  62 */       break;
/*     */     case CHILD_CHANGED: 
/*  64 */       this.eventListener.onChildChanged(eventData.getSnapshot(), eventData.getPreviousName());
/*  65 */       break;
/*     */     case CHILD_MOVED: 
/*  67 */       this.eventListener.onChildMoved(eventData.getSnapshot(), eventData.getPreviousName());
/*  68 */       break;
/*     */     case CHILD_REMOVED: 
/*  70 */       this.eventListener.onChildRemoved(eventData.getSnapshot());
/*  71 */       break;
/*     */     }
/*     */     
/*     */   }
/*     */   
/*     */ 
/*     */   public void fireCancelEvent(FirebaseError error)
/*     */   {
/*  79 */     this.eventListener.onCancelled(error);
/*     */   }
/*     */   
/*     */   public EventRegistration clone(QuerySpec newQuery)
/*     */   {
/*  84 */     return new ChildEventRegistration(this.repo, this.eventListener, newQuery);
/*     */   }
/*     */   
/*     */   public boolean isSameListener(EventRegistration other)
/*     */   {
/*  89 */     return ((other instanceof ChildEventRegistration)) && (((ChildEventRegistration)other).eventListener.equals(this.eventListener));
/*     */   }
/*     */   
/*     */ 
/*     */   @NotNull
/*     */   public QuerySpec getQuerySpec()
/*     */   {
/*  96 */     return this.spec;
/*     */   }
/*     */   
/*     */   public String toString()
/*     */   {
/* 101 */     return "ChildEventRegistration";
/*     */   }
/*     */   
/*     */   Repo getRepo() {
/* 105 */     return this.repo;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/ChildEventRegistration.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */