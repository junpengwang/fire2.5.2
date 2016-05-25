/*    */ package com.shaded.fasterxml.jackson.core;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class JsonGenerationException
/*    */   extends JsonProcessingException
/*    */ {
/*    */   private static final long serialVersionUID = 123L;
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public JsonGenerationException(Throwable paramThrowable)
/*    */   {
/* 20 */     super(paramThrowable);
/*    */   }
/*    */   
/*    */   public JsonGenerationException(String paramString)
/*    */   {
/* 25 */     super(paramString, (JsonLocation)null);
/*    */   }
/*    */   
/*    */   public JsonGenerationException(String paramString, Throwable paramThrowable)
/*    */   {
/* 30 */     super(paramString, (JsonLocation)null, paramThrowable);
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/shaded/fasterxml/jackson/core/JsonGenerationException.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */