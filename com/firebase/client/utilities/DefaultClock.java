/*   */ package com.firebase.client.utilities;
/*   */ 
/*   */ public class DefaultClock implements Clock
/*   */ {
/*   */   public long millis() {
/* 6 */     return System.currentTimeMillis();
/*   */   }
/*   */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/utilities/DefaultClock.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */