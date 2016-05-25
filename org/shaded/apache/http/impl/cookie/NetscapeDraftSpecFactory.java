/*    */ package org.shaded.apache.http.impl.cookie;
/*    */ 
/*    */ import java.util.Collection;
/*    */ import org.shaded.apache.http.annotation.Immutable;
/*    */ import org.shaded.apache.http.cookie.CookieSpec;
/*    */ import org.shaded.apache.http.cookie.CookieSpecFactory;
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
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ @Immutable
/*    */ public class NetscapeDraftSpecFactory
/*    */   implements CookieSpecFactory
/*    */ {
/*    */   public CookieSpec newInstance(HttpParams params)
/*    */   {
/* 55 */     if (params != null)
/*    */     {
/* 57 */       String[] patterns = null;
/* 58 */       Collection<?> param = (Collection)params.getParameter("http.protocol.cookie-datepatterns");
/*    */       
/* 60 */       if (param != null) {
/* 61 */         patterns = new String[param.size()];
/* 62 */         patterns = (String[])param.toArray(patterns);
/*    */       }
/* 64 */       return new NetscapeDraftSpec(patterns);
/*    */     }
/* 66 */     return new NetscapeDraftSpec();
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/impl/cookie/NetscapeDraftSpecFactory.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       0.7.1
 */