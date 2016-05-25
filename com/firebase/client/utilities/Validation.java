/*     */ package com.firebase.client.utilities;
/*     */ 
/*     */ import com.firebase.client.FirebaseException;
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.core.ValidationPath;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import com.firebase.client.snapshot.NodeUtilities;
/*     */ import com.firebase.client.snapshot.PriorityUtilities;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Map.Entry;
/*     */ import java.util.SortedMap;
/*     */ import java.util.TreeMap;
/*     */ import java.util.regex.Matcher;
/*     */ import java.util.regex.Pattern;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class Validation
/*     */ {
/*  27 */   private static final Pattern INVALID_PATH_REGEX = Pattern.compile("[\\[\\]\\.#$]");
/*  28 */   private static final Pattern INVALID_KEY_REGEX = Pattern.compile("[\\[\\]\\.#\\$\\/\\u0000-\\u001F\\u007F]");
/*     */   
/*     */   private static boolean isValidPathString(String pathString) {
/*  31 */     return !INVALID_PATH_REGEX.matcher(pathString).find();
/*     */   }
/*     */   
/*     */   public static void validatePathString(String pathString) throws FirebaseException {
/*  35 */     if (!isValidPathString(pathString)) {
/*  36 */       throw new FirebaseException("Invalid Firebase path: " + pathString + ". Firebase paths must not contain '.', '#', '$', '[', or ']'");
/*     */     }
/*     */   }
/*     */   
/*     */   public static void validateRootPathString(String pathString) throws FirebaseException
/*     */   {
/*  42 */     if (pathString.startsWith(".info")) {
/*  43 */       validatePathString(pathString.substring(5));
/*  44 */     } else if (pathString.startsWith("/.info")) {
/*  45 */       validatePathString(pathString.substring(6));
/*     */     } else {
/*  47 */       validatePathString(pathString);
/*     */     }
/*     */   }
/*     */   
/*     */   private static boolean isWritableKey(String key) {
/*  52 */     return (key != null) && (key.length() > 0) && ((key.equals(".value")) || (key.equals(".priority")) || ((!key.startsWith(".")) && (!INVALID_KEY_REGEX.matcher(key).find())));
/*     */   }
/*     */   
/*     */ 
/*     */   private static boolean isValidKey(String key)
/*     */   {
/*  58 */     return (key.equals(".info")) || (!INVALID_KEY_REGEX.matcher(key).find());
/*     */   }
/*     */   
/*     */   public static void validateNullableKey(String key) throws FirebaseException {
/*  62 */     if ((key != null) && (!isValidKey(key))) {
/*  63 */       throw new FirebaseException("Invalid key: " + key + ". Keys must not contain '/', '.', '#', '$', '[', or ']'");
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private static boolean isWritablePath(Path path)
/*     */   {
/*  71 */     ChildKey front = path.getFront();
/*  72 */     return (front == null) || (!front.asString().startsWith("."));
/*     */   }
/*     */   
/*     */   public static void validateWritableObject(Object object)
/*     */   {
/*  77 */     if ((object instanceof Map)) {
/*  78 */       Map<String, Object> map = (Map)object;
/*  79 */       if (map.containsKey(".sv"))
/*     */       {
/*  81 */         return;
/*     */       }
/*  83 */       for (Map.Entry<String, Object> entry : map.entrySet()) {
/*  84 */         validateWritableKey((String)entry.getKey());
/*  85 */         validateWritableObject(entry.getValue());
/*     */       }
/*  87 */     } else if ((object instanceof List)) {
/*  88 */       List<Object> list = (List)object;
/*  89 */       for (Object child : list) {
/*  90 */         validateWritableObject(child);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public static void validateWritableKey(String key)
/*     */     throws FirebaseException
/*     */   {
/*  98 */     if (!isWritableKey(key)) {
/*  99 */       throw new FirebaseException("Invalid key: " + key + ". Keys must not contain '/', '.', '#', '$', '[', or ']'");
/*     */     }
/*     */   }
/*     */   
/*     */   public static void validateWritablePath(Path path) throws FirebaseException
/*     */   {
/* 105 */     if (!isWritablePath(path)) {
/* 106 */       throw new FirebaseException("Invalid write location: " + path.toString());
/*     */     }
/*     */   }
/*     */   
/*     */   public static Map<Path, Node> parseAndValidateUpdate(Path path, Map<String, Object> update) throws FirebaseException {
/* 111 */     SortedMap<Path, Node> parsedUpdate = new TreeMap();
/* 112 */     for (Map.Entry<String, Object> entry : update.entrySet()) {
/* 113 */       Path updatePath = new Path((String)entry.getKey());
/* 114 */       Object newValue = entry.getValue();
/* 115 */       ValidationPath.validateWithObject(path.child(updatePath), newValue);
/* 116 */       String childName = !updatePath.isEmpty() ? updatePath.getBack().asString() : "";
/* 117 */       if ((childName.equals(".sv")) || (childName.equals(".value"))) {
/* 118 */         throw new FirebaseException("Path '" + updatePath + "' contains disallowed child name: " + childName);
/*     */       }
/* 120 */       if ((childName.equals(".priority")) && 
/* 121 */         (!PriorityUtilities.isValidPriority(NodeUtilities.NodeFromJSON(newValue)))) {
/* 122 */         throw new FirebaseException("Path '" + updatePath + "' contains invalid priority " + "(must be a string, double, ServerValue, or null).");
/*     */       }
/*     */       
/*     */ 
/* 126 */       validateWritableObject(newValue);
/* 127 */       parsedUpdate.put(updatePath, NodeUtilities.NodeFromJSON(newValue));
/*     */     }
/*     */     
/* 130 */     Path prevPath = null;
/* 131 */     for (Path curPath : parsedUpdate.keySet())
/*     */     {
/* 133 */       Utilities.hardAssert((prevPath == null) || (prevPath.compareTo(curPath) < 0));
/* 134 */       if ((prevPath != null) && (prevPath.contains(curPath))) {
/* 135 */         throw new FirebaseException("Path '" + prevPath + "' is an ancestor of '" + curPath + "' in an update.");
/*     */       }
/* 137 */       prevPath = curPath;
/*     */     }
/* 139 */     return parsedUpdate;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/utilities/Validation.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */