/*    */ package org.shaded.apache.http.impl.auth;
/*    */ 
/*    */ import org.shaded.apache.http.annotation.Immutable;
/*    */ import org.shaded.apache.http.auth.AuthScheme;
/*    */ import org.shaded.apache.http.auth.AuthSchemeFactory;
/*    */ import org.shaded.apache.http.params.HttpParams;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ @Immutable
/*    */ public class BasicSchemeFactory
/*    */   implements AuthSchemeFactory
/*    */ {
/*    */   public AuthScheme newInstance(HttpParams params)
/*    */   {
/* 46 */     return new BasicScheme();
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/impl/auth/BasicSchemeFactory.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       0.7.1
 */