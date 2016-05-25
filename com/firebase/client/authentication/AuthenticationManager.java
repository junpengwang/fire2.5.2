/*     */ package com.firebase.client.authentication;
/*     */ 
/*     */ import com.firebase.client.AuthData;
/*     */ import com.firebase.client.CredentialStore;
/*     */ import com.firebase.client.Firebase;
/*     */ import com.firebase.client.Firebase.AuthListener;
/*     */ import com.firebase.client.Firebase.AuthResultHandler;
/*     */ import com.firebase.client.Firebase.AuthStateListener;
/*     */ import com.firebase.client.Firebase.CompletionListener;
/*     */ import com.firebase.client.Firebase.ResultHandler;
/*     */ import com.firebase.client.Firebase.ValueResultHandler;
/*     */ import com.firebase.client.FirebaseError;
/*     */ import com.firebase.client.core.Context;
/*     */ import com.firebase.client.core.PersistentConnection;
/*     */ import com.firebase.client.core.Repo;
/*     */ import com.firebase.client.core.RepoInfo;
/*     */ import com.firebase.client.utilities.HttpUtilities.HttpRequestType;
/*     */ import com.firebase.client.utilities.LogWrapper;
/*     */ import com.firebase.client.utilities.Utilities;
/*     */ import java.io.IOException;
/*     */ import java.net.URI;
/*     */ import java.util.Collections;
/*     */ import java.util.HashMap;
/*     */ import java.util.Map;
/*     */ import java.util.Set;
/*     */ import java.util.concurrent.Semaphore;
/*     */ import org.shaded.apache.http.client.methods.HttpUriRequest;
/*     */ import org.shaded.apache.http.impl.client.DefaultHttpClient;
/*     */ 
/*     */ public class AuthenticationManager
/*     */ {
/*     */   private static final String TOKEN_KEY = "token";
/*     */   private static final String USER_DATA_KEY = "userData";
/*     */   private static final String AUTH_DATA_KEY = "authData";
/*     */   private static final String ERROR_KEY = "error";
/*     */   private static final String CUSTOM_PROVIDER = "custom";
/*     */   private static final String LOG_TAG = "AuthenticationManager";
/*     */   private static final int CONNECTION_TIMEOUT = 20000;
/*     */   private final Context context;
/*     */   private final Repo repo;
/*     */   private final RepoInfo repoInfo;
/*     */   private final PersistentConnection connection;
/*     */   private final CredentialStore store;
/*     */   private final LogWrapper logger;
/*     */   private final Set<Firebase.AuthStateListener> listenerSet;
/*     */   private AuthData authData;
/*     */   private AuthAttempt currentAuthAttempt;
/*     */   
/*     */   private class AuthAttempt
/*     */   {
/*     */     private Firebase.AuthResultHandler handler;
/*     */     private final Firebase.AuthListener legacyListener;
/*     */     
/*     */     AuthAttempt(Firebase.AuthResultHandler handler)
/*     */     {
/*  56 */       this.handler = handler;
/*  57 */       this.legacyListener = null;
/*     */     }
/*     */     
/*     */     AuthAttempt(Firebase.AuthListener legacyListener) {
/*  61 */       this.legacyListener = legacyListener;
/*  62 */       this.handler = null;
/*     */     }
/*     */     
/*     */     public void fireError(final FirebaseError error) {
/*  66 */       if ((this.legacyListener != null) || (this.handler != null)) {
/*  67 */         AuthenticationManager.this.fireEvent(new Runnable()
/*     */         {
/*     */           public void run() {
/*  70 */             if (AuthenticationManager.AuthAttempt.this.legacyListener != null) {
/*  71 */               AuthenticationManager.AuthAttempt.this.legacyListener.onAuthError(error);
/*  72 */             } else if (AuthenticationManager.AuthAttempt.this.handler != null) {
/*  73 */               AuthenticationManager.AuthAttempt.this.handler.onAuthenticationError(error);
/*  74 */               AuthenticationManager.AuthAttempt.this.handler = null;
/*     */             }
/*     */           }
/*     */         });
/*     */       }
/*     */     }
/*     */     
/*     */     public void fireSuccess(final AuthData authData) {
/*  82 */       if ((this.legacyListener != null) || (this.handler != null)) {
/*  83 */         AuthenticationManager.this.fireEvent(new Runnable()
/*     */         {
/*     */           public void run() {
/*  86 */             if (AuthenticationManager.AuthAttempt.this.legacyListener != null) {
/*  87 */               AuthenticationManager.AuthAttempt.this.legacyListener.onAuthSuccess(authData);
/*  88 */             } else if (AuthenticationManager.AuthAttempt.this.handler != null) {
/*  89 */               AuthenticationManager.AuthAttempt.this.handler.onAuthenticated(authData);
/*  90 */               AuthenticationManager.AuthAttempt.this.handler = null;
/*     */             }
/*     */           }
/*     */         });
/*     */       }
/*     */     }
/*     */     
/*     */     public void fireRevoked(final FirebaseError error) {
/*  98 */       if (this.legacyListener != null) {
/*  99 */         AuthenticationManager.this.fireEvent(new Runnable()
/*     */         {
/*     */           public void run() {
/* 102 */             AuthenticationManager.AuthAttempt.this.legacyListener.onAuthRevoked(error);
/*     */           }
/*     */         });
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public AuthenticationManager(Context context, Repo repo, RepoInfo repoInfo, PersistentConnection connection) {
/* 110 */     this.context = context;
/* 111 */     this.repo = repo;
/* 112 */     this.repoInfo = repoInfo;
/* 113 */     this.connection = connection;
/* 114 */     this.authData = null;
/* 115 */     this.store = context.getCredentialStore();
/* 116 */     this.logger = context.getLogger("AuthenticationManager");
/* 117 */     this.listenerSet = new java.util.HashSet();
/*     */   }
/*     */   
/*     */   private void fireEvent(Runnable r) {
/* 121 */     this.context.getEventTarget().postEvent(r);
/*     */   }
/*     */   
/*     */   private void fireOnSuccess(final Firebase.ValueResultHandler handler, final Object result) {
/* 125 */     if (handler != null) {
/* 126 */       fireEvent(new Runnable()
/*     */       {
/*     */         public void run() {
/* 129 */           handler.onSuccess(result);
/*     */         }
/*     */       });
/*     */     }
/*     */   }
/*     */   
/*     */   private void fireOnError(final Firebase.ValueResultHandler handler, final FirebaseError error) {
/* 136 */     if (handler != null) {
/* 137 */       fireEvent(new Runnable()
/*     */       {
/*     */         public void run() {
/* 140 */           handler.onError(error);
/*     */         }
/*     */       });
/*     */     }
/*     */   }
/*     */   
/*     */   private Firebase.ValueResultHandler ignoreResultValueHandler(final Firebase.ResultHandler handler) {
/* 147 */     new Firebase.ValueResultHandler()
/*     */     {
/*     */       public void onSuccess(Object result) {
/* 150 */         handler.onSuccess();
/*     */       }
/*     */       
/*     */       public void onError(FirebaseError error)
/*     */       {
/* 155 */         handler.onError(error);
/*     */       }
/*     */     };
/*     */   }
/*     */   
/*     */   private void preemptAnyExistingAttempts() {
/* 161 */     if (this.currentAuthAttempt != null) {
/* 162 */       FirebaseError error = new FirebaseError(-5, "Due to another authentication attempt, this authentication attempt was aborted before it could complete.");
/*     */       
/* 164 */       this.currentAuthAttempt.fireError(error);
/* 165 */       this.currentAuthAttempt = null;
/*     */     }
/*     */   }
/*     */   
/*     */   private FirebaseError decodeErrorResponse(Object errorResponse) {
/* 170 */     String code = (String)Utilities.getOrNull(errorResponse, "code", String.class);
/* 171 */     String message = (String)Utilities.getOrNull(errorResponse, "message", String.class);
/* 172 */     String details = (String)Utilities.getOrNull(errorResponse, "details", String.class);
/* 173 */     if (code != null)
/*     */     {
/* 175 */       return FirebaseError.fromStatus(code, message, details);
/*     */     }
/* 177 */     String errorMessage = message == null ? "Error while authenticating." : message;
/* 178 */     return new FirebaseError(64537, errorMessage, details);
/*     */   }
/*     */   
/*     */   private boolean attemptHasBeenPreempted(AuthAttempt attempt)
/*     */   {
/* 183 */     return attempt != this.currentAuthAttempt;
/*     */   }
/*     */   
/*     */   private AuthAttempt newAuthAttempt(Firebase.AuthResultHandler handler) {
/* 187 */     preemptAnyExistingAttempts();
/* 188 */     this.currentAuthAttempt = new AuthAttempt(handler);
/* 189 */     return this.currentAuthAttempt;
/*     */   }
/*     */   
/*     */   private AuthAttempt newAuthAttempt(Firebase.AuthListener listener)
/*     */   {
/* 194 */     preemptAnyExistingAttempts();
/* 195 */     this.currentAuthAttempt = new AuthAttempt(listener);
/* 196 */     return this.currentAuthAttempt;
/*     */   }
/*     */   
/*     */   private void fireAuthErrorIfNotPreempted(final FirebaseError error, final AuthAttempt attempt) {
/* 200 */     if (!attemptHasBeenPreempted(attempt)) {
/* 201 */       if (attempt != null) {
/* 202 */         fireEvent(new Runnable()
/*     */         {
/*     */           public void run() {
/* 205 */             attempt.fireError(error);
/*     */           }
/*     */         });
/*     */       }
/* 209 */       this.currentAuthAttempt = null;
/*     */     }
/*     */   }
/*     */   
/*     */   private void checkServerSettings() {
/* 214 */     if (this.repoInfo.isDemoHost()) {
/* 215 */       this.logger.warn("Firebase authentication is supported on production Firebases only (*.firebaseio.com). To secure your Firebase, create a production Firebase at https://www.firebase.com.");
/*     */     }
/* 217 */     else if ((this.repoInfo.isCustomHost()) && (!this.context.isCustomAuthenticationServerSet())) {
/* 218 */       throw new IllegalStateException("For a custom firebase host you must first set your authentication server before using authentication features!");
/*     */     }
/*     */   }
/*     */   
/*     */   private String getFirebaseCredentialIdentifier() {
/* 223 */     return this.repoInfo.host;
/*     */   }
/*     */   
/*     */   private void scheduleNow(Runnable r) {
/* 227 */     this.context.getRunLoop().scheduleNow(r);
/*     */   }
/*     */   
/*     */   private AuthData parseAuthData(String token, Map<String, Object> rawAuthData, Map<String, Object> userData)
/*     */   {
/* 232 */     Map<String, Object> authData = (Map)Utilities.getOrNull(rawAuthData, "auth", Map.class);
/* 233 */     if (authData == null) {
/* 234 */       this.logger.warn("Received invalid auth data: " + rawAuthData);
/*     */     }
/*     */     
/* 237 */     Object expiresObj = rawAuthData.get("expires");
/*     */     long expires;
/* 239 */     long expires; if (expiresObj == null) {
/* 240 */       expires = 0L; } else { long expires;
/* 241 */       if ((expiresObj instanceof Integer)) {
/* 242 */         expires = ((Integer)expiresObj).intValue(); } else { long expires;
/* 243 */         if ((expiresObj instanceof Long)) {
/* 244 */           expires = ((Long)expiresObj).longValue(); } else { long expires;
/* 245 */           if ((expiresObj instanceof Double)) {
/* 246 */             expires = ((Double)expiresObj).longValue();
/*     */           } else
/* 248 */             expires = 0L;
/*     */         }
/*     */       } }
/* 251 */     String uid = (String)Utilities.getOrNull(authData, "uid", String.class);
/* 252 */     if (uid == null) {
/* 253 */       uid = (String)Utilities.getOrNull(userData, "uid", String.class);
/*     */     }
/*     */     
/* 256 */     String provider = (String)Utilities.getOrNull(authData, "provider", String.class);
/* 257 */     if (provider == null) {
/* 258 */       provider = (String)Utilities.getOrNull(userData, "provider", String.class);
/*     */     }
/* 260 */     if (provider == null) {
/* 261 */       provider = "custom";
/*     */     }
/*     */     
/* 264 */     if ((uid == null) || (uid.isEmpty())) {
/* 265 */       this.logger.warn("Received invalid auth data: " + authData);
/*     */     }
/* 267 */     Map<String, Object> providerData = (Map)Utilities.getOrNull(userData, provider, Map.class);
/* 268 */     if (providerData == null) {
/* 269 */       providerData = new HashMap();
/*     */     }
/* 271 */     return new AuthData(token, expires, uid, provider, authData, providerData);
/*     */   }
/*     */   
/*     */   private void handleBadAuthStatus(FirebaseError error, AuthAttempt attempt, boolean revoked)
/*     */   {
/* 276 */     boolean expiredToken = error.getCode() == -6;
/* 277 */     if ((expiredToken) && (this.context.getAuthExpirationBehavior() == com.firebase.client.core.AuthExpirationBehavior.PAUSE_WRITES_UNTIL_REAUTH))
/*     */     {
/* 279 */       if (this.logger.logsDebug()) this.logger.debug("Pausing writes due to expired token.");
/* 280 */       this.connection.pauseWrites();
/* 281 */     } else if (this.connection.writesPaused()) {
/* 282 */       assert (this.context.getAuthExpirationBehavior() == com.firebase.client.core.AuthExpirationBehavior.PAUSE_WRITES_UNTIL_REAUTH);
/* 283 */       if (this.logger.logsDebug()) this.logger.debug("Invalid auth while writes are paused; keeping existing session.");
/*     */     } else {
/* 285 */       clearSession();
/*     */     }
/*     */     
/* 288 */     updateAuthState(null);
/* 289 */     if (attempt != null) {
/* 290 */       if (revoked) {
/* 291 */         attempt.fireRevoked(error);
/*     */       } else {
/* 293 */         attempt.fireError(error);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private void handleAuthSuccess(String credential, Map<String, Object> authDataMap, Map<String, Object> optionalUserData, boolean isNewSession, AuthAttempt attempt)
/*     */   {
/* 301 */     if (isNewSession)
/*     */     {
/* 303 */       if (((authDataMap.get("auth") != null) || (authDataMap.get("expires") != null)) && 
/* 304 */         (!saveSession(credential, authDataMap, optionalUserData))) {
/* 305 */         this.logger.warn("Failed to store credentials! Authentication will not be persistent!");
/*     */       }
/*     */     }
/*     */     
/* 309 */     AuthData authData = parseAuthData(credential, authDataMap, optionalUserData);
/* 310 */     updateAuthState(authData);
/* 311 */     if (attempt != null) {
/* 312 */       attempt.fireSuccess(authData);
/*     */     }
/*     */     
/* 315 */     if (this.connection.writesPaused()) {
/* 316 */       if (this.logger.logsDebug()) this.logger.debug("Unpausing writes after successful login.");
/* 317 */       this.connection.unpauseWrites();
/*     */     }
/*     */   }
/*     */   
/*     */   public void resumeSession()
/*     */   {
/*     */     try {
/* 324 */       String credentialData = this.store.loadCredential(getFirebaseCredentialIdentifier(), this.context.getSessionPersistenceKey());
/* 325 */       if (credentialData != null) {
/* 326 */         Map<String, Object> credentials = (Map)com.firebase.client.utilities.encoding.JsonHelpers.getMapper().readValue(credentialData, new com.shaded.fasterxml.jackson.core.type.TypeReference() {});
/* 328 */         final String tokenString = (String)Utilities.getOrNull(credentials, "token", String.class);
/* 329 */         final Map<String, Object> authDataObj = (Map)Utilities.getOrNull(credentials, "authData", Map.class);
/* 330 */         final Map<String, Object> userData = (Map)Utilities.getOrNull(credentials, "userData", Map.class);
/* 331 */         if (authDataObj != null) {
/* 332 */           AuthData authData = parseAuthData(tokenString, authDataObj, userData);
/* 333 */           updateAuthState(authData);
/* 334 */           this.context.getRunLoop().scheduleNow(new Runnable()
/*     */           {
/*     */             public void run() {
/* 337 */               AuthenticationManager.this.connection.auth(tokenString, new Firebase.AuthListener()
/*     */               {
/*     */                 public void onAuthError(FirebaseError error) {
/* 340 */                   AuthenticationManager.this.handleBadAuthStatus(error, null, false);
/*     */                 }
/*     */                 
/*     */                 public void onAuthSuccess(Object authData)
/*     */                 {
/* 345 */                   AuthenticationManager.this.handleAuthSuccess(AuthenticationManager.6.this.val$tokenString, AuthenticationManager.6.this.val$authDataObj, AuthenticationManager.6.this.val$userData, false, null);
/*     */                 }
/*     */                 
/*     */                 public void onAuthRevoked(FirebaseError error)
/*     */                 {
/* 350 */                   AuthenticationManager.this.handleBadAuthStatus(error, null, true);
/*     */                 }
/*     */               });
/*     */             }
/*     */           });
/*     */         }
/*     */       }
/*     */     } catch (IOException e) {
/* 358 */       this.logger.warn("Failed resuming authentication session!", e);
/* 359 */       clearSession();
/*     */     }
/*     */   }
/*     */   
/*     */   private boolean saveSession(String token, Map<String, Object> authData, Map<String, Object> userData) {
/* 364 */     String firebaseId = getFirebaseCredentialIdentifier();
/* 365 */     String sessionId = this.context.getSessionPersistenceKey();
/* 366 */     this.store.clearCredential(firebaseId, sessionId);
/* 367 */     Map<String, Object> sessionMap = new HashMap();
/* 368 */     sessionMap.put("token", token);
/* 369 */     sessionMap.put("authData", authData);
/* 370 */     sessionMap.put("userData", userData);
/*     */     try {
/* 372 */       if (this.logger.logsDebug()) this.logger.debug("Storing credentials for Firebase \"" + firebaseId + "\" and session \"" + sessionId + "\".");
/* 373 */       String credentialData = com.firebase.client.utilities.encoding.JsonHelpers.getMapper().writeValueAsString(sessionMap);
/* 374 */       return this.store.storeCredential(firebaseId, sessionId, credentialData);
/*     */     } catch (com.shaded.fasterxml.jackson.core.JsonProcessingException e) {
/* 376 */       throw new RuntimeException(e);
/*     */     }
/*     */   }
/*     */   
/*     */   private boolean clearSession() {
/* 381 */     String firebaseId = getFirebaseCredentialIdentifier();
/* 382 */     String sessionId = this.context.getSessionPersistenceKey();
/* 383 */     if (this.logger.logsDebug()) this.logger.debug("Clearing credentials for Firebase \"" + firebaseId + "\" and session \"" + sessionId + "\".");
/* 384 */     return this.store.clearCredential(firebaseId, sessionId);
/*     */   }
/*     */   
/*     */   private void updateAuthState(final AuthData authData) {
/* 388 */     boolean changed = authData != null;
/* 389 */     this.authData = authData;
/* 390 */     if (changed) {
/* 391 */       for (final Firebase.AuthStateListener listener : this.listenerSet) {
/* 392 */         fireEvent(new Runnable()
/*     */         {
/*     */           public void run() {
/* 395 */             listener.onAuthStateChanged(authData);
/*     */           }
/*     */         });
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private String buildUrlPath(String urlPath) {
/* 403 */     StringBuilder path = new StringBuilder();
/* 404 */     path.append("/v2/");
/* 405 */     path.append(this.repoInfo.namespace);
/* 406 */     if (!urlPath.startsWith("/")) {
/* 407 */       path.append("/");
/*     */     }
/* 409 */     path.append(urlPath);
/* 410 */     return path.toString();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private void makeRequest(String urlPath, HttpUtilities.HttpRequestType type, Map<String, String> urlParams, Map<String, String> requestParams, final RequestHandler handler)
/*     */   {
/* 418 */     Map<String, String> actualUrlParams = new HashMap(urlParams);
/* 419 */     actualUrlParams.put("transport", "json");
/* 420 */     actualUrlParams.put("v", this.context.getPlatformVersion());
/* 421 */     final HttpUriRequest request = com.firebase.client.utilities.HttpUtilities.requestWithType(this.context.getAuthenticationServer(), buildUrlPath(urlPath), type, actualUrlParams, requestParams);
/* 422 */     if (this.logger.logsDebug())
/*     */     {
/* 424 */       URI uri = request.getURI();
/* 425 */       String scheme = uri.getScheme();
/* 426 */       String authority = uri.getAuthority();
/* 427 */       String path = uri.getPath();
/* 428 */       int numQueryParams = uri.getQuery().split("&").length;
/* 429 */       this.logger.debug(String.format("Sending request to %s://%s%s with %d query params", new Object[] { scheme, authority, path, Integer.valueOf(numQueryParams) }));
/*     */     }
/* 431 */     this.context.runBackgroundTask(new Runnable()
/*     */     {
/*     */       public void run() {
/* 434 */         org.shaded.apache.http.params.HttpParams httpParameters = new org.shaded.apache.http.params.BasicHttpParams();
/* 435 */         org.shaded.apache.http.params.HttpConnectionParams.setConnectionTimeout(httpParameters, 20000);
/* 436 */         org.shaded.apache.http.params.HttpConnectionParams.setSoTimeout(httpParameters, 20000);
/*     */         
/* 438 */         DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
/* 439 */         httpClient.getParams().setParameter("http.protocol.cookie-policy", "compatibility");
/*     */         try {
/* 441 */           final Map<String, Object> result = (Map)httpClient.execute(request, new JsonBasicResponseHandler());
/* 442 */           if (result == null) {
/* 443 */             throw new IOException("Authentication server did not respond with a valid response");
/*     */           }
/* 445 */           AuthenticationManager.this.scheduleNow(new Runnable()
/*     */           {
/*     */             public void run() {
/* 448 */               AuthenticationManager.8.this.val$handler.onResult(result);
/*     */             }
/*     */           });
/*     */         } catch (IOException e) {
/* 452 */           AuthenticationManager.this.scheduleNow(new Runnable()
/*     */           {
/*     */             public void run() {
/* 455 */               AuthenticationManager.8.this.val$handler.onError(e);
/*     */             }
/*     */           });
/*     */         }
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private void makeAuthenticationRequest(String urlPath, Map<String, String> params, Firebase.AuthResultHandler handler) {
/* 464 */     final AuthAttempt attempt = newAuthAttempt(handler);
/* 465 */     makeRequest(urlPath, HttpUtilities.HttpRequestType.GET, params, Collections.emptyMap(), new RequestHandler()
/*     */     {
/*     */       public void onResult(Map<String, Object> result) {
/* 468 */         Object errorResponse = result.get("error");
/* 469 */         String token = (String)Utilities.getOrNull(result, "token", String.class);
/* 470 */         if ((errorResponse == null) && (token != null))
/*     */         {
/* 472 */           if (!AuthenticationManager.this.attemptHasBeenPreempted(attempt)) {
/* 473 */             AuthenticationManager.this.authWithCredential(token, result, attempt);
/*     */           }
/*     */         } else {
/* 476 */           FirebaseError error = AuthenticationManager.this.decodeErrorResponse(errorResponse);
/* 477 */           AuthenticationManager.this.fireAuthErrorIfNotPreempted(error, attempt);
/*     */         }
/*     */       }
/*     */       
/*     */       public void onError(IOException e)
/*     */       {
/* 483 */         FirebaseError error = new FirebaseError(-24, "There was an exception while connecting to the authentication server: " + e.getLocalizedMessage());
/*     */         
/* 485 */         AuthenticationManager.this.fireAuthErrorIfNotPreempted(error, attempt);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void makeOperationRequest(String urlPath, HttpUtilities.HttpRequestType type, Map<String, String> urlParams, Map<String, String> requestParams, Firebase.ResultHandler handler, boolean logUserOut)
/*     */   {
/* 497 */     makeOperationRequestWithResult(urlPath, type, urlParams, requestParams, ignoreResultValueHandler(handler), logUserOut);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void makeOperationRequestWithResult(String urlPath, HttpUtilities.HttpRequestType type, Map<String, String> urlParams, Map<String, String> requestParams, final Firebase.ValueResultHandler<Map<String, Object>> handler, final boolean logUserOut)
/*     */   {
/* 506 */     makeRequest(urlPath, type, urlParams, requestParams, new RequestHandler()
/*     */     {
/*     */       public void onResult(final Map<String, Object> result) {
/* 509 */         Object errorResponse = result.get("error");
/* 510 */         if (errorResponse == null) {
/* 511 */           if (logUserOut) {
/* 512 */             String uid = (String)Utilities.getOrNull(result, "uid", String.class);
/* 513 */             if ((uid != null) && (AuthenticationManager.this.authData != null) && (uid.equals(AuthenticationManager.this.authData.getUid()))) {
/* 514 */               AuthenticationManager.this.unauth(null, false);
/*     */             }
/*     */           }
/*     */           
/* 518 */           AuthenticationManager.this.scheduleNow(new Runnable()
/*     */           {
/*     */             public void run() {
/* 521 */               AuthenticationManager.this.fireOnSuccess(AuthenticationManager.10.this.val$handler, result);
/*     */             }
/*     */           });
/*     */         } else {
/* 525 */           FirebaseError error = AuthenticationManager.this.decodeErrorResponse(errorResponse);
/* 526 */           AuthenticationManager.this.fireOnError(handler, error);
/*     */         }
/*     */       }
/*     */       
/*     */       public void onError(IOException e)
/*     */       {
/* 532 */         FirebaseError error = new FirebaseError(-24, "There was an exception while performing the request: " + e.getLocalizedMessage());
/*     */         
/* 534 */         AuthenticationManager.this.fireOnError(handler, error);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private void authWithCredential(final String credential, final Map<String, Object> optionalUserData, final AuthAttempt attempt)
/*     */   {
/* 541 */     if (attempt != this.currentAuthAttempt) {
/* 542 */       throw new IllegalStateException("Ooops. We messed up tracking which authentications are running!");
/*     */     }
/* 544 */     if (this.logger.logsDebug()) { this.logger.debug("Authenticating with credential of length " + credential.length());
/*     */     }
/* 546 */     this.currentAuthAttempt = null;
/* 547 */     this.connection.auth(credential, new Firebase.AuthListener()
/*     */     {
/*     */       public void onAuthSuccess(Object authData) {
/* 550 */         AuthenticationManager.this.handleAuthSuccess(credential, (Map)authData, optionalUserData, true, attempt);
/*     */       }
/*     */       
/*     */       public void onAuthRevoked(FirebaseError error) {
/* 554 */         AuthenticationManager.this.handleBadAuthStatus(error, attempt, true);
/*     */       }
/*     */       
/*     */       public void onAuthError(FirebaseError error) {
/* 558 */         AuthenticationManager.this.handleBadAuthStatus(error, attempt, false);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public AuthData getAuth() {
/* 564 */     return this.authData;
/*     */   }
/*     */   
/*     */   public void unauth() {
/* 568 */     checkServerSettings();
/* 569 */     unauth(null);
/*     */   }
/*     */   
/*     */   public void unauth(Firebase.CompletionListener listener) {
/* 573 */     unauth(listener, true);
/*     */   }
/*     */   
/*     */   public void unauth(final Firebase.CompletionListener listener, boolean waitForCompletion) {
/* 577 */     checkServerSettings();
/* 578 */     final Semaphore semaphore = new Semaphore(0);
/* 579 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 582 */         AuthenticationManager.this.preemptAnyExistingAttempts();
/* 583 */         AuthenticationManager.this.updateAuthState(null);
/*     */         
/* 585 */         semaphore.release();
/* 586 */         AuthenticationManager.this.clearSession();
/* 587 */         AuthenticationManager.this.connection.unauth(new Firebase.CompletionListener()
/*     */         {
/*     */           public void onComplete(FirebaseError error, Firebase unusedRef) {
/* 590 */             if (AuthenticationManager.12.this.val$listener != null) {
/* 591 */               Firebase ref = new Firebase(AuthenticationManager.this.repo, new com.firebase.client.core.Path(""));
/* 592 */               AuthenticationManager.12.this.val$listener.onComplete(error, ref);
/*     */             }
/*     */           }
/*     */         });
/*     */         
/* 597 */         if (AuthenticationManager.this.connection.writesPaused()) {
/* 598 */           if (AuthenticationManager.this.logger.logsDebug()) AuthenticationManager.this.logger.debug("Unpausing writes after explicit unauth.");
/* 599 */           AuthenticationManager.this.connection.unpauseWrites();
/*     */         }
/*     */       }
/*     */     });
/* 603 */     if (waitForCompletion) {
/*     */       try {
/* 605 */         semaphore.acquire();
/*     */       } catch (InterruptedException e) {
/* 607 */         throw new RuntimeException(e);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public void addAuthStateListener(final Firebase.AuthStateListener listener)
/*     */   {
/* 614 */     checkServerSettings();
/* 615 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 618 */         AuthenticationManager.this.listenerSet.add(listener);
/* 619 */         final AuthData authData = AuthenticationManager.this.authData;
/* 620 */         AuthenticationManager.this.fireEvent(new Runnable()
/*     */         {
/*     */           public void run() {
/* 623 */             AuthenticationManager.13.this.val$listener.onAuthStateChanged(authData);
/*     */           }
/*     */         });
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public void removeAuthStateListener(final Firebase.AuthStateListener listener) {
/* 631 */     checkServerSettings();
/* 632 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 635 */         AuthenticationManager.this.listenerSet.remove(listener);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public void authAnonymously(final Firebase.AuthResultHandler handler) {
/* 641 */     checkServerSettings();
/* 642 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 645 */         Map<String, String> params = new HashMap();
/* 646 */         AuthenticationManager.this.makeAuthenticationRequest("/auth/anonymous", params, handler);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public void authWithPassword(final String email, final String password, final Firebase.AuthResultHandler handler) {
/* 652 */     checkServerSettings();
/* 653 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 656 */         Map<String, String> params = new HashMap();
/* 657 */         params.put("email", email);
/* 658 */         params.put("password", password);
/* 659 */         AuthenticationManager.this.makeAuthenticationRequest("/auth/password", params, handler);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public void authWithCustomToken(final String token, final Firebase.AuthResultHandler handler) {
/* 665 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 668 */         AuthenticationManager.AuthAttempt attempt = AuthenticationManager.this.newAuthAttempt(handler);
/* 669 */         AuthenticationManager.this.authWithCredential(token, null, attempt);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void authWithFirebaseToken(final String token, final Firebase.AuthListener listener)
/*     */   {
/* 680 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 683 */         AuthenticationManager.AuthAttempt attempt = AuthenticationManager.this.newAuthAttempt(listener);
/* 684 */         AuthenticationManager.this.authWithCredential(token, null, attempt);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public void authWithOAuthToken(String provider, String token, Firebase.AuthResultHandler handler) {
/* 690 */     if (token == null) {
/* 691 */       throw new IllegalArgumentException("Token must not be null!");
/*     */     }
/* 693 */     Map<String, String> params = new HashMap();
/* 694 */     params.put("access_token", token);
/* 695 */     authWithOAuthToken(provider, params, handler);
/*     */   }
/*     */   
/*     */   public void authWithOAuthToken(final String provider, final Map<String, String> params, final Firebase.AuthResultHandler handler) {
/* 699 */     checkServerSettings();
/* 700 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 703 */         String url = String.format("/auth/%s/token", new Object[] { provider });
/* 704 */         AuthenticationManager.this.makeAuthenticationRequest(url, params, handler);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public void createUser(String email, String password, Firebase.ResultHandler handler) {
/* 710 */     createUser(email, password, ignoreResultValueHandler(handler));
/*     */   }
/*     */   
/*     */   public void createUser(final String email, final String password, final Firebase.ValueResultHandler<Map<String, Object>> handler) {
/* 714 */     checkServerSettings();
/* 715 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 718 */         Map<String, String> requestParams = new HashMap();
/* 719 */         requestParams.put("email", email);
/* 720 */         requestParams.put("password", password);
/* 721 */         AuthenticationManager.this.makeOperationRequestWithResult("/users", HttpUtilities.HttpRequestType.POST, Collections.emptyMap(), requestParams, handler, false);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */   public void removeUser(final String email, final String password, final Firebase.ResultHandler handler)
/*     */   {
/* 729 */     checkServerSettings();
/* 730 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 733 */         Map<String, String> urlParams = new HashMap();
/* 734 */         urlParams.put("password", password);
/* 735 */         String url = String.format("/users/%s", new Object[] { email });
/* 736 */         AuthenticationManager.this.makeOperationRequest(url, HttpUtilities.HttpRequestType.DELETE, urlParams, Collections.emptyMap(), handler, true);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public void changePassword(final String email, final String oldPassword, final String newPassword, final Firebase.ResultHandler handler)
/*     */   {
/* 743 */     checkServerSettings();
/* 744 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 747 */         Map<String, String> urlParams = new HashMap();
/* 748 */         urlParams.put("oldPassword", oldPassword);
/* 749 */         Map<String, String> requestParams = new HashMap();
/* 750 */         requestParams.put("password", newPassword);
/* 751 */         String url = String.format("/users/%s/password", new Object[] { email });
/* 752 */         AuthenticationManager.this.makeOperationRequest(url, HttpUtilities.HttpRequestType.PUT, urlParams, requestParams, handler, false);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public void changeEmail(final String oldEmail, final String password, final String newEmail, final Firebase.ResultHandler handler) {
/* 758 */     checkServerSettings();
/* 759 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 762 */         Map<String, String> urlParams = new HashMap();
/* 763 */         urlParams.put("password", password);
/* 764 */         Map<String, String> requestParams = new HashMap();
/* 765 */         requestParams.put("email", newEmail);
/* 766 */         String url = String.format("/users/%s/email", new Object[] { oldEmail });
/* 767 */         AuthenticationManager.this.makeOperationRequest(url, HttpUtilities.HttpRequestType.PUT, urlParams, requestParams, handler, false);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   public void resetPassword(final String email, final Firebase.ResultHandler handler) {
/* 773 */     checkServerSettings();
/* 774 */     scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 777 */         String url = String.format("/users/%s/password", new Object[] { email });
/* 778 */         Map<String, String> params = Collections.emptyMap();
/* 779 */         AuthenticationManager.this.makeOperationRequest(url, HttpUtilities.HttpRequestType.POST, params, params, handler, false);
/*     */       }
/*     */     });
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/authentication/AuthenticationManager.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */