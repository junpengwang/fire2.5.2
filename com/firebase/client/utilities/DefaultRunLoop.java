/*    */ package com.firebase.client.utilities;
/*    */ 
/*    */ import java.util.concurrent.ScheduledThreadPoolExecutor;
/*    */ 
/*    */ public abstract class DefaultRunLoop implements com.firebase.client.RunLoop
/*    */ {
/*    */   private ScheduledThreadPoolExecutor executor;
/*    */   
/*    */   private class FirebaseThreadFactory implements java.util.concurrent.ThreadFactory {
/*    */     private FirebaseThreadFactory() {}
/*    */     
/*    */     public Thread newThread(Runnable r) {
/* 13 */       Thread thread = DefaultRunLoop.this.getThreadFactory().newThread(r);
/* 14 */       com.firebase.client.core.ThreadInitializer initializer = DefaultRunLoop.this.getThreadInitializer();
/* 15 */       initializer.setName(thread, "FirebaseWorker");
/* 16 */       initializer.setDaemon(thread, true);
/* 17 */       initializer.setUncaughtExceptionHandler(thread, new Thread.UncaughtExceptionHandler()
/*    */       {
/*    */         public void uncaughtException(Thread t, Throwable e) {
/* 20 */           DefaultRunLoop.this.handleException(e);
/*    */         }
/* 22 */       });
/* 23 */       return thread;
/*    */     }
/*    */   }
/*    */   
/*    */   protected java.util.concurrent.ThreadFactory getThreadFactory() {
/* 28 */     return java.util.concurrent.Executors.defaultThreadFactory();
/*    */   }
/*    */   
/*    */   protected com.firebase.client.core.ThreadInitializer getThreadInitializer() {
/* 32 */     return com.firebase.client.core.ThreadInitializer.defaultInstance;
/*    */   }
/*    */   
/*    */ 
/*    */   public abstract void handleException(Throwable paramThrowable);
/*    */   
/*    */   public DefaultRunLoop()
/*    */   {
/* 40 */     int threadsInPool = 1;
/* 41 */     java.util.concurrent.ThreadFactory threadFactory = new FirebaseThreadFactory(null);
/* 42 */     this.executor = new ScheduledThreadPoolExecutor(threadsInPool, threadFactory);
/*    */     
/* 44 */     this.executor.setKeepAliveTime(3L, java.util.concurrent.TimeUnit.SECONDS);
/*    */   }
/*    */   
/*    */   public void scheduleNow(final Runnable runnable)
/*    */   {
/* 49 */     this.executor.execute(new Runnable()
/*    */     {
/*    */       public void run()
/*    */       {
/*    */         try {
/* 54 */           runnable.run();
/*    */         } catch (Throwable e) {
/* 56 */           DefaultRunLoop.this.handleException(e);
/*    */         }
/*    */       }
/*    */     });
/*    */   }
/*    */   
/*    */   public java.util.concurrent.ScheduledFuture schedule(final Runnable runnable, long milliseconds)
/*    */   {
/* 64 */     this.executor.schedule(new Runnable()
/*    */     {
/*    */       public void run()
/*    */       {
/*    */         try {
/* 69 */           runnable.run();
/*    */         } catch (Throwable e) {
/* 71 */           DefaultRunLoop.this.handleException(e); } } }, milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public void shutdown()
/*    */   {
/* 79 */     this.executor.setCorePoolSize(0);
/*    */   }
/*    */   
/*    */   public void restart()
/*    */   {
/* 84 */     this.executor.setCorePoolSize(1);
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/utilities/DefaultRunLoop.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */