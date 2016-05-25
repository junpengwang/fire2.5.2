/*    */ package org.shaded.apache.http.auth.params;
/*    */ 
/*    */ import org.shaded.apache.http.params.HttpAbstractParamBean;
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
/*    */ public class AuthParamBean
/*    */   extends HttpAbstractParamBean
/*    */ {
/*    */   public AuthParamBean(HttpParams params)
/*    */   {
/* 43 */     super(params);
/*    */   }
/*    */   
/*    */   public void setCredentialCharset(String charset) {
/* 47 */     AuthParams.setCredentialCharset(this.params, charset);
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/auth/params/AuthParamBean.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       0.7.1
 */