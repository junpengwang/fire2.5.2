/*    */ package org.shaded.apache.commons.logging;
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
/*    */ public class LogConfigurationException
/*    */   extends RuntimeException
/*    */ {
/*    */   public LogConfigurationException() {}
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
/*    */   public LogConfigurationException(String message)
/*    */   {
/* 50 */     super(message);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public LogConfigurationException(Throwable cause)
/*    */   {
/* 63 */     this(cause == null ? null : cause.toString(), cause);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public LogConfigurationException(String message, Throwable cause)
/*    */   {
/* 76 */     super(message + " (Caused by " + cause + ")");
/* 77 */     this.cause = cause;
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/* 85 */   protected Throwable cause = null;
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public Throwable getCause()
/*    */   {
/* 93 */     return this.cause;
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/commons/logging/LogConfigurationException.class
 * Java compiler version: 1 (45.3)
 * JD-Core Version:       0.7.1
 */