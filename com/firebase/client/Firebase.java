/*     */ package com.firebase.client;
/*     */ 
/*     */ import com.firebase.client.authentication.AuthenticationManager;
/*     */ import com.firebase.client.core.CompoundWrite;
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.core.Repo;
/*     */ import com.firebase.client.core.RepoManager;
/*     */ import com.firebase.client.core.ValidationPath;
/*     */ import com.firebase.client.core.view.QueryParams;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import com.firebase.client.snapshot.NodeUtilities;
/*     */ import com.firebase.client.snapshot.PriorityUtilities;
/*     */ import com.firebase.client.utilities.ParsedUrl;
/*     */ import com.firebase.client.utilities.PushIdGenerator;
/*     */ import com.firebase.client.utilities.Utilities;
/*     */ import com.firebase.client.utilities.Validation;
/*     */ import com.firebase.client.utilities.encoding.JsonHelpers;
/*     */ import com.shaded.fasterxml.jackson.databind.ObjectMapper;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.net.URLEncoder;
/*     */ import java.util.Map;
/*     */ 
/*     */ public class Firebase
/*     */   extends Query
/*     */ {
/*     */   private static Config defaultConfig;
/*     */   
/*     */   private AuthenticationManager getAuthenticationManager()
/*     */   {
/*  31 */     return getRepo().getAuthenticationManager();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Firebase(String url)
/*     */   {
/* 155 */     this(Utilities.parseUrl(url));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Firebase(Repo repo, Path path)
/*     */   {
/* 164 */     super(repo, path);
/*     */   }
/*     */   
/*     */   Firebase(String url, Config config) {
/* 168 */     this(Utilities.parseUrl(url), config);
/*     */   }
/*     */   
/*     */   private Firebase(ParsedUrl parsedUrl, Config config) {
/* 172 */     super(RepoManager.getRepo(config, parsedUrl.repoInfo), parsedUrl.path, QueryParams.DEFAULT_PARAMS, false);
/*     */   }
/*     */   
/*     */   private Firebase(ParsedUrl parsedUrl)
/*     */   {
/* 177 */     this(parsedUrl, getDefaultConfig());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Firebase child(String pathString)
/*     */   {
/* 186 */     if (pathString == null) {
/* 187 */       throw new NullPointerException("Can't pass null for argument 'pathString' in child()");
/*     */     }
/* 189 */     if (getPath().isEmpty())
/*     */     {
/* 191 */       Validation.validateRootPathString(pathString);
/*     */     } else {
/* 193 */       Validation.validatePathString(pathString);
/*     */     }
/* 195 */     Path childPath = getPath().child(new Path(pathString));
/* 196 */     return new Firebase(this.repo, childPath);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Firebase push()
/*     */   {
/* 207 */     String childNameStr = PushIdGenerator.generatePushChildName(this.repo.getServerTime());
/* 208 */     ChildKey childKey = ChildKey.fromString(childNameStr);
/* 209 */     return new Firebase(this.repo, getPath().child(childKey));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setValue(Object value)
/*     */   {
/* 238 */     setValueInternal(value, PriorityUtilities.parsePriority(null), null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setValue(Object value, Object priority)
/*     */   {
/* 268 */     setValueInternal(value, PriorityUtilities.parsePriority(priority), null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setValue(Object value, CompletionListener listener)
/*     */   {
/* 298 */     setValueInternal(value, PriorityUtilities.parsePriority(null), listener);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setValue(Object value, Object priority, CompletionListener listener)
/*     */   {
/* 329 */     setValueInternal(value, PriorityUtilities.parsePriority(priority), listener);
/*     */   }
/*     */   
/*     */   private void setValueInternal(Object value, Node priority, final CompletionListener listener) {
/* 333 */     Validation.validateWritablePath(getPath());
/* 334 */     ValidationPath.validateWithObject(getPath(), value);
/*     */     try {
/* 336 */       Object bouncedValue = JsonHelpers.getMapper().convertValue(value, Object.class);
/* 337 */       Validation.validateWritableObject(bouncedValue);
/* 338 */       final Node node = NodeUtilities.NodeFromJSON(bouncedValue, priority);
/* 339 */       this.repo.scheduleNow(new Runnable()
/*     */       {
/*     */         public void run()
/*     */         {
/* 343 */           Firebase.this.repo.setValue(Firebase.this.getPath(), node, listener);
/*     */         }
/*     */       });
/*     */     } catch (IllegalArgumentException e) {
/* 347 */       throw new FirebaseException("Failed to parse to snapshot", e);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setPriority(Object priority)
/*     */   {
/* 377 */     setPriorityInternal(PriorityUtilities.parsePriority(priority), null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setPriority(Object priority, CompletionListener listener)
/*     */   {
/* 406 */     setPriorityInternal(PriorityUtilities.parsePriority(priority), listener);
/*     */   }
/*     */   
/*     */   private void setPriorityInternal(final Node priority, final CompletionListener listener) {
/* 410 */     Validation.validateWritablePath(getPath());
/* 411 */     this.repo.scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 414 */         Firebase.this.repo.setValue(Firebase.this.getPath().child(ChildKey.getPriorityKey()), priority, listener);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void updateChildren(Map<String, Object> update)
/*     */   {
/* 426 */     updateChildren(update, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void updateChildren(final Map<String, Object> update, final CompletionListener listener)
/*     */   {
/* 435 */     if (update == null) {
/* 436 */       throw new NullPointerException("Can't pass null for argument 'update' in updateChildren()");
/*     */     }
/* 438 */     Map<Path, Node> parsedUpdate = Validation.parseAndValidateUpdate(getPath(), update);
/* 439 */     final CompoundWrite merge = CompoundWrite.fromPathMerge(parsedUpdate);
/*     */     
/* 441 */     this.repo.scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 444 */         Firebase.this.repo.updateChildren(Firebase.this.getPath(), merge, listener, update);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void removeValue()
/*     */   {
/* 455 */     setValue(null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void removeValue(CompletionListener listener)
/*     */   {
/* 463 */     setValue(null, listener);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public OnDisconnect onDisconnect()
/*     */   {
/* 473 */     Validation.validateWritablePath(getPath());
/* 474 */     return new OnDisconnect(this.repo, getPath());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   @Deprecated
/*     */   public void auth(String credential, AuthListener listener)
/*     */   {
/* 487 */     if (listener == null) {
/* 488 */       throw new NullPointerException("Can't pass null for argument 'listener' in auth()");
/*     */     }
/* 490 */     if (credential == null) {
/* 491 */       throw new NullPointerException("Can't pass null for argument 'credential' in auth()");
/*     */     }
/* 493 */     getAuthenticationManager().authWithFirebaseToken(credential, listener);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void unauth()
/*     */   {
/* 501 */     getAuthenticationManager().unauth();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   @Deprecated
/*     */   public void unauth(CompletionListener listener)
/*     */   {
/* 512 */     if (listener == null) {
/* 513 */       throw new NullPointerException("Can't pass null for argument 'listener' in unauth()");
/*     */     }
/* 515 */     getAuthenticationManager().unauth(listener);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public AuthStateListener addAuthStateListener(AuthStateListener listener)
/*     */   {
/* 537 */     if (listener == null) {
/* 538 */       throw new NullPointerException("Can't pass null for argument 'listener' in addAuthStateListener()");
/*     */     }
/* 540 */     getAuthenticationManager().addAuthStateListener(listener);
/* 541 */     return listener;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void removeAuthStateListener(AuthStateListener listener)
/*     */   {
/* 550 */     if (listener == null) {
/* 551 */       throw new NullPointerException("Can't pass null for argument 'listener' in removeAuthStateListener()");
/*     */     }
/* 553 */     getAuthenticationManager().removeAuthStateListener(listener);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public AuthData getAuth()
/*     */   {
/* 564 */     return getAuthenticationManager().getAuth();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void authAnonymously(AuthResultHandler handler)
/*     */   {
/* 574 */     getAuthenticationManager().authAnonymously(handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void authWithPassword(String email, String password, AuthResultHandler handler)
/*     */   {
/* 586 */     if (email == null) {
/* 587 */       throw new NullPointerException("Can't pass null for argument 'email' in authWithPassword()");
/*     */     }
/* 589 */     if (password == null) {
/* 590 */       throw new NullPointerException("Can't pass null for argument 'password' in authWithPassword()");
/*     */     }
/* 592 */     getAuthenticationManager().authWithPassword(email, password, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void authWithCustomToken(String token, AuthResultHandler handler)
/*     */   {
/* 603 */     if (token == null) {
/* 604 */       throw new NullPointerException("Can't pass null for argument 'token' in authWithCustomToken()");
/*     */     }
/* 606 */     getAuthenticationManager().authWithCustomToken(token, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void authWithOAuthToken(String provider, String token, AuthResultHandler handler)
/*     */   {
/* 624 */     if (provider == null) {
/* 625 */       throw new NullPointerException("Can't pass null for argument 'provider' in authWithOAuthToken()");
/*     */     }
/* 627 */     if (token == null) {
/* 628 */       throw new NullPointerException("Can't pass null for argument 'token' in authWithOAuthToken()");
/*     */     }
/* 630 */     getAuthenticationManager().authWithOAuthToken(provider, token, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void authWithOAuthToken(String provider, Map<String, String> options, AuthResultHandler handler)
/*     */   {
/* 648 */     if (provider == null) {
/* 649 */       throw new NullPointerException("Can't pass null for argument 'provider' in authWithOAuthToken()");
/*     */     }
/* 651 */     if (options == null) {
/* 652 */       throw new NullPointerException("Can't pass null for argument 'options' in authWithOAuthToken()");
/*     */     }
/* 654 */     getAuthenticationManager().authWithOAuthToken(provider, options, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void createUser(String email, String password, ResultHandler handler)
/*     */   {
/* 668 */     if (email == null) {
/* 669 */       throw new NullPointerException("Can't pass null for argument 'email' in createUser()");
/*     */     }
/* 671 */     if (password == null) {
/* 672 */       throw new NullPointerException("Can't pass null for argument 'password' in createUser()");
/*     */     }
/* 674 */     getAuthenticationManager().createUser(email, password, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void createUser(String email, String password, ValueResultHandler<Map<String, Object>> handler)
/*     */   {
/* 689 */     if (email == null) {
/* 690 */       throw new NullPointerException("Can't pass null for argument 'email' in createUser()");
/*     */     }
/* 692 */     if (password == null) {
/* 693 */       throw new NullPointerException("Can't pass null for argument 'password' in createUser()");
/*     */     }
/* 695 */     getAuthenticationManager().createUser(email, password, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void removeUser(String email, String password, ResultHandler handler)
/*     */   {
/* 707 */     if (email == null) {
/* 708 */       throw new NullPointerException("Can't pass null for argument 'email' in removeUser()");
/*     */     }
/* 710 */     if (password == null) {
/* 711 */       throw new NullPointerException("Can't pass null for argument 'password' in removeUser()");
/*     */     }
/* 713 */     getAuthenticationManager().removeUser(email, password, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void changePassword(String email, String oldPassword, String newPassword, ResultHandler handler)
/*     */   {
/* 726 */     if (email == null) {
/* 727 */       throw new NullPointerException("Can't pass null for argument 'email' in changePassword()");
/*     */     }
/* 729 */     if (oldPassword == null) {
/* 730 */       throw new NullPointerException("Can't pass null for argument 'oldPassword' in changePassword()");
/*     */     }
/* 732 */     if (newPassword == null) {
/* 733 */       throw new NullPointerException("Can't pass null for argument 'newPassword' in changePassword()");
/*     */     }
/* 735 */     getAuthenticationManager().changePassword(email, oldPassword, newPassword, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void changeEmail(String oldEmail, String password, String newEmail, ResultHandler handler)
/*     */   {
/* 748 */     if (oldEmail == null) {
/* 749 */       throw new NullPointerException("Can't pass null for argument 'oldEmail' in changeEmail()");
/*     */     }
/* 751 */     if (password == null) {
/* 752 */       throw new NullPointerException("Can't pass null for argument 'password' in changeEmail()");
/*     */     }
/* 754 */     if (newEmail == null) {
/* 755 */       throw new NullPointerException("Can't pass null for argument 'newEmail' in changeEmail()");
/*     */     }
/* 757 */     getAuthenticationManager().changeEmail(oldEmail, password, newEmail, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void resetPassword(String email, ResultHandler handler)
/*     */   {
/* 768 */     if (email == null) {
/* 769 */       throw new NullPointerException("Can't pass null for argument 'email' in resetPassword()");
/*     */     }
/* 771 */     getAuthenticationManager().resetPassword(email, handler);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void runTransaction(Transaction.Handler handler)
/*     */   {
/* 783 */     runTransaction(handler, true);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void runTransaction(final Transaction.Handler handler, final boolean fireLocalEvents)
/*     */   {
/* 796 */     if (handler == null) {
/* 797 */       throw new NullPointerException("Can't pass null for argument 'handler' in runTransaction()");
/*     */     }
/* 799 */     Validation.validateWritablePath(getPath());
/* 800 */     this.repo.scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 803 */         Firebase.this.repo.startTransaction(Firebase.this.getPath(), handler, fireLocalEvents);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static void goOffline()
/*     */   {
/* 832 */     goOffline(getDefaultConfig());
/*     */   }
/*     */   
/*     */   static void goOffline(Config config) {
/* 836 */     RepoManager.interrupt(config);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static void goOnline()
/*     */   {
/* 845 */     goOnline(getDefaultConfig());
/*     */   }
/*     */   
/*     */   static void goOnline(Config config) {
/* 849 */     RepoManager.resume(config);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public FirebaseApp getApp()
/*     */   {
/* 861 */     return this.repo.getFirebaseApp();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public String toString()
/*     */   {
/* 869 */     Firebase parent = getParent();
/* 870 */     if (parent == null) {
/* 871 */       return this.repo.toString();
/*     */     }
/*     */     try {
/* 874 */       return parent.toString() + "/" + URLEncoder.encode(getKey(), "UTF-8").replace("+", "%20");
/*     */     }
/*     */     catch (UnsupportedEncodingException e) {
/* 877 */       throw new FirebaseException("Failed to URLEncode key: " + getKey(), e);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Firebase getParent()
/*     */   {
/* 886 */     Path parentPath = getPath().getParent();
/* 887 */     if (parentPath != null) {
/* 888 */       return new Firebase(this.repo, parentPath);
/*     */     }
/* 890 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Firebase getRoot()
/*     */   {
/* 898 */     return new Firebase(this.repo, new Path(""));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public String getKey()
/*     */   {
/* 905 */     if (getPath().isEmpty()) {
/* 906 */       return null;
/*     */     }
/* 908 */     return getPath().getBack().asString();
/*     */   }
/*     */   
/*     */   public boolean equals(Object other)
/*     */   {
/* 913 */     return ((other instanceof Firebase)) && (toString().equals(other.toString()));
/*     */   }
/*     */   
/*     */   public int hashCode()
/*     */   {
/* 918 */     return toString().hashCode();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public static String getSdkVersion()
/*     */   {
/* 925 */     return "2.5.2";
/*     */   }
/*     */   
/*     */   void setHijackHash(final boolean hijackHash) {
/* 929 */     this.repo.scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 932 */         Firebase.this.repo.setHijackHash(hijackHash);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public static synchronized Config getDefaultConfig()
/*     */   {
/* 942 */     if (defaultConfig == null) {
/* 943 */       defaultConfig = new Config();
/*     */     }
/* 945 */     return defaultConfig;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static synchronized void setDefaultConfig(Config config)
/*     */   {
/* 954 */     if ((defaultConfig != null) && (defaultConfig.isFrozen())) {
/* 955 */       throw new FirebaseException("Modifications to Config objects must occur before they are in use");
/*     */     }
/* 957 */     defaultConfig = config;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public static void setAndroidContext(android.content.Context context)
/*     */   {
/* 971 */     if (context == null) {
/* 972 */       throw new NullPointerException("Can't pass null for argument 'context' in setAndroidContext()");
/*     */     }
/* 974 */     com.firebase.client.core.Context.setAndroidContext(context);
/*     */   }
/*     */   
/*     */   public static abstract interface ValueResultHandler<T>
/*     */   {
/*     */     public abstract void onSuccess(T paramT);
/*     */     
/*     */     public abstract void onError(FirebaseError paramFirebaseError);
/*     */   }
/*     */   
/*     */   public static abstract interface ResultHandler
/*     */   {
/*     */     public abstract void onSuccess();
/*     */     
/*     */     public abstract void onError(FirebaseError paramFirebaseError);
/*     */   }
/*     */   
/*     */   public static abstract interface AuthResultHandler
/*     */   {
/*     */     public abstract void onAuthenticated(AuthData paramAuthData);
/*     */     
/*     */     public abstract void onAuthenticationError(FirebaseError paramFirebaseError);
/*     */   }
/*     */   
/*     */   public static abstract interface AuthStateListener
/*     */   {
/*     */     public abstract void onAuthStateChanged(AuthData paramAuthData);
/*     */   }
/*     */   
/*     */   @Deprecated
/*     */   public static abstract interface AuthListener
/*     */   {
/*     */     public abstract void onAuthError(FirebaseError paramFirebaseError);
/*     */     
/*     */     public abstract void onAuthSuccess(Object paramObject);
/*     */     
/*     */     public abstract void onAuthRevoked(FirebaseError paramFirebaseError);
/*     */   }
/*     */   
/*     */   public static abstract interface CompletionListener
/*     */   {
/*     */     public abstract void onComplete(FirebaseError paramFirebaseError, Firebase paramFirebase);
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/Firebase.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */