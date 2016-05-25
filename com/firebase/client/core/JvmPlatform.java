/*    */ package com.firebase.client.core;
/*    */ 
/*    */ import com.firebase.client.EventTarget;
/*    */ import com.firebase.client.Firebase;
/*    */ import com.firebase.client.Logger;
/*    */ import com.firebase.client.Logger.Level;
/*    */ import com.firebase.client.RunLoop;
/*    */ import com.firebase.client.utilities.DefaultLogger;
/*    */ import com.firebase.client.utilities.DefaultRunLoop;
/*    */ import com.firebase.client.utilities.LogWrapper;
/*    */ import java.util.List;
/*    */ 
/*    */  enum JvmPlatform implements Platform
/*    */ {
/* 15 */   INSTANCE;
/*    */   
/*    */   private JvmPlatform() {}
/*    */   
/* 19 */   public Logger newLogger(Context ctx, Logger.Level level, List<String> components) { return new DefaultLogger(level, components); }
/*    */   
/*    */ 
/*    */   public EventTarget newEventTarget(Context ctx)
/*    */   {
/* 24 */     return new ThreadPoolEventTarget(java.util.concurrent.Executors.defaultThreadFactory(), ThreadInitializer.defaultInstance);
/*    */   }
/*    */   
/*    */   public RunLoop newRunLoop(Context context)
/*    */   {
/* 29 */     final LogWrapper logger = context.getLogger("RunLoop");
/* 30 */     new DefaultRunLoop()
/*    */     {
/*    */       public void handleException(Throwable e) {
/* 33 */         logger.error("Uncaught exception in Firebase runloop (" + Firebase.getSdkVersion() + "). Please report to support@firebase.com", e);
/*    */       }
/*    */     };
/*    */   }
/*    */   
/*    */ 
/*    */   public String getUserAgent(Context ctx)
/*    */   {
/* 41 */     String deviceName = System.getProperty("java.vm.name", "Unknown JVM");
/* 42 */     String systemVersion = System.getProperty("java.specification.version", "Unknown");
/*    */     
/* 44 */     return systemVersion + "/" + deviceName;
/*    */   }
/*    */   
/*    */   public String getPlatformVersion()
/*    */   {
/* 49 */     return "jvm-" + Firebase.getSdkVersion();
/*    */   }
/*    */   
/*    */   public com.firebase.client.core.persistence.PersistenceManager createPersistenceManager(Context ctx, String namespace)
/*    */   {
/* 54 */     return null;
/*    */   }
/*    */   
/*    */   public com.firebase.client.CredentialStore newCredentialStore(Context ctx)
/*    */   {
/* 59 */     return new com.firebase.client.authentication.NoopCredentialStore(ctx);
/*    */   }
/*    */   
/*    */   public void runBackgroundTask(final Context ctx, final Runnable r)
/*    */   {
/* 64 */     new Thread()
/*    */     {
/*    */       public void run() {
/*    */         try {
/* 68 */           r.run();
/*    */         }
/*    */         catch (OutOfMemoryError e) {
/* 71 */           throw e;
/*    */         } catch (Throwable e) {
/* 73 */           ctx.getLogger("BackgroundTask").error("An unexpected error occurred. Please contact support@firebase.com. Details: ", e);
/* 74 */           throw new RuntimeException(e);
/*    */         }
/*    */       }
/*    */     }.start();
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/JvmPlatform.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */