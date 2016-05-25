/*    */ package com.firebase.client.core.view;
/*    */ 
/*    */ import com.firebase.client.core.EventRegistration;
/*    */ import com.firebase.client.core.Path;
/*    */ 
/*    */ public class CancelEvent implements Event
/*    */ {
/*    */   private final Path path;
/*    */   private final EventRegistration eventRegistration;
/*    */   private final com.firebase.client.FirebaseError error;
/*    */   
/*    */   public CancelEvent(EventRegistration eventRegistration, com.firebase.client.FirebaseError error, Path path)
/*    */   {
/* 14 */     this.eventRegistration = eventRegistration;
/* 15 */     this.path = path;
/* 16 */     this.error = error;
/*    */   }
/*    */   
/*    */   public Path getPath() {
/* 20 */     return this.path;
/*    */   }
/*    */   
/*    */   public void fire() {
/* 24 */     this.eventRegistration.fireCancelEvent(this.error);
/*    */   }
/*    */   
/*    */   public String toString() {
/* 28 */     return getPath() + ":" + "CANCEL";
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/view/CancelEvent.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */