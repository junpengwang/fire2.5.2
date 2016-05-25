/*    */ package org.shaded.apache.http.params;
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
/*    */ public class HttpConnectionParamBean
/*    */   extends HttpAbstractParamBean
/*    */ {
/*    */   public HttpConnectionParamBean(HttpParams params)
/*    */   {
/* 44 */     super(params);
/*    */   }
/*    */   
/*    */   public void setSoTimeout(int soTimeout) {
/* 48 */     HttpConnectionParams.setSoTimeout(this.params, soTimeout);
/*    */   }
/*    */   
/*    */   public void setTcpNoDelay(boolean tcpNoDelay) {
/* 52 */     HttpConnectionParams.setTcpNoDelay(this.params, tcpNoDelay);
/*    */   }
/*    */   
/*    */   public void setSocketBufferSize(int socketBufferSize) {
/* 56 */     HttpConnectionParams.setSocketBufferSize(this.params, socketBufferSize);
/*    */   }
/*    */   
/*    */   public void setLinger(int linger) {
/* 60 */     HttpConnectionParams.setLinger(this.params, linger);
/*    */   }
/*    */   
/*    */   public void setConnectionTimeout(int connectionTimeout) {
/* 64 */     HttpConnectionParams.setConnectionTimeout(this.params, connectionTimeout);
/*    */   }
/*    */   
/*    */   public void setStaleCheckingEnabled(boolean staleCheckingEnabled) {
/* 68 */     HttpConnectionParams.setStaleCheckingEnabled(this.params, staleCheckingEnabled);
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/params/HttpConnectionParamBean.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */