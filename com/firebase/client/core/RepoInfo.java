/*    */ package com.firebase.client.core;
/*    */ 
/*    */ import java.net.URI;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class RepoInfo
/*    */ {
/*    */   private static final String VERSION_PARAM = "v";
/*    */   private static final String LAST_SESSION_ID_PARAM = "ls";
/*    */   public String host;
/*    */   public boolean secure;
/*    */   public String namespace;
/*    */   public String internalHost;
/*    */   
/*    */   public String toString()
/*    */   {
/* 22 */     return "http" + (this.secure ? "s" : "") + "://" + this.host;
/*    */   }
/*    */   
/*    */   public String toDebugString() {
/* 26 */     return "(host=" + this.host + ", secure=" + this.secure + ", ns=" + this.namespace + " internal=" + this.internalHost + ")";
/*    */   }
/*    */   
/*    */   public URI getConnectionURL(String optLastSessionId) {
/* 30 */     String scheme = this.secure ? "wss" : "ws";
/* 31 */     String url = scheme + "://" + this.internalHost + "/.ws?ns=" + this.namespace + "&" + "v" + "=" + "5";
/*    */     
/* 33 */     if (optLastSessionId != null) {
/* 34 */       url = url + "&ls=" + optLastSessionId;
/*    */     }
/* 36 */     return URI.create(url);
/*    */   }
/*    */   
/*    */   public boolean isCacheableHost() {
/* 40 */     return this.internalHost.startsWith("s-");
/*    */   }
/*    */   
/*    */   public boolean isSecure() {
/* 44 */     return this.secure;
/*    */   }
/*    */   
/*    */   public boolean isDemoHost() {
/* 48 */     return this.host.contains(".firebaseio-demo.com");
/*    */   }
/*    */   
/*    */   public boolean isCustomHost() {
/* 52 */     return (!this.host.contains(".firebaseio.com")) && (!this.host.contains(".firebaseio-demo.com"));
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/RepoInfo.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */