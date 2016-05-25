/*     */ package com.firebase.client.core;
/*     */ 
/*     */ import com.firebase.client.CredentialStore;
/*     */ import com.firebase.client.EventTarget;
/*     */ import com.firebase.client.Firebase;
/*     */ import com.firebase.client.FirebaseException;
/*     */ import com.firebase.client.Logger;
/*     */ import com.firebase.client.Logger.Level;
/*     */ import com.firebase.client.RunLoop;
/*     */ import com.firebase.client.core.persistence.NoopPersistenceManager;
/*     */ import com.firebase.client.core.persistence.PersistenceManager;
/*     */ import com.firebase.client.utilities.LogWrapper;
/*     */ import java.lang.reflect.Constructor;
/*     */ import java.lang.reflect.InvocationTargetException;
/*     */ import java.util.List;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class Context
/*     */ {
/*     */   private static final long DEFAULT_CACHE_SIZE = 10485760L;
/*     */   protected Logger logger;
/*     */   protected EventTarget eventTarget;
/*     */   protected CredentialStore credentialStore;
/*     */   protected RunLoop runLoop;
/*     */   protected String persistenceKey;
/*     */   protected List<String> loggedComponents;
/*     */   protected String userAgent;
/*     */   protected String authenticationServer;
/*  31 */   protected Logger.Level logLevel = Logger.Level.INFO;
/*     */   protected boolean persistenceEnabled;
/*  33 */   protected AuthExpirationBehavior authExpirationBehavior = AuthExpirationBehavior.DEFAULT;
/*  34 */   protected long cacheSize = 10485760L;
/*     */   private PersistenceManager forcedPersistenceManager;
/*  36 */   private boolean frozen = false;
/*  37 */   private boolean stopped = false;
/*     */   private static Platform platform;
/*     */   private static android.content.Context androidContext;
/*     */   
/*     */   private Platform getPlatform()
/*     */   {
/*  43 */     if (platform == null) {
/*  44 */       if (AndroidSupport.isAndroid())
/*  45 */         throw new RuntimeException("You need to set the Android context using Firebase.setAndroidContext() before using Firebase.");
/*  46 */       if (GaePlatform.isActive()) {
/*  47 */         GaePlatform gaePlatform = GaePlatform.INSTANCE;
/*  48 */         gaePlatform.initialize();
/*  49 */         platform = gaePlatform;
/*     */       } else {
/*  51 */         platform = JvmPlatform.INSTANCE;
/*     */       }
/*     */     }
/*  54 */     return platform;
/*     */   }
/*     */   
/*     */   public static synchronized void setAndroidContext(android.content.Context context) {
/*  58 */     if (androidContext == null) {
/*  59 */       androidContext = context.getApplicationContext();
/*     */       try {
/*  61 */         Class androidPlatformClass = Class.forName("com.firebase.client.android.AndroidPlatform");
/*  62 */         Constructor constructor = androidPlatformClass.getConstructor(new Class[] { android.content.Context.class });
/*  63 */         platform = (Platform)constructor.newInstance(new Object[] { context });
/*     */       }
/*     */       catch (ClassNotFoundException e) {
/*  66 */         throw new RuntimeException("Android classes not found. Are you using the firebase-client-android artifact?");
/*     */       }
/*     */       catch (NoSuchMethodException e) {
/*  69 */         throw new RuntimeException("Failed to instantiate AndroidPlatform class.  Using ProGuard?  See http://stackoverflow.com/questions/26273929/what-proguard-configuration-do-i-need-for-firebase-on-android", e);
/*     */       }
/*     */       catch (InvocationTargetException e) {
/*  72 */         throw new RuntimeException("Failed to instantiate AndroidPlatform class.", e);
/*     */       } catch (InstantiationException e) {
/*  74 */         throw new RuntimeException("Failed to instantiate AndroidPlatform class.", e);
/*     */       } catch (IllegalAccessException e) {
/*  76 */         throw new RuntimeException("Failed to instantiate AndroidPlatform class.", e);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public boolean isFrozen() {
/*  82 */     return this.frozen;
/*     */   }
/*     */   
/*     */   public boolean isStopped() {
/*  86 */     return this.stopped;
/*     */   }
/*     */   
/*     */   synchronized void freeze() {
/*  90 */     if (!this.frozen) {
/*  91 */       this.frozen = true;
/*  92 */       initServices();
/*     */     }
/*     */   }
/*     */   
/*     */   public void requireStarted() {
/*  97 */     if (this.stopped) {
/*  98 */       restartServices();
/*  99 */       this.stopped = false;
/*     */     }
/*     */   }
/*     */   
/*     */   private void initServices()
/*     */   {
/* 105 */     ensureLogger();
/*     */     
/* 107 */     getPlatform();
/* 108 */     ensureUserAgent();
/*     */     
/* 110 */     ensureEventTarget();
/* 111 */     ensureRunLoop();
/* 112 */     ensureSessionIdentifier();
/* 113 */     ensureCredentialStore();
/*     */   }
/*     */   
/*     */   private void restartServices() {
/* 117 */     this.eventTarget.restart();
/* 118 */     this.runLoop.restart();
/*     */   }
/*     */   
/*     */   void stop() {
/* 122 */     this.stopped = true;
/* 123 */     this.eventTarget.shutdown();
/* 124 */     this.runLoop.shutdown();
/*     */   }
/*     */   
/*     */   protected void assertUnfrozen() {
/* 128 */     if (isFrozen()) {
/* 129 */       throw new FirebaseException("Modifications to Config objects must occur before they are in use");
/*     */     }
/*     */   }
/*     */   
/*     */   public LogWrapper getLogger(String component) {
/* 134 */     return new LogWrapper(this.logger, component);
/*     */   }
/*     */   
/*     */   public LogWrapper getLogger(String component, String prefix) {
/* 138 */     return new LogWrapper(this.logger, component, prefix);
/*     */   }
/*     */   
/*     */   PersistenceManager getPersistenceManager(String firebaseId)
/*     */   {
/* 143 */     if (this.forcedPersistenceManager != null) {
/* 144 */       return this.forcedPersistenceManager;
/*     */     }
/* 146 */     if (this.persistenceEnabled) {
/* 147 */       PersistenceManager cache = platform.createPersistenceManager(this, firebaseId);
/* 148 */       if (cache == null) {
/* 149 */         throw new IllegalArgumentException("You have enabled persistence, but persistence is not supported on this platform.");
/*     */       }
/*     */       
/* 152 */       return cache;
/*     */     }
/* 154 */     return new NoopPersistenceManager();
/*     */   }
/*     */   
/*     */   public boolean isPersistenceEnabled()
/*     */   {
/* 159 */     return this.persistenceEnabled;
/*     */   }
/*     */   
/*     */   public AuthExpirationBehavior getAuthExpirationBehavior()
/*     */   {
/* 164 */     return this.authExpirationBehavior;
/*     */   }
/*     */   
/*     */   public long getPersistenceCacheSizeBytes() {
/* 168 */     return this.cacheSize;
/*     */   }
/*     */   
/*     */   void forcePersistenceManager(PersistenceManager persistenceManager)
/*     */   {
/* 173 */     this.forcedPersistenceManager = persistenceManager;
/*     */   }
/*     */   
/*     */   public EventTarget getEventTarget() {
/* 177 */     return this.eventTarget;
/*     */   }
/*     */   
/*     */   public RunLoop getRunLoop() {
/* 181 */     return this.runLoop;
/*     */   }
/*     */   
/*     */   public void runBackgroundTask(Runnable r) {
/* 185 */     getPlatform().runBackgroundTask(this, r);
/*     */   }
/*     */   
/*     */   public String getUserAgent() {
/* 189 */     return this.userAgent;
/*     */   }
/*     */   
/*     */   public String getPlatformVersion() {
/* 193 */     return getPlatform().getPlatformVersion();
/*     */   }
/*     */   
/*     */   public String getSessionPersistenceKey() {
/* 197 */     return this.persistenceKey;
/*     */   }
/*     */   
/*     */   public CredentialStore getCredentialStore() {
/* 201 */     return this.credentialStore;
/*     */   }
/*     */   
/*     */   public String getAuthenticationServer() {
/* 205 */     if (this.authenticationServer == null) {
/* 206 */       return "https://auth.firebase.com/";
/*     */     }
/* 208 */     return this.authenticationServer;
/*     */   }
/*     */   
/*     */   public boolean isCustomAuthenticationServerSet()
/*     */   {
/* 213 */     return this.authenticationServer != null;
/*     */   }
/*     */   
/*     */   private void ensureLogger() {
/* 217 */     if (this.logger == null) {
/* 218 */       this.logger = getPlatform().newLogger(this, this.logLevel, this.loggedComponents);
/*     */     }
/*     */   }
/*     */   
/*     */   private void ensureRunLoop() {
/* 223 */     if (this.runLoop == null) {
/* 224 */       this.runLoop = platform.newRunLoop(this);
/*     */     }
/*     */   }
/*     */   
/*     */   private void ensureEventTarget() {
/* 229 */     if (this.eventTarget == null) {
/* 230 */       this.eventTarget = getPlatform().newEventTarget(this);
/*     */     }
/*     */   }
/*     */   
/*     */   private void ensureUserAgent() {
/* 235 */     if (this.userAgent == null) {
/* 236 */       this.userAgent = buildUserAgent(getPlatform().getUserAgent(this));
/*     */     }
/*     */   }
/*     */   
/*     */   private void ensureCredentialStore() {
/* 241 */     if (this.credentialStore == null) {
/* 242 */       this.credentialStore = getPlatform().newCredentialStore(this);
/*     */     }
/*     */   }
/*     */   
/*     */   private void ensureSessionIdentifier() {
/* 247 */     if (this.persistenceKey == null) {
/* 248 */       this.persistenceKey = "default";
/*     */     }
/*     */   }
/*     */   
/*     */   private String buildUserAgent(String platformAgent) {
/* 253 */     StringBuilder sb = new StringBuilder().append("Firebase/").append("5").append("/").append(Firebase.getSdkVersion()).append("/").append(platformAgent);
/*     */     
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 260 */     return sb.toString();
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/Context.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */