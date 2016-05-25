/*     */ package com.firebase.client.android;
/*     */ 
/*     */ import android.os.Build.VERSION;
/*     */ import android.os.Handler;
/*     */ import android.util.Log;
/*     */ import com.firebase.client.EventTarget;
/*     */ import com.firebase.client.Firebase;
/*     */ import com.firebase.client.Logger;
/*     */ import com.firebase.client.Logger.Level;
/*     */ import com.firebase.client.RunLoop;
/*     */ import com.firebase.client.core.persistence.CachePolicy;
/*     */ import com.firebase.client.utilities.DefaultRunLoop;
/*     */ import com.firebase.client.utilities.LogWrapper;
/*     */ import java.util.List;
/*     */ import java.util.Set;
/*     */ 
/*     */ public class AndroidPlatform implements com.firebase.client.core.Platform
/*     */ {
/*  19 */   private static final Object mutex = new Object();
/*     */   
/*     */   private static AndroidPlatform platform;
/*     */   private final android.content.Context applicationContext;
/*  23 */   private final Set<String> createdPersistenceCaches = new java.util.HashSet();
/*     */   
/*     */   public AndroidPlatform(android.content.Context context) {
/*  26 */     this.applicationContext = context.getApplicationContext();
/*  27 */     synchronized (mutex) {
/*  28 */       if (platform == null) {
/*  29 */         platform = this;
/*     */       } else {
/*  31 */         throw new IllegalStateException("Created more than one AndroidPlatform instance!");
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public EventTarget newEventTarget(com.firebase.client.core.Context context)
/*     */   {
/*  38 */     return new AndroidEventTarget();
/*     */   }
/*     */   
/*     */   public RunLoop newRunLoop(com.firebase.client.core.Context ctx)
/*     */   {
/*  43 */     final LogWrapper logger = ctx.getLogger("RunLoop");
/*  44 */     new DefaultRunLoop()
/*     */     {
/*     */       public void handleException(final Throwable e) {
/*  47 */         final String message = "Uncaught exception in Firebase runloop (" + Firebase.getSdkVersion() + "). Please report to support@firebase.com";
/*     */         
/*     */ 
/*  50 */         logger.error(message, e);
/*     */         
/*     */ 
/*     */ 
/*     */ 
/*  55 */         Handler handler = new Handler(AndroidPlatform.this.applicationContext.getMainLooper());
/*  56 */         handler.post(new Runnable()
/*     */         {
/*     */           public void run() {
/*  59 */             throw new RuntimeException(message, e);
/*     */           }
/*     */         });
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */   public Logger newLogger(com.firebase.client.core.Context context, Logger.Level component, List<String> enabledComponents)
/*     */   {
/*  68 */     return new AndroidLogger(component, enabledComponents);
/*     */   }
/*     */   
/*     */   public String getUserAgent(com.firebase.client.core.Context context)
/*     */   {
/*  73 */     return Build.VERSION.SDK_INT + "/Android";
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void runBackgroundTask(com.firebase.client.core.Context context, final Runnable r)
/*     */   {
/*  82 */     new Thread()
/*     */     {
/*     */       public void run() {
/*     */         try {
/*  86 */           r.run();
/*     */         }
/*     */         catch (OutOfMemoryError e) {
/*  89 */           throw e;
/*     */         } catch (Throwable e) {
/*  91 */           Log.e("Firebase", "An unexpected error occurred. Please contact support@firebase.com. Details: ", e);
/*  92 */           throw new RuntimeException(e);
/*     */         }
/*     */       }
/*     */     }.start();
/*     */   }
/*     */   
/*     */   public String getPlatformVersion()
/*     */   {
/* 100 */     return "android-" + Firebase.getSdkVersion();
/*     */   }
/*     */   
/*     */   public synchronized com.firebase.client.core.persistence.PersistenceManager createPersistenceManager(com.firebase.client.core.Context firebaseContext, String firebaseId)
/*     */   {
/* 105 */     String sessionId = firebaseContext.getSessionPersistenceKey();
/* 106 */     String cacheId = firebaseId + "_" + sessionId;
/*     */     
/* 108 */     if (this.createdPersistenceCaches.contains(cacheId)) {
/* 109 */       throw new com.firebase.client.FirebaseException("SessionPersistenceKey '" + sessionId + "' has already been used.");
/*     */     }
/* 111 */     this.createdPersistenceCaches.add(cacheId);
/* 112 */     SqlPersistenceStorageEngine engine = new SqlPersistenceStorageEngine(this.applicationContext, firebaseContext, cacheId);
/* 113 */     CachePolicy cachePolicy = new com.firebase.client.core.persistence.LRUCachePolicy(firebaseContext.getPersistenceCacheSizeBytes());
/* 114 */     return new com.firebase.client.core.persistence.DefaultPersistenceManager(firebaseContext, engine, cachePolicy);
/*     */   }
/*     */   
/*     */   public com.firebase.client.CredentialStore newCredentialStore(com.firebase.client.core.Context context)
/*     */   {
/* 119 */     return new AndroidCredentialStore(this.applicationContext);
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/android/AndroidPlatform.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */