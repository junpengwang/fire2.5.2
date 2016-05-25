/*    */ package com.firebase.client;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public abstract interface Logger
/*    */ {
/*    */   public abstract void onLogMessage(Level paramLevel, String paramString1, String paramString2, long paramLong);
/*    */   
/*    */ 
/*    */ 
/*    */   public abstract Level getLogLevel();
/*    */   
/*    */ 
/*    */ 
/*    */   public static enum Level
/*    */   {
/* 18 */     DEBUG,  INFO,  WARN,  ERROR,  NONE;
/*    */     
/*    */     private Level() {}
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/Logger.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */