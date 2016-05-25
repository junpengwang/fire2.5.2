/*     */ package com.firebase.client.core;
/*     */ 
/*     */ import com.firebase.client.CredentialStore;
/*     */ import com.firebase.client.EventTarget;
/*     */ import com.firebase.client.Firebase;
/*     */ import com.firebase.client.Logger;
/*     */ import com.firebase.client.Logger.Level;
/*     */ import com.firebase.client.RunLoop;
/*     */ import com.firebase.client.authentication.NoopCredentialStore;
/*     */ import com.firebase.client.utilities.DefaultLogger;
/*     */ import com.firebase.client.utilities.DefaultRunLoop;
/*     */ import com.firebase.client.utilities.LogWrapper;
/*     */ import com.firebase.tubesock.WebSocket;
/*     */ import java.lang.reflect.InvocationTargetException;
/*     */ import java.lang.reflect.Method;
/*     */ import java.util.List;
/*     */ import java.util.concurrent.ThreadFactory;
/*     */ 
/*     */  enum GaePlatform implements Platform
/*     */ {
/*  21 */   INSTANCE;
/*     */   
/*     */   static ThreadFactory threadFactoryInstance;
/*  24 */   static final ThreadInitializer threadInitializerInstance = new ThreadInitializer()
/*     */   {
/*     */     public void setName(Thread t, String name) {}
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */     public void setDaemon(Thread t, boolean isDaemon) {}
/*     */     
/*     */ 
/*     */ 
/*     */     public void setUncaughtExceptionHandler(Thread t, Thread.UncaughtExceptionHandler handler)
/*     */     {
/*  37 */       t.setUncaughtExceptionHandler(handler);
/*     */     }
/*     */   };
/*     */   
/*     */   private GaePlatform() {}
/*     */   
/*  43 */   public Logger newLogger(Context ctx, Logger.Level level, List<String> components) { return new DefaultLogger(level, components); }
/*     */   
/*     */   private static ThreadFactory getGaeThreadFactory()
/*     */   {
/*  47 */     if (threadFactoryInstance == null) {
/*     */       try {
/*  49 */         Class c = Class.forName("com.google.appengine.api.ThreadManager");
/*  50 */         if (c != null) {
/*  51 */           threadFactoryInstance = (ThreadFactory)c.getMethod("backgroundThreadFactory", new Class[0]).invoke(null, new Object[0]);
/*     */         }
/*     */       }
/*     */       catch (ClassNotFoundException e) {
/*  55 */         return null;
/*     */       } catch (InvocationTargetException e) {
/*  57 */         throw new RuntimeException(e);
/*     */       } catch (NoSuchMethodException e) {
/*  59 */         throw new RuntimeException(e);
/*     */       } catch (IllegalAccessException e) {
/*  61 */         throw new RuntimeException(e);
/*     */       }
/*     */     }
/*  64 */     return threadFactoryInstance;
/*     */   }
/*     */   
/*     */   public static boolean isActive() {
/*  68 */     return getGaeThreadFactory() != null;
/*     */   }
/*     */   
/*     */   public void initialize() {
/*  72 */     WebSocket.setThreadFactory(threadFactoryInstance, new com.firebase.tubesock.ThreadInitializer()
/*     */     {
/*     */       public void setName(Thread thread, String s)
/*     */       {
/*  76 */         GaePlatform.threadInitializerInstance.setName(thread, s);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public EventTarget newEventTarget(Context ctx)
/*     */   {
/*  83 */     return new ThreadPoolEventTarget(getGaeThreadFactory(), threadInitializerInstance);
/*     */   }
/*     */   
/*     */   public RunLoop newRunLoop(Context context)
/*     */   {
/*  88 */     final LogWrapper logger = context.getLogger("RunLoop");
/*  89 */     new DefaultRunLoop()
/*     */     {
/*     */       public void handleException(Throwable e) {
/*  92 */         logger.error("Uncaught exception in Firebase runloop (" + Firebase.getSdkVersion() + "). Please report to support@firebase.com", e);
/*     */       }
/*     */       
/*     */ 
/*     */       protected ThreadFactory getThreadFactory()
/*     */       {
/*  98 */         return GaePlatform.threadFactoryInstance;
/*     */       }
/*     */       
/*     */       protected ThreadInitializer getThreadInitializer()
/*     */       {
/* 103 */         return GaePlatform.threadInitializerInstance;
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */   public String getUserAgent(Context ctx)
/*     */   {
/* 110 */     String deviceName = "AppEngine";
/* 111 */     String systemVersion = System.getProperty("java.specification.version", "Unknown");
/*     */     
/* 113 */     return systemVersion + "/" + deviceName;
/*     */   }
/*     */   
/*     */   public String getPlatformVersion()
/*     */   {
/* 118 */     return "gae-" + Firebase.getSdkVersion();
/*     */   }
/*     */   
/*     */   public com.firebase.client.core.persistence.PersistenceManager createPersistenceManager(Context ctx, String namespace)
/*     */   {
/* 123 */     return null;
/*     */   }
/*     */   
/*     */   public CredentialStore newCredentialStore(Context ctx)
/*     */   {
/* 128 */     return new NoopCredentialStore(ctx);
/*     */   }
/*     */   
/*     */   public void runBackgroundTask(final Context ctx, final Runnable r)
/*     */   {
/* 133 */     threadFactoryInstance.newThread(new Runnable()
/*     */     {
/*     */       public void run() {
/*     */         try {
/* 137 */           r.run();
/*     */         }
/*     */         catch (OutOfMemoryError e) {
/* 140 */           throw e;
/*     */         } catch (Throwable e) {
/* 142 */           ctx.getLogger("BackgroundTask").error("An unexpected error occurred. Please contact support@firebase.com. Details: ", e);
/*     */           
/* 144 */           throw new RuntimeException(e);
/*     */         }
/*     */       }
/*     */     }).start();
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/GaePlatform.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */