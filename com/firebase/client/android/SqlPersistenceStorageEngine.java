/*     */ package com.firebase.client.android;
/*     */ 
/*     */ import android.content.ContentValues;
/*     */ import android.database.Cursor;
/*     */ import android.database.sqlite.SQLiteDatabase;
/*     */ import android.database.sqlite.SQLiteOpenHelper;
/*     */ import com.firebase.client.core.CompoundWrite;
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.core.UserWriteRecord;
/*     */ import com.firebase.client.core.persistence.PersistenceStorageEngine;
/*     */ import com.firebase.client.core.persistence.PruneForest;
/*     */ import com.firebase.client.core.persistence.TrackedQuery;
/*     */ import com.firebase.client.core.utilities.ImmutableTree;
/*     */ import com.firebase.client.core.utilities.ImmutableTree.TreeVisitor;
/*     */ import com.firebase.client.core.view.QueryParams;
/*     */ import com.firebase.client.core.view.QuerySpec;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.ChildrenNode;
/*     */ import com.firebase.client.snapshot.EmptyNode;
/*     */ import com.firebase.client.snapshot.NamedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import com.firebase.client.snapshot.NodeUtilities;
/*     */ import com.firebase.client.utilities.LogWrapper;
/*     */ import com.firebase.client.utilities.NodeSizeEstimator;
/*     */ import com.firebase.client.utilities.Pair;
/*     */ import com.firebase.client.utilities.Utilities;
/*     */ import com.shaded.fasterxml.jackson.databind.ObjectMapper;
/*     */ import java.io.IOException;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.net.URLEncoder;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collection;
/*     */ import java.util.Collections;
/*     */ import java.util.HashMap;
/*     */ import java.util.HashSet;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Map.Entry;
/*     */ import java.util.Set;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class SqlPersistenceStorageEngine
/*     */   implements PersistenceStorageEngine
/*     */ {
/*     */   private static final String createServerCache = "CREATE TABLE serverCache (path TEXT PRIMARY KEY, value BLOB);";
/*     */   private static final String SERVER_CACHE_TABLE = "serverCache";
/*     */   private static final String PATH_COLUMN_NAME = "path";
/*     */   private static final String VALUE_COLUMN_NAME = "value";
/*     */   private static final String createWrites = "CREATE TABLE writes (id INTEGER, path TEXT, type TEXT, part INTEGER, node BLOB, UNIQUE (id, part));";
/*     */   private static final String WRITES_TABLE = "writes";
/*     */   private static final String WRITE_ID_COLUMN_NAME = "id";
/*     */   private static final String WRITE_NODE_COLUMN_NAME = "node";
/*     */   private static final String WRITE_PART_COLUMN_NAME = "part";
/*     */   private static final String WRITE_TYPE_COLUMN_NAME = "type";
/*     */   private static final String WRITE_TYPE_OVERWRITE = "o";
/*     */   private static final String WRITE_TYPE_MERGE = "m";
/*     */   private static final String createTrackedQueries = "CREATE TABLE trackedQueries (id INTEGER PRIMARY KEY, path TEXT, queryParams TEXT, lastUse INTEGER, complete INTEGER, active INTEGER);";
/*     */   private static final String TRACKED_QUERY_TABLE = "trackedQueries";
/*     */   private static final String TRACKED_QUERY_ID_COLUMN_NAME = "id";
/*     */   private static final String TRACKED_QUERY_PATH_COLUMN_NAME = "path";
/*     */   private static final String TRACKED_QUERY_PARAMS_COLUMN_NAME = "queryParams";
/*     */   private static final String TRACKED_QUERY_LAST_USE_COLUMN_NAME = "lastUse";
/*     */   private static final String TRACKED_QUERY_COMPLETE_COLUMN_NAME = "complete";
/*     */   private static final String TRACKED_QUERY_ACTIVE_COLUMN_NAME = "active";
/*     */   private static final String createTrackedKeys = "CREATE TABLE trackedKeys (id INTEGER, key TEXT);";
/*     */   private static final String TRACKED_KEYS_TABLE = "trackedKeys";
/*     */   private static final String TRACKED_KEYS_ID_COLUMN_NAME = "id";
/*     */   private static final String TRACKED_KEYS_KEY_COLUMN_NAME = "key";
/*     */   private static final String ROW_ID_COLUMN_NAME = "rowid";
/*     */   private static final int CHILDREN_NODE_SPLIT_SIZE_THRESHOLD = 16384;
/*     */   private static final int ROW_SPLIT_SIZE = 262144;
/*     */   private static final String PART_KEY_FORMAT = ".part-%04d";
/*     */   private static final String FIRST_PART_KEY = ".part-0000";
/*     */   private static final String PART_KEY_PREFIX = ".part-";
/*     */   private static final String LOGGER_COMPONENT = "Persistence";
/*     */   private final SQLiteDatabase database;
/*     */   private final ObjectMapper jsonMapper;
/*     */   private final LogWrapper logger;
/*     */   
/*     */   private static class PersistentCacheOpenHelper
/*     */     extends SQLiteOpenHelper
/*     */   {
/*     */     private static final int DATABASE_VERSION = 2;
/*     */     
/*     */     public PersistentCacheOpenHelper(android.content.Context context, String cacheId)
/*     */     {
/* 143 */       super(cacheId, null, 2);
/*     */     }
/*     */     
/*     */     public void onCreate(SQLiteDatabase db)
/*     */     {
/* 148 */       db.execSQL("CREATE TABLE serverCache (path TEXT PRIMARY KEY, value BLOB);");
/* 149 */       db.execSQL("CREATE TABLE writes (id INTEGER, path TEXT, type TEXT, part INTEGER, node BLOB, UNIQUE (id, part));");
/* 150 */       db.execSQL("CREATE TABLE trackedQueries (id INTEGER PRIMARY KEY, path TEXT, queryParams TEXT, lastUse INTEGER, complete INTEGER, active INTEGER);");
/* 151 */       db.execSQL("CREATE TABLE trackedKeys (id INTEGER, key TEXT);");
/*     */     }
/*     */     
/*     */     public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
/*     */     {
/* 156 */       assert (newVersion == 2) : "Why is onUpgrade() called with a different version?";
/* 157 */       if (oldVersion <= 1)
/*     */       {
/*     */ 
/*     */ 
/* 161 */         dropTable(db, "serverCache");
/* 162 */         db.execSQL("CREATE TABLE serverCache (path TEXT PRIMARY KEY, value BLOB);");
/*     */         
/*     */ 
/* 165 */         dropTable(db, "complete");
/*     */         
/*     */ 
/* 168 */         db.execSQL("CREATE TABLE trackedKeys (id INTEGER, key TEXT);");
/* 169 */         db.execSQL("CREATE TABLE trackedQueries (id INTEGER PRIMARY KEY, path TEXT, queryParams TEXT, lastUse INTEGER, complete INTEGER, active INTEGER);");
/*     */       } else {
/* 171 */         throw new AssertionError("We don't handle upgrading to " + newVersion);
/*     */       }
/*     */     }
/*     */     
/*     */     private void dropTable(SQLiteDatabase db, String table) {
/* 176 */       db.execSQL("DROP TABLE IF EXISTS " + table);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/* 185 */   private long transactionStart = 0L;
/*     */   
/*     */   public SqlPersistenceStorageEngine(android.content.Context context, com.firebase.client.core.Context firebaseContext, String cacheId) {
/*     */     String sanitizedCacheId;
/*     */     try {
/* 190 */       sanitizedCacheId = URLEncoder.encode(cacheId, "utf-8");
/*     */     } catch (IOException e) {
/* 192 */       throw new RuntimeException(e);
/*     */     }
/* 194 */     PersistentCacheOpenHelper helper = new PersistentCacheOpenHelper(context, sanitizedCacheId);
/*     */     
/*     */ 
/*     */ 
/* 198 */     this.database = helper.getWritableDatabase();
/* 199 */     this.jsonMapper = new ObjectMapper();
/* 200 */     this.logger = firebaseContext.getLogger("Persistence");
/*     */   }
/*     */   
/*     */   public void saveUserOverwrite(Path path, Node node, long writeId)
/*     */   {
/* 205 */     verifyInsideTransaction();
/* 206 */     long start = System.currentTimeMillis();
/* 207 */     byte[] serializedNode = serializeObject(node.getValue(true));
/* 208 */     saveWrite(path, writeId, "o", serializedNode);
/* 209 */     long duration = System.currentTimeMillis() - start;
/* 210 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Persisted user overwrite in %dms", new Object[] { Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public void saveUserMerge(Path path, CompoundWrite children, long writeId)
/*     */   {
/* 215 */     verifyInsideTransaction();
/* 216 */     long start = System.currentTimeMillis();
/* 217 */     byte[] serializedNode = serializeObject(children.getValue(true));
/* 218 */     saveWrite(path, writeId, "m", serializedNode);
/* 219 */     long duration = System.currentTimeMillis() - start;
/* 220 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Persisted user merge in %dms", new Object[] { Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public void removeUserWrite(long writeId)
/*     */   {
/* 225 */     verifyInsideTransaction();
/* 226 */     long start = System.currentTimeMillis();
/* 227 */     int count = this.database.delete("writes", "id = ?", new String[] { String.valueOf(writeId) });
/* 228 */     long duration = System.currentTimeMillis() - start;
/* 229 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Deleted %d write(s) with writeId %d in %dms", new Object[] { Integer.valueOf(count), Long.valueOf(writeId), Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public List<UserWriteRecord> loadUserWrites()
/*     */   {
/* 234 */     String[] columns = { "id", "path", "type", "part", "node" };
/* 235 */     long start = System.currentTimeMillis();
/* 236 */     Cursor cursor = this.database.query("writes", columns, null, null, null, null, "id, part");
/*     */     
/* 238 */     List<UserWriteRecord> writes = new ArrayList();
/*     */     try { Path path;
/* 240 */       while (cursor.moveToNext()) {
/* 241 */         long writeId = cursor.getLong(0);
/* 242 */         path = new Path(cursor.getString(1));
/* 243 */         String type = cursor.getString(2);
/*     */         byte[] serialized;
/* 245 */         byte[] serialized; if (cursor.isNull(3))
/*     */         {
/* 247 */           serialized = cursor.getBlob(4);
/*     */         }
/*     */         else {
/* 250 */           List<byte[]> parts = new ArrayList();
/*     */           do {
/* 252 */             parts.add(cursor.getBlob(4));
/* 253 */           } while ((cursor.moveToNext()) && (cursor.getLong(0) == writeId));
/*     */           
/* 255 */           cursor.moveToPrevious();
/* 256 */           serialized = joinBytes(parts);
/*     */         }
/* 258 */         Object writeValue = this.jsonMapper.readValue(serialized, Object.class);
/*     */         UserWriteRecord record;
/* 260 */         if ("o".equals(type)) {
/* 261 */           Node set = NodeUtilities.NodeFromJSON(writeValue);
/* 262 */           record = new UserWriteRecord(writeId, path, set, true); } else { UserWriteRecord record;
/* 263 */           if ("m".equals(type))
/*     */           {
/* 265 */             CompoundWrite merge = CompoundWrite.fromValue((Map)writeValue);
/* 266 */             record = new UserWriteRecord(writeId, path, merge);
/*     */           } else {
/* 268 */             throw new IllegalStateException("Got invalid write type: " + type); } }
/*     */         UserWriteRecord record;
/* 270 */         writes.add(record);
/*     */       }
/* 272 */       long duration = System.currentTimeMillis() - start;
/* 273 */       if (this.logger.logsDebug()) this.logger.debug(String.format("Loaded %d writes in %dms", new Object[] { Integer.valueOf(writes.size()), Long.valueOf(duration) }));
/* 274 */       return writes;
/*     */     } catch (IOException e) {
/* 276 */       throw new RuntimeException("Failed to load writes", e);
/*     */     } finally {
/* 278 */       cursor.close();
/*     */     }
/*     */   }
/*     */   
/*     */   private void saveWrite(Path path, long writeId, String type, byte[] serializedWrite) {
/* 283 */     verifyInsideTransaction();
/* 284 */     this.database.delete("writes", "id = ?", new String[] { String.valueOf(writeId) });
/* 285 */     if (serializedWrite.length >= 262144) {
/* 286 */       List<byte[]> parts = splitBytes(serializedWrite, 262144);
/* 287 */       for (int i = 0; i < parts.size(); i++) {
/* 288 */         ContentValues values = new ContentValues();
/* 289 */         values.put("id", Long.valueOf(writeId));
/* 290 */         values.put("path", pathToKey(path));
/* 291 */         values.put("type", type);
/* 292 */         values.put("part", Integer.valueOf(i));
/* 293 */         values.put("node", (byte[])parts.get(i));
/* 294 */         this.database.insertWithOnConflict("writes", null, values, 5);
/*     */       }
/*     */     } else {
/* 297 */       ContentValues values = new ContentValues();
/* 298 */       values.put("id", Long.valueOf(writeId));
/* 299 */       values.put("path", pathToKey(path));
/* 300 */       values.put("type", type);
/* 301 */       values.put("part", (Integer)null);
/* 302 */       values.put("node", serializedWrite);
/* 303 */       this.database.insertWithOnConflict("writes", null, values, 5);
/*     */     }
/*     */   }
/*     */   
/*     */   public Node serverCache(Path path)
/*     */   {
/* 309 */     return loadNested(path);
/*     */   }
/*     */   
/*     */   public void overwriteServerCache(Path path, Node node)
/*     */   {
/* 314 */     verifyInsideTransaction();
/* 315 */     updateServerCache(path, node, false);
/*     */   }
/*     */   
/*     */   public void mergeIntoServerCache(Path path, Node node)
/*     */   {
/* 320 */     verifyInsideTransaction();
/* 321 */     updateServerCache(path, node, true);
/*     */   }
/*     */   
/*     */   private void updateServerCache(Path path, Node node, boolean merge) {
/* 325 */     long start = System.currentTimeMillis();
/*     */     int savedRows;
/*     */     int removedRows;
/* 328 */     int savedRows; if (!merge) {
/* 329 */       int removedRows = removeNested("serverCache", path);
/* 330 */       savedRows = saveNested(path, node);
/*     */     } else {
/* 332 */       removedRows = 0;
/* 333 */       savedRows = 0;
/* 334 */       for (NamedNode child : node) {
/* 335 */         removedRows += removeNested("serverCache", path.child(child.getName()));
/* 336 */         savedRows += saveNested(path.child(child.getName()), child.getNode());
/*     */       }
/*     */     }
/* 339 */     long duration = System.currentTimeMillis() - start;
/* 340 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Persisted a total of %d rows and deleted %d rows for a set at %s in %dms", new Object[] { Integer.valueOf(savedRows), Integer.valueOf(removedRows), path.toString(), Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public void mergeIntoServerCache(Path path, CompoundWrite children)
/*     */   {
/* 345 */     verifyInsideTransaction();
/* 346 */     long start = System.currentTimeMillis();
/* 347 */     int savedRows = 0;
/* 348 */     int removedRows = 0;
/* 349 */     for (Map.Entry<Path, Node> entry : children) {
/* 350 */       removedRows += removeNested("serverCache", path.child((Path)entry.getKey()));
/* 351 */       savedRows += saveNested(path.child((Path)entry.getKey()), (Node)entry.getValue());
/*     */     }
/* 353 */     long duration = System.currentTimeMillis() - start;
/* 354 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Persisted a total of %d rows and deleted %d rows for a merge at %s in %dms", new Object[] { Integer.valueOf(savedRows), Integer.valueOf(removedRows), path.toString(), Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public long serverCacheEstimatedSizeInBytes()
/*     */   {
/* 359 */     String query = String.format("SELECT sum(length(%s) + length(%s)) FROM %s", new Object[] { "value", "path", "serverCache" });
/* 360 */     Cursor cursor = this.database.rawQuery(query, null);
/*     */     try {
/* 362 */       if (cursor.moveToFirst()) {
/* 363 */         return cursor.getLong(0);
/*     */       }
/* 365 */       throw new IllegalStateException("Couldn't read database result!");
/*     */     }
/*     */     finally {
/* 368 */       cursor.close();
/*     */     }
/*     */   }
/*     */   
/*     */   public void saveTrackedQuery(TrackedQuery trackedQuery)
/*     */   {
/* 374 */     verifyInsideTransaction();
/* 375 */     long start = System.currentTimeMillis();
/* 376 */     ContentValues values = new ContentValues();
/* 377 */     values.put("id", Long.valueOf(trackedQuery.id));
/* 378 */     values.put("path", pathToKey(trackedQuery.querySpec.getPath()));
/* 379 */     values.put("queryParams", trackedQuery.querySpec.getParams().toJSON());
/* 380 */     values.put("lastUse", Long.valueOf(trackedQuery.lastUse));
/* 381 */     values.put("complete", Boolean.valueOf(trackedQuery.complete));
/* 382 */     values.put("active", Boolean.valueOf(trackedQuery.active));
/* 383 */     this.database.insertWithOnConflict("trackedQueries", null, values, 5);
/* 384 */     long duration = System.currentTimeMillis() - start;
/* 385 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Saved new tracked query in %dms", new Object[] { Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public void deleteTrackedQuery(long trackedQueryId)
/*     */   {
/* 390 */     verifyInsideTransaction();
/* 391 */     String trackedQueryIdStr = String.valueOf(trackedQueryId);
/* 392 */     String queriesWhereClause = "id = ?";
/* 393 */     this.database.delete("trackedQueries", queriesWhereClause, new String[] { trackedQueryIdStr });
/*     */     
/* 395 */     String keysWhereClause = "id = ?";
/* 396 */     this.database.delete("trackedKeys", keysWhereClause, new String[] { trackedQueryIdStr });
/*     */   }
/*     */   
/*     */   public List<TrackedQuery> loadTrackedQueries()
/*     */   {
/* 401 */     String[] columns = { "id", "path", "queryParams", "lastUse", "complete", "active" };
/*     */     
/* 403 */     long start = System.currentTimeMillis();
/* 404 */     Cursor cursor = this.database.query("trackedQueries", columns, null, null, null, null, "id");
/*     */     
/* 406 */     List<TrackedQuery> queries = new ArrayList();
/*     */     try { Path path;
/* 408 */       while (cursor.moveToNext()) {
/* 409 */         long id = cursor.getLong(0);
/* 410 */         path = new Path(cursor.getString(1));
/* 411 */         String paramsStr = cursor.getString(2);
/*     */         Object paramsObject;
/*     */         try {
/* 414 */           paramsObject = this.jsonMapper.readValue(paramsStr, Object.class);
/*     */         } catch (IOException e) {
/* 416 */           throw new RuntimeException(e);
/*     */         }
/*     */         
/* 419 */         QuerySpec query = QuerySpec.fromPathAndQueryObject(path, (Map)paramsObject);
/* 420 */         long lastUse = cursor.getLong(3);
/* 421 */         boolean complete = cursor.getInt(4) != 0;
/* 422 */         boolean active = cursor.getInt(5) != 0;
/* 423 */         TrackedQuery trackedQuery = new TrackedQuery(id, query, lastUse, complete, active);
/* 424 */         queries.add(trackedQuery);
/*     */       }
/* 426 */       long duration = System.currentTimeMillis() - start;
/* 427 */       if (this.logger.logsDebug()) this.logger.debug(String.format("Loaded %d tracked queries in %dms", new Object[] { Integer.valueOf(queries.size()), Long.valueOf(duration) }));
/* 428 */       return queries;
/*     */     } finally {
/* 430 */       cursor.close();
/*     */     }
/*     */   }
/*     */   
/*     */   public void resetPreviouslyActiveTrackedQueries(long lastUse)
/*     */   {
/* 436 */     verifyInsideTransaction();
/* 437 */     long start = System.currentTimeMillis();
/*     */     
/* 439 */     String whereClause = "active = 1";
/*     */     
/* 441 */     ContentValues values = new ContentValues();
/* 442 */     values.put("active", Boolean.valueOf(false));
/* 443 */     values.put("lastUse", Long.valueOf(lastUse));
/*     */     
/* 445 */     this.database.updateWithOnConflict("trackedQueries", values, whereClause, new String[0], 5);
/* 446 */     long duration = System.currentTimeMillis() - start;
/* 447 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Reset active tracked queries in %dms", new Object[] { Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public void saveTrackedQueryKeys(long trackedQueryId, Set<ChildKey> keys)
/*     */   {
/* 452 */     verifyInsideTransaction();
/* 453 */     long start = System.currentTimeMillis();
/*     */     
/* 455 */     String trackedQueryIdStr = String.valueOf(trackedQueryId);
/* 456 */     String keysWhereClause = "id = ?";
/* 457 */     this.database.delete("trackedKeys", keysWhereClause, new String[] { trackedQueryIdStr });
/*     */     
/* 459 */     for (ChildKey addedKey : keys) {
/* 460 */       ContentValues values = new ContentValues();
/* 461 */       values.put("id", Long.valueOf(trackedQueryId));
/* 462 */       values.put("key", addedKey.asString());
/* 463 */       this.database.insertWithOnConflict("trackedKeys", null, values, 5);
/*     */     }
/*     */     
/* 466 */     long duration = System.currentTimeMillis() - start;
/* 467 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Set %d tracked query keys for tracked query %d in %dms", new Object[] { Integer.valueOf(keys.size()), Long.valueOf(trackedQueryId), Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public void updateTrackedQueryKeys(long trackedQueryId, Set<ChildKey> added, Set<ChildKey> removed)
/*     */   {
/* 472 */     verifyInsideTransaction();
/* 473 */     long start = System.currentTimeMillis();
/* 474 */     String whereClause = "id = ? AND key = ?";
/* 475 */     String trackedQueryIdStr = String.valueOf(trackedQueryId);
/* 476 */     for (ChildKey removedKey : removed) {
/* 477 */       this.database.delete("trackedKeys", whereClause, new String[] { trackedQueryIdStr, removedKey.asString() });
/*     */     }
/* 479 */     for (ChildKey addedKey : added) {
/* 480 */       ContentValues values = new ContentValues();
/* 481 */       values.put("id", Long.valueOf(trackedQueryId));
/* 482 */       values.put("key", addedKey.asString());
/* 483 */       this.database.insertWithOnConflict("trackedKeys", null, values, 5);
/*     */     }
/* 485 */     long duration = System.currentTimeMillis() - start;
/* 486 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Updated tracked query keys (%d added, %d removed) for tracked query id %d in %dms", new Object[] { Integer.valueOf(added.size()), Integer.valueOf(removed.size()), Long.valueOf(trackedQueryId), Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public Set<ChildKey> loadTrackedQueryKeys(long trackedQueryId)
/*     */   {
/* 491 */     return loadTrackedQueryKeys(Collections.singleton(Long.valueOf(trackedQueryId)));
/*     */   }
/*     */   
/*     */   public Set<ChildKey> loadTrackedQueryKeys(Set<Long> trackedQueryIds)
/*     */   {
/* 496 */     String[] columns = { "key" };
/* 497 */     long start = System.currentTimeMillis();
/* 498 */     String whereClause = "id IN (" + commaSeparatedList(trackedQueryIds) + ")";
/* 499 */     Cursor cursor = this.database.query(true, "trackedKeys", columns, whereClause, null, null, null, null, null);
/*     */     
/* 501 */     Set<ChildKey> keys = new HashSet();
/*     */     try {
/* 503 */       while (cursor.moveToNext()) {
/* 504 */         String key = cursor.getString(0);
/* 505 */         keys.add(ChildKey.fromString(key));
/*     */       }
/* 507 */       long duration = System.currentTimeMillis() - start;
/* 508 */       if (this.logger.logsDebug()) this.logger.debug(String.format("Loaded %d tracked queries keys for tracked queries %s in %dms", new Object[] { Integer.valueOf(keys.size()), trackedQueryIds.toString(), Long.valueOf(duration) }));
/* 509 */       return keys;
/*     */     } finally {
/* 511 */       cursor.close();
/*     */     }
/*     */   }
/*     */   
/*     */   public void pruneCache(Path root, PruneForest pruneForest)
/*     */   {
/* 517 */     if (!pruneForest.prunesAnything())
/* 518 */       return;
/* 519 */     verifyInsideTransaction();
/* 520 */     long start = System.currentTimeMillis();
/* 521 */     Cursor cursor = loadNestedQuery(root, new String[] { "rowid", "path" });
/* 522 */     ImmutableTree<Long> rowIdsToPrune = new ImmutableTree(null);
/* 523 */     ImmutableTree<Long> rowIdsToKeep = new ImmutableTree(null);
/* 524 */     while (cursor.moveToNext()) {
/* 525 */       long rowId = cursor.getLong(0);
/* 526 */       Path rowPath = new Path(cursor.getString(1));
/* 527 */       if (!root.contains(rowPath)) {
/* 528 */         this.logger.warn("We are pruning at " + root + " but we have data stored higher up at " + rowPath + ". Ignoring.");
/*     */       } else {
/* 530 */         Path relativePath = Path.getRelative(root, rowPath);
/* 531 */         if (pruneForest.shouldPruneUnkeptDescendants(relativePath)) {
/* 532 */           rowIdsToPrune = rowIdsToPrune.set(relativePath, Long.valueOf(rowId));
/* 533 */         } else if (pruneForest.shouldKeep(relativePath)) {
/* 534 */           rowIdsToKeep = rowIdsToKeep.set(relativePath, Long.valueOf(rowId));
/*     */ 
/*     */         }
/*     */         else
/*     */         {
/*     */ 
/* 540 */           this.logger.warn("We are pruning at " + root + " and have data at " + rowPath + " that isn't marked for pruning or keeping. Ignoring.");
/*     */         }
/*     */       }
/*     */     }
/*     */     
/* 545 */     int prunedCount = 0;int resavedCount = 0;
/* 546 */     if (!rowIdsToPrune.isEmpty()) {
/* 547 */       List<Pair<Path, Node>> rowsToResave = new ArrayList();
/* 548 */       pruneTreeRecursive(root, Path.getEmptyPath(), rowIdsToPrune, rowIdsToKeep, pruneForest, rowsToResave);
/*     */       
/* 550 */       Collection<Long> rowIdsToDelete = rowIdsToPrune.values();
/* 551 */       String whereClause = "rowid IN (" + commaSeparatedList(rowIdsToDelete) + ")";
/* 552 */       this.database.delete("serverCache", whereClause, null);
/*     */       
/* 554 */       for (Pair<Path, Node> node : rowsToResave) {
/* 555 */         saveNested(root.child((Path)node.getFirst()), (Node)node.getSecond());
/*     */       }
/*     */       
/* 558 */       prunedCount = rowIdsToDelete.size();
/* 559 */       resavedCount = rowsToResave.size();
/*     */     }
/* 561 */     long duration = System.currentTimeMillis() - start;
/* 562 */     if (this.logger.logsDebug()) { this.logger.debug(String.format("Pruned %d rows with %d nodes resaved in %dms", new Object[] { Integer.valueOf(prunedCount), Integer.valueOf(resavedCount), Long.valueOf(duration) }));
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   private void pruneTreeRecursive(Path pruneRoot, final Path relativePath, ImmutableTree<Long> rowIdsToPrune, final ImmutableTree<Long> rowIdsToKeep, PruneForest pruneForest, final List<Pair<Path, Node>> rowsToResaveAccumulator)
/*     */   {
/* 569 */     if (rowIdsToPrune.getValue() != null)
/*     */     {
/* 571 */       int nodesToResave = ((Integer)pruneForest.foldKeptNodes(Integer.valueOf(0), new ImmutableTree.TreeVisitor()
/*     */       {
/*     */         public Integer onNodeValue(Path keepPath, Void ignore, Integer nodesToResave)
/*     */         {
/* 575 */           return Integer.valueOf(rowIdsToKeep.get(keepPath) == null ? nodesToResave.intValue() + 1 : nodesToResave.intValue());
/*     */         }
/*     */       })).intValue();
/* 578 */       if (nodesToResave > 0) {
/* 579 */         Path absolutePath = pruneRoot.child(relativePath);
/* 580 */         if (this.logger.logsDebug()) this.logger.debug(String.format("Need to rewrite %d nodes below path %s", new Object[] { Integer.valueOf(nodesToResave), absolutePath }));
/* 581 */         final Node currentNode = loadNested(absolutePath);
/* 582 */         pruneForest.foldKeptNodes(null, new ImmutableTree.TreeVisitor()
/*     */         {
/*     */           public Void onNodeValue(Path keepPath, Void ignore, Void ignore2)
/*     */           {
/* 586 */             if (rowIdsToKeep.get(keepPath) == null) {
/* 587 */               rowsToResaveAccumulator.add(new Pair(relativePath.child(keepPath), currentNode.getChild(keepPath)));
/*     */             }
/* 589 */             return null;
/*     */           }
/*     */         });
/*     */       }
/*     */     }
/*     */     else {
/* 595 */       for (Map.Entry<ChildKey, ImmutableTree<Long>> entry : rowIdsToPrune.getChildren()) {
/* 596 */         ChildKey childKey = (ChildKey)entry.getKey();
/* 597 */         PruneForest childPruneForest = pruneForest.child((ChildKey)entry.getKey());
/* 598 */         pruneTreeRecursive(pruneRoot, relativePath.child(childKey), (ImmutableTree)entry.getValue(), rowIdsToKeep.getChild(childKey), childPruneForest, rowsToResaveAccumulator);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public void removeAllUserWrites()
/*     */   {
/* 606 */     verifyInsideTransaction();
/* 607 */     long start = System.currentTimeMillis();
/* 608 */     int count = this.database.delete("writes", null, null);
/* 609 */     long duration = System.currentTimeMillis() - start;
/* 610 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Deleted %d (all) write(s) in %dms", new Object[] { Integer.valueOf(count), Long.valueOf(duration) }));
/*     */   }
/*     */   
/*     */   public void purgeCache()
/*     */   {
/* 615 */     verifyInsideTransaction();
/* 616 */     this.database.delete("serverCache", null, null);
/* 617 */     this.database.delete("writes", null, null);
/* 618 */     this.database.delete("trackedQueries", null, null);
/* 619 */     this.database.delete("trackedKeys", null, null);
/*     */   }
/*     */   
/*     */   public void beginTransaction() {
/* 623 */     Utilities.hardAssert(!this.database.inTransaction(), "runInTransaction called when an existing transaction is already in progress.");
/* 624 */     if (this.logger.logsDebug()) this.logger.debug("Starting transaction.");
/* 625 */     this.transactionStart = System.currentTimeMillis();
/* 626 */     this.database.beginTransaction();
/*     */   }
/*     */   
/*     */   public void endTransaction() {
/* 630 */     Utilities.hardAssert(this.database.inTransaction(), "endTransaction called when there is no existing transaction");
/* 631 */     this.database.endTransaction();
/* 632 */     long elapsed = System.currentTimeMillis() - this.transactionStart;
/* 633 */     if (this.logger.logsDebug()) this.logger.debug(String.format("Transaction completed. Elapsed: %dms", new Object[] { Long.valueOf(elapsed) }));
/*     */   }
/*     */   
/*     */   public void setTransactionSuccessful() {
/* 637 */     this.database.setTransactionSuccessful();
/*     */   }
/*     */   
/*     */   private void verifyInsideTransaction() {
/* 641 */     Utilities.hardAssert(this.database.inTransaction(), "Transaction expected to already be in progress.");
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private int saveNested(Path path, Node node)
/*     */   {
/* 653 */     long estimatedSize = NodeSizeEstimator.estimateSerializedNodeSize(node);
/* 654 */     if (((node instanceof ChildrenNode)) && (estimatedSize > 16384L)) {
/* 655 */       if (this.logger.logsDebug()) { this.logger.debug(String.format("Node estimated serialized size at path %s of %d bytes exceeds limit of %d bytes. Splitting up.", new Object[] { path, Long.valueOf(estimatedSize), Integer.valueOf(16384) }));
/*     */       }
/* 657 */       int sum = 0;
/* 658 */       for (NamedNode child : node) {
/* 659 */         sum += saveNested(path.child(child.getName()), child.getNode());
/*     */       }
/* 661 */       if (!node.getPriority().isEmpty()) {
/* 662 */         saveNode(path.child(ChildKey.getPriorityKey()), node.getPriority());
/* 663 */         sum++;
/*     */       }
/*     */       
/*     */ 
/* 667 */       saveNode(path, EmptyNode.Empty());
/* 668 */       sum++;
/*     */       
/* 670 */       return sum;
/*     */     }
/* 672 */     saveNode(path, node);
/* 673 */     return 1;
/*     */   }
/*     */   
/*     */   private String partKey(Path path, int i)
/*     */   {
/* 678 */     return pathToKey(path) + String.format(".part-%04d", new Object[] { Integer.valueOf(i) });
/*     */   }
/*     */   
/*     */   private void saveNode(Path path, Node node) {
/* 682 */     byte[] serialized = serializeObject(node.getValue(true));
/* 683 */     if (serialized.length >= 262144) {
/* 684 */       List<byte[]> parts = splitBytes(serialized, 262144);
/* 685 */       if (this.logger.logsDebug()) this.logger.debug("Saving huge leaf node with " + parts.size() + " parts.");
/* 686 */       for (int i = 0; i < parts.size(); i++) {
/* 687 */         ContentValues values = new ContentValues();
/* 688 */         values.put("path", partKey(path, i));
/* 689 */         values.put("value", (byte[])parts.get(i));
/* 690 */         this.database.insertWithOnConflict("serverCache", null, values, 5);
/*     */       }
/*     */     } else {
/* 693 */       ContentValues values = new ContentValues();
/* 694 */       values.put("path", pathToKey(path));
/* 695 */       values.put("value", serialized);
/* 696 */       this.database.insertWithOnConflict("serverCache", null, values, 5);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private Node loadNested(Path path)
/*     */   {
/* 707 */     List<String> pathStrings = new ArrayList();
/* 708 */     List<byte[]> payloads = new ArrayList();
/*     */     
/* 710 */     long queryStart = System.currentTimeMillis();
/* 711 */     Cursor cursor = loadNestedQuery(path, new String[] { "path", "value" });
/* 712 */     long queryDuration = System.currentTimeMillis() - queryStart;
/* 713 */     long loadingStart = System.currentTimeMillis();
/*     */     try {
/* 715 */       while (cursor.moveToNext()) {
/* 716 */         pathStrings.add(cursor.getString(0));
/* 717 */         payloads.add(cursor.getBlob(1));
/*     */       }
/*     */     } finally {
/* 720 */       cursor.close();
/*     */     }
/* 722 */     long loadingDuration = System.currentTimeMillis() - loadingStart;
/* 723 */     long serializingStart = System.currentTimeMillis();
/*     */     
/* 725 */     Node node = EmptyNode.Empty();
/* 726 */     boolean sawDescendant = false;
/* 727 */     Map<Path, Node> priorities = new HashMap();
/* 728 */     for (int i = 0; i < payloads.size(); i++) {
/*     */       Node savedNode;
/*     */       Path savedPath;
/* 731 */       if (((String)pathStrings.get(i)).endsWith(".part-0000"))
/*     */       {
/*     */ 
/* 734 */         String pathString = (String)pathStrings.get(i);
/* 735 */         Path savedPath = new Path(pathString.substring(0, pathString.length() - ".part-0000".length()));
/* 736 */         int splitNodeRunLength = splitNodeRunLength(savedPath, pathStrings, i);
/* 737 */         if (this.logger.logsDebug()) this.logger.debug("Loading split node with " + splitNodeRunLength + " parts.");
/* 738 */         Node savedNode = deserializeNode(joinBytes(payloads.subList(i, i + splitNodeRunLength)));
/*     */         
/* 740 */         i = i + splitNodeRunLength - 1;
/*     */       } else {
/* 742 */         savedNode = deserializeNode((byte[])payloads.get(i));
/* 743 */         savedPath = new Path((String)pathStrings.get(i));
/*     */       }
/* 745 */       if ((savedPath.getBack() != null) && (savedPath.getBack().isPriorityChildName()))
/*     */       {
/* 747 */         priorities.put(savedPath, savedNode);
/* 748 */       } else if (savedPath.contains(path)) {
/* 749 */         Utilities.hardAssert(!sawDescendant, "Descendants of path must come after ancestors.");
/* 750 */         node = savedNode.getChild(Path.getRelative(savedPath, path));
/* 751 */       } else if (path.contains(savedPath)) {
/* 752 */         sawDescendant = true;
/* 753 */         Path childPath = Path.getRelative(path, savedPath);
/* 754 */         node = node.updateChild(childPath, savedNode);
/*     */       } else {
/* 756 */         throw new IllegalStateException(String.format("Loading an unrelated row with path %s for %s", new Object[] { savedPath, path }));
/*     */       }
/*     */     }
/*     */     
/*     */ 
/* 761 */     for (Map.Entry<Path, Node> entry : priorities.entrySet()) {
/* 762 */       Path priorityPath = (Path)entry.getKey();
/* 763 */       node = node.updateChild(Path.getRelative(path, priorityPath), (Node)entry.getValue());
/*     */     }
/*     */     
/* 766 */     long serializeDuration = System.currentTimeMillis() - serializingStart;
/* 767 */     long duration = System.currentTimeMillis() - queryStart;
/* 768 */     if (this.logger.logsDebug()) {
/* 769 */       this.logger.debug(String.format("Loaded a total of %d rows for a total of %d nodes at %s in %dms (Query: %dms, Loading: %dms, Serializing: %dms)", new Object[] { Integer.valueOf(payloads.size()), Integer.valueOf(NodeSizeEstimator.nodeCount(node)), path, Long.valueOf(duration), Long.valueOf(queryDuration), Long.valueOf(loadingDuration), Long.valueOf(serializeDuration) }));
/*     */     }
/*     */     
/*     */ 
/* 773 */     return node;
/*     */   }
/*     */   
/*     */   private int splitNodeRunLength(Path path, List<String> pathStrings, int startPosition) {
/* 777 */     int endPosition = startPosition + 1;
/* 778 */     String pathPrefix = pathToKey(path);
/* 779 */     if (!((String)pathStrings.get(startPosition)).startsWith(pathPrefix)) {
/* 780 */       throw new IllegalStateException("Extracting split nodes needs to start with path prefix");
/*     */     }
/* 782 */     while ((endPosition < pathStrings.size()) && (((String)pathStrings.get(endPosition)).equals(partKey(path, endPosition - startPosition))))
/*     */     {
/* 784 */       endPosition++;
/*     */     }
/* 786 */     if ((endPosition < pathStrings.size()) && (((String)pathStrings.get(endPosition)).startsWith(pathPrefix + ".part-")))
/*     */     {
/* 788 */       throw new IllegalStateException("Run did not finish with all parts");
/*     */     }
/* 790 */     return endPosition - startPosition;
/*     */   }
/*     */   
/*     */   private Cursor loadNestedQuery(Path path, String[] columns) {
/* 794 */     String pathPrefixStart = pathToKey(path);
/* 795 */     String pathPrefixEnd = pathPrefixStartToPrefixEnd(pathPrefixStart);
/*     */     
/* 797 */     String[] arguments = new String[path.size() + 3];
/* 798 */     String whereClause = buildAncestorWhereClause(path, arguments);
/* 799 */     whereClause = whereClause + " OR (path > ? AND path < ?)";
/* 800 */     arguments[(path.size() + 1)] = pathPrefixStart;
/* 801 */     arguments[(path.size() + 2)] = pathPrefixEnd;
/* 802 */     String orderBy = "path";
/*     */     
/* 804 */     return this.database.query("serverCache", columns, whereClause, arguments, null, null, orderBy);
/*     */   }
/*     */   
/*     */   private static String pathToKey(Path path) {
/* 808 */     if (path.isEmpty()) {
/* 809 */       return "/";
/*     */     }
/* 811 */     return path.toString() + "/";
/*     */   }
/*     */   
/*     */   private static String pathPrefixStartToPrefixEnd(String prefix)
/*     */   {
/* 816 */     assert (prefix.endsWith("/")) : "Path keys must end with a '/'";
/* 817 */     return prefix.substring(0, prefix.length() - 1) + '0';
/*     */   }
/*     */   
/*     */   private static String buildAncestorWhereClause(Path path, String[] arguments) {
/* 821 */     assert (arguments.length >= path.size() + 1);
/* 822 */     int count = 0;
/* 823 */     StringBuilder whereClause = new StringBuilder("(");
/* 824 */     while (!path.isEmpty()) {
/* 825 */       whereClause.append("path");
/* 826 */       whereClause.append(" = ? OR ");
/* 827 */       arguments[count] = pathToKey(path);
/* 828 */       path = path.getParent();
/* 829 */       count++;
/*     */     }
/* 831 */     whereClause.append("path");
/* 832 */     whereClause.append(" = ?)");
/* 833 */     arguments[count] = pathToKey(Path.getEmptyPath());
/* 834 */     return whereClause.toString();
/*     */   }
/*     */   
/*     */   private int removeNested(String table, Path path) {
/* 838 */     String pathPrefixQuery = "path >= ? AND path < ?";
/* 839 */     String pathPrefixStart = pathToKey(path);
/* 840 */     String pathPrefixEnd = pathPrefixStartToPrefixEnd(pathPrefixStart);
/* 841 */     return this.database.delete(table, pathPrefixQuery, new String[] { pathPrefixStart, pathPrefixEnd });
/*     */   }
/*     */   
/*     */   private static List<byte[]> splitBytes(byte[] bytes, int size) {
/* 845 */     int parts = (bytes.length - 1) / size + 1;
/* 846 */     List<byte[]> partList = new ArrayList(parts);
/* 847 */     for (int i = 0; i < parts; i++) {
/* 848 */       int length = Math.min(size, bytes.length - i * size);
/* 849 */       byte[] part = new byte[length];
/* 850 */       System.arraycopy(bytes, i * size, part, 0, length);
/* 851 */       partList.add(part);
/*     */     }
/* 853 */     return partList;
/*     */   }
/*     */   
/*     */   private byte[] joinBytes(List<byte[]> payloads) {
/* 857 */     int totalSize = 0;
/* 858 */     for (byte[] payload : payloads) {
/* 859 */       totalSize += payload.length;
/*     */     }
/* 861 */     byte[] buffer = new byte[totalSize];
/* 862 */     int currentBytePosition = 0;
/* 863 */     for (byte[] payload : payloads) {
/* 864 */       System.arraycopy(payload, 0, buffer, currentBytePosition, payload.length);
/* 865 */       currentBytePosition += payload.length;
/*     */     }
/* 867 */     return buffer;
/*     */   }
/*     */   
/*     */   private byte[] serializeObject(Object object) {
/*     */     try {
/* 872 */       return this.jsonMapper.writeValueAsBytes(object);
/*     */     } catch (IOException e) {
/* 874 */       throw new RuntimeException("Could not serialize leaf node", e);
/*     */     }
/*     */   }
/*     */   
/*     */   private Node deserializeNode(byte[] value) {
/*     */     try {
/* 880 */       Object o = this.jsonMapper.readValue(value, Object.class);
/* 881 */       return NodeUtilities.NodeFromJSON(o);
/*     */     } catch (IOException e) {
/*     */       try {
/* 884 */         String stringValue = new String(value, "UTF-8");
/* 885 */         throw new RuntimeException("Could not deserialize node: " + stringValue, e);
/*     */       } catch (UnsupportedEncodingException e1) {
/* 887 */         throw new RuntimeException("Failed to serialize values to utf-8: " + Arrays.toString(value), e);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private String commaSeparatedList(Collection<Long> items) {
/* 893 */     StringBuilder list = new StringBuilder();
/* 894 */     boolean first = true;
/* 895 */     for (Iterator i$ = items.iterator(); i$.hasNext();) { long item = ((Long)i$.next()).longValue();
/* 896 */       if (!first)
/* 897 */         list.append(",");
/* 898 */       first = false;
/* 899 */       list.append(item);
/*     */     }
/* 901 */     return list.toString();
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/android/SqlPersistenceStorageEngine.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */