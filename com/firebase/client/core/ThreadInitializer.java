/*   */ package com.firebase.client.core;
/*   */ 
/*   */ 
/*   */ 
/*   */ public abstract interface ThreadInitializer
/*   */ {
/* 7 */   public static final ThreadInitializer defaultInstance = new ThreadInitializer()
/*   */   {
/*   */     public void setName(Thread t, String name) {
/* : */       t.setName(name);
/*   */     }
/*   */     
/*   */     public void setDaemon(Thread t, boolean isDaemon)
/*   */     {
/* ? */       t.setDaemon(isDaemon);
/*   */     }
/*   */     
/*   */     public void setUncaughtExceptionHandler(Thread t, Thread.UncaughtExceptionHandler handler)
/*   */     {
/* D */       t.setUncaughtExceptionHandler(handler);
/*   */     }
/*   */   };
/*   */   
/*   */   public abstract void setName(Thread paramThread, String paramString);
/*   */   
/*   */   public abstract void setDaemon(Thread paramThread, boolean paramBoolean);
/*   */   
/*   */   public abstract void setUncaughtExceptionHandler(Thread paramThread, Thread.UncaughtExceptionHandler paramUncaughtExceptionHandler);
/*   */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/ThreadInitializer.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */