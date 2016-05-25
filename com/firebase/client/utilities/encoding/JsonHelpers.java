/*    */ package com.firebase.client.utilities.encoding;
/*    */ 
/*    */ import com.shaded.fasterxml.jackson.databind.ObjectMapper;
/*    */ 
/*    */ public class JsonHelpers
/*    */ {
/*  7 */   private static final ObjectMapper mapperInstance = new ObjectMapper();
/*    */   
/*    */   public static ObjectMapper getMapper() {
/* 10 */     return mapperInstance;
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/utilities/encoding/JsonHelpers.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */