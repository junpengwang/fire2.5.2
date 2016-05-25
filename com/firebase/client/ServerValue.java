/*    */ package com.firebase.client;
/*    */ 
/*    */ import java.util.Collections;
/*    */ import java.util.HashMap;
/*    */ import java.util.Map;
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
/*    */ public class ServerValue
/*    */ {
/* 25 */   public static final Map<String, String> TIMESTAMP = createServerValuePlaceholder("timestamp");
/*    */   
/*    */   private static Map<String, String> createServerValuePlaceholder(String key) {
/* 28 */     Map<String, String> result = new HashMap();
/* 29 */     result.put(".sv", key);
/* 30 */     return Collections.unmodifiableMap(result);
/*    */   }
/*    */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/ServerValue.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */