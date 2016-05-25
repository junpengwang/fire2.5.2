/*    */ package com.firebase.client.core;
/*    */ 
/*    */ import com.firebase.client.FirebaseError;
/*    */ import com.firebase.client.annotations.NotNull;
/*    */ import com.firebase.client.core.view.Change;
/*    */ import com.firebase.client.core.view.DataEvent;
/*    */ import com.firebase.client.core.view.Event.EventType;
/*    */ import com.firebase.client.core.view.QuerySpec;
/*    */ import java.util.concurrent.atomic.AtomicBoolean;
/*    */ 
/*    */ public abstract class EventRegistration
/*    */ {
/* 13 */   private AtomicBoolean zombied = new AtomicBoolean(false);
/*    */   private EventRegistrationZombieListener listener;
/* 15 */   private boolean isUserInitiated = false;
/*    */   
/*    */   public abstract boolean respondsTo(Event.EventType paramEventType);
/*    */   
/*    */   public abstract DataEvent createEvent(Change paramChange, QuerySpec paramQuerySpec);
/*    */   
/*    */   public abstract void fireEvent(DataEvent paramDataEvent);
/*    */   
/*    */   public abstract void fireCancelEvent(FirebaseError paramFirebaseError);
/*    */   
/*    */   public abstract EventRegistration clone(QuerySpec paramQuerySpec);
/*    */   
/*    */   public abstract boolean isSameListener(EventRegistration paramEventRegistration);
/*    */   
/*    */   @NotNull
/*    */   public abstract QuerySpec getQuerySpec();
/*    */   
/*    */   public void zombify() {
/* 33 */     if ((this.zombied.compareAndSet(false, true)) && 
/* 34 */       (this.listener != null)) {
/* 35 */       this.listener.onZombied(this);
/* 36 */       this.listener = null;
/*    */     }
/*    */   }
/*    */   
/*    */   public boolean isZombied()
/*    */   {
/* 42 */     return this.zombied.get();
/*    */   }
/*    */   
/*    */   public void setOnZombied(EventRegistrationZombieListener listener) {
/* 46 */     assert (!isZombied());
/* 47 */     assert (this.listener == null);
/* 48 */     this.listener = listener;
/*    */   }
/*    */   
/*    */   public boolean isUserInitiated() {
/* 52 */     return this.isUserInitiated;
/*    */   }
/*    */   
/*    */   public void setIsUserInitiated(boolean isUserInitiated) {
/* 56 */     this.isUserInitiated = isUserInitiated;
/*    */   }
/*    */   
/*    */   Repo getRepo()
/*    */   {
/* 61 */     return null;
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/EventRegistration.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */