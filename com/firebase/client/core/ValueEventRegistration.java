/*    */ package com.firebase.client.core;
/*    */ 
/*    */ import com.firebase.client.ValueEventListener;
/*    */ import com.firebase.client.core.view.Change;
/*    */ import com.firebase.client.core.view.DataEvent;
/*    */ import com.firebase.client.core.view.Event.EventType;
/*    */ import com.firebase.client.core.view.QuerySpec;
/*    */ 
/*    */ public class ValueEventRegistration extends EventRegistration
/*    */ {
/*    */   private final Repo repo;
/*    */   private final ValueEventListener eventListener;
/*    */   private final QuerySpec spec;
/*    */   
/*    */   public ValueEventRegistration(Repo repo, ValueEventListener eventListener, @com.firebase.client.annotations.NotNull QuerySpec spec)
/*    */   {
/* 17 */     this.repo = repo;
/* 18 */     this.eventListener = eventListener;
/* 19 */     this.spec = spec;
/*    */   }
/*    */   
/*    */   public boolean respondsTo(Event.EventType eventType)
/*    */   {
/* 24 */     return eventType == Event.EventType.VALUE;
/*    */   }
/*    */   
/*    */   public boolean equals(Object other)
/*    */   {
/* 29 */     return ((other instanceof ValueEventRegistration)) && (((ValueEventRegistration)other).eventListener.equals(this.eventListener)) && (((ValueEventRegistration)other).repo.equals(this.repo)) && (((ValueEventRegistration)other).spec.equals(this.spec));
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public int hashCode()
/*    */   {
/* 37 */     int result = this.eventListener.hashCode();
/* 38 */     result = 31 * result + this.repo.hashCode();
/* 39 */     result = 31 * result + this.spec.hashCode();
/* 40 */     return result;
/*    */   }
/*    */   
/*    */   public DataEvent createEvent(Change change, QuerySpec query)
/*    */   {
/* 45 */     com.firebase.client.Firebase ref = new com.firebase.client.Firebase(this.repo, query.getPath());
/*    */     
/* 47 */     com.firebase.client.DataSnapshot dataSnapshot = new com.firebase.client.DataSnapshot(ref, change.getIndexedNode());
/* 48 */     return new DataEvent(Event.EventType.VALUE, this, dataSnapshot, null);
/*    */   }
/*    */   
/*    */   public void fireEvent(DataEvent eventData)
/*    */   {
/* 53 */     if (isZombied()) {
/* 54 */       return;
/*    */     }
/* 56 */     this.eventListener.onDataChange(eventData.getSnapshot());
/*    */   }
/*    */   
/*    */   public void fireCancelEvent(com.firebase.client.FirebaseError error)
/*    */   {
/* 61 */     this.eventListener.onCancelled(error);
/*    */   }
/*    */   
/*    */   public EventRegistration clone(QuerySpec newQuery)
/*    */   {
/* 66 */     return new ValueEventRegistration(this.repo, this.eventListener, newQuery);
/*    */   }
/*    */   
/*    */   public boolean isSameListener(EventRegistration other)
/*    */   {
/* 71 */     return ((other instanceof ValueEventRegistration)) && (((ValueEventRegistration)other).eventListener.equals(this.eventListener));
/*    */   }
/*    */   
/*    */ 
/*    */   @com.firebase.client.annotations.NotNull
/*    */   public QuerySpec getQuerySpec()
/*    */   {
/* 78 */     return this.spec;
/*    */   }
/*    */   
/*    */   public String toString()
/*    */   {
/* 83 */     return "ValueEventRegistration";
/*    */   }
/*    */   
/*    */   Repo getRepo()
/*    */   {
/* 88 */     return this.repo;
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/ValueEventRegistration.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */