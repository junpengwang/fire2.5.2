/*    */ package com.firebase.client.core;
/*    */ 
/*    */ import com.firebase.client.EventTarget;
/*    */ import java.util.concurrent.BlockingQueue;
/*    */ import java.util.concurrent.LinkedBlockingQueue;
/*    */ import java.util.concurrent.ThreadFactory;
/*    */ import java.util.concurrent.ThreadPoolExecutor;
/*    */ import java.util.concurrent.TimeUnit;
/*    */ 
/*    */ 
/*    */ 
/*    */ class ThreadPoolEventTarget
/*    */   implements EventTarget
/*    */ {
/*    */   private final ThreadPoolExecutor executor;
/*    */   
/*    */   public ThreadPoolEventTarget(final ThreadFactory wrappedFactory, final ThreadInitializer threadInitializer)
/*    */   {
/* 19 */     int poolSize = 1;
/* 20 */     BlockingQueue<Runnable> queue = new LinkedBlockingQueue();
/*    */     
/* 22 */     this.executor = new ThreadPoolExecutor(poolSize, poolSize, 3L, TimeUnit.SECONDS, queue, new ThreadFactory()
/*    */     {
/*    */ 
/*    */       public Thread newThread(Runnable r)
/*    */       {
/* 27 */         Thread thread = wrappedFactory.newThread(r);
/* 28 */         threadInitializer.setName(thread, "FirebaseEventTarget");
/* 29 */         threadInitializer.setDaemon(thread, true);
/*    */         
/* 31 */         return thread;
/*    */       }
/*    */     });
/*    */   }
/*    */   
/*    */   public void postEvent(Runnable r)
/*    */   {
/* 38 */     this.executor.execute(r);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public void shutdown()
/*    */   {
/* 48 */     this.executor.setCorePoolSize(0);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public void restart()
/*    */   {
/* 58 */     this.executor.setCorePoolSize(1);
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/ThreadPoolEventTarget.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */