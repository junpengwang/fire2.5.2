/*     */ package com.firebase.client;
/*     */ 
/*     */ import com.firebase.client.core.ChildEventRegistration;
/*     */ import com.firebase.client.core.EventRegistration;
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.core.Repo;
/*     */ import com.firebase.client.core.ValueEventRegistration;
/*     */ import com.firebase.client.core.ZombieEventManager;
/*     */ import com.firebase.client.core.view.QueryParams;
/*     */ import com.firebase.client.core.view.QuerySpec;
/*     */ import com.firebase.client.snapshot.BooleanNode;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.DoubleNode;
/*     */ import com.firebase.client.snapshot.EmptyNode;
/*     */ import com.firebase.client.snapshot.Index;
/*     */ import com.firebase.client.snapshot.KeyIndex;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import com.firebase.client.snapshot.PathIndex;
/*     */ import com.firebase.client.snapshot.PriorityIndex;
/*     */ import com.firebase.client.snapshot.PriorityUtilities;
/*     */ import com.firebase.client.snapshot.StringNode;
/*     */ import com.firebase.client.utilities.Validation;
/*     */ 
/*     */ public class Query
/*     */ {
/*     */   protected final Repo repo;
/*     */   protected final Path path;
/*     */   protected final QueryParams params;
/*     */   private final boolean orderByCalled;
/*     */   
/*     */   Query(Repo repo, Path path, QueryParams params, boolean orderByCalled) throws FirebaseException
/*     */   {
/*  33 */     this.repo = repo;
/*  34 */     this.path = path;
/*  35 */     this.params = params;
/*  36 */     this.orderByCalled = orderByCalled;
/*  37 */     com.firebase.client.utilities.Utilities.hardAssert(params.isValid(), "Validation of queries failed.");
/*     */   }
/*     */   
/*     */   Query(Repo repo, Path path) {
/*  41 */     this.repo = repo;
/*  42 */     this.path = path;
/*  43 */     this.params = QueryParams.DEFAULT_PARAMS;
/*  44 */     this.orderByCalled = false;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private void validateQueryEndpoints(QueryParams params)
/*     */   {
/*  51 */     if (params.getIndex().equals(KeyIndex.getInstance())) {
/*  52 */       String message = "You must use startAt(String value), endAt(String value) or equalTo(String value) in combination with orderByKey(). Other type of values or using the version with 2 parameters is not supported";
/*     */       
/*     */ 
/*  55 */       if (params.hasStart()) {
/*  56 */         Node startNode = params.getIndexStartValue();
/*  57 */         ChildKey startName = params.getIndexStartName();
/*  58 */         if ((startName != ChildKey.getMinName()) || (!(startNode instanceof StringNode))) {
/*  59 */           throw new IllegalArgumentException(message);
/*     */         }
/*     */       }
/*  62 */       if (params.hasEnd()) {
/*  63 */         Node endNode = params.getIndexEndValue();
/*  64 */         ChildKey endName = params.getIndexEndName();
/*  65 */         if ((endName != ChildKey.getMaxName()) || (!(endNode instanceof StringNode))) {
/*  66 */           throw new IllegalArgumentException(message);
/*     */         }
/*     */       }
/*  69 */     } else if ((params.getIndex().equals(PriorityIndex.getInstance())) && (
/*  70 */       ((params.hasStart()) && (!PriorityUtilities.isValidPriority(params.getIndexStartValue()))) || ((params.hasEnd()) && (!PriorityUtilities.isValidPriority(params.getIndexEndValue())))))
/*     */     {
/*  72 */       throw new IllegalArgumentException("When using orderByPriority(), values provided to startAt(), endAt(), or equalTo() must be valid priorities.");
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void validateLimit(QueryParams params)
/*     */   {
/*  82 */     if ((params.hasStart()) && (params.hasEnd()) && (params.hasLimit()) && (!params.hasAnchoredLimit())) {
/*  83 */       throw new IllegalArgumentException("Can't combine startAt(), endAt() and limit(). Use limitToFirst() or limitToLast() instead");
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private void validateEqualToCall()
/*     */   {
/*  92 */     if (this.params.hasStart()) {
/*  93 */       throw new IllegalArgumentException("Can't call equalTo() and startAt() combined");
/*     */     }
/*  95 */     if (this.params.hasEnd()) {
/*  96 */       throw new IllegalArgumentException("Can't call equalTo() and endAt() combined");
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private void validateNoOrderByCall()
/*     */   {
/* 104 */     if (this.orderByCalled) {
/* 105 */       throw new IllegalArgumentException("You can't combine multiple orderBy calls!");
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public ValueEventListener addValueEventListener(ValueEventListener listener)
/*     */   {
/* 118 */     addEventRegistration(new ValueEventRegistration(this.repo, listener, getSpec()));
/* 119 */     return listener;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public ChildEventListener addChildEventListener(ChildEventListener listener)
/*     */   {
/* 129 */     addEventRegistration(new ChildEventRegistration(this.repo, listener, getSpec()));
/* 130 */     return listener;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void addListenerForSingleValueEvent(final ValueEventListener listener)
/*     */   {
/* 139 */     addEventRegistration(new ValueEventRegistration(this.repo, new ValueEventListener()
/*     */     {
/*     */       public void onDataChange(DataSnapshot snapshot)
/*     */       {
/* 143 */         Query.this.removeEventListener(this);
/* 144 */         listener.onDataChange(snapshot);
/*     */       }
/*     */       
/*     */ 
/*     */ 
/* 149 */       public void onCancelled(FirebaseError error) { listener.onCancelled(error); } }, getSpec()));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void removeEventListener(ValueEventListener listener)
/*     */   {
/* 159 */     if (listener == null) {
/* 160 */       throw new NullPointerException("listener must not be null");
/*     */     }
/* 162 */     removeEventRegistration(new ValueEventRegistration(this.repo, listener, getSpec()));
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void removeEventListener(ChildEventListener listener)
/*     */   {
/* 170 */     if (listener == null) {
/* 171 */       throw new NullPointerException("listener must not be null");
/*     */     }
/* 173 */     removeEventRegistration(new ChildEventRegistration(this.repo, listener, getSpec()));
/*     */   }
/*     */   
/*     */   private void removeEventRegistration(final EventRegistration registration)
/*     */   {
/* 178 */     ZombieEventManager.getInstance().zombifyForRemove(registration);
/* 179 */     this.repo.scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 182 */         Query.this.repo.removeEventCallback(registration);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */   private void addEventRegistration(final EventRegistration listener) {
/* 188 */     ZombieEventManager.getInstance().recordEventRegistration(listener);
/* 189 */     this.repo.scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 192 */         Query.this.repo.addEventCallback(listener);
/*     */       }
/*     */     });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void keepSynced(final boolean keepSynced)
/*     */   {
/* 206 */     if ((!this.path.isEmpty()) && (this.path.getFront().equals(ChildKey.getInfoKey()))) {
/* 207 */       throw new FirebaseException("Can't call keepSynced() on .info paths.");
/*     */     }
/*     */     
/* 210 */     this.repo.scheduleNow(new Runnable()
/*     */     {
/*     */       public void run() {
/* 213 */         Query.this.repo.keepSynced(Query.this.getSpec(), keepSynced);
/*     */       }
/*     */     });
/*     */   }
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
/*     */   public Query startAt()
/*     */   {
/* 237 */     return startAt(EmptyNode.Empty(), null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query startAt(String value)
/*     */   {
/* 247 */     return startAt(value, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query startAt(double value)
/*     */   {
/* 257 */     return startAt(value, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query startAt(boolean value)
/*     */   {
/* 268 */     return startAt(value, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query startAt(String value, String key)
/*     */   {
/* 280 */     Node node = value != null ? new StringNode(value, PriorityUtilities.NullPriority()) : EmptyNode.Empty();
/* 281 */     return startAt(node, key);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query startAt(double value, String key)
/*     */   {
/* 293 */     return startAt(new DoubleNode(Double.valueOf(value), PriorityUtilities.NullPriority()), key);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query startAt(boolean value, String key)
/*     */   {
/* 306 */     return startAt(new BooleanNode(Boolean.valueOf(value), PriorityUtilities.NullPriority()), key);
/*     */   }
/*     */   
/*     */   private Query startAt(Node node, String key) {
/* 310 */     Validation.validateNullableKey(key);
/* 311 */     if ((!node.isLeafNode()) && (!node.isEmpty())) {
/* 312 */       throw new IllegalArgumentException("Can only use simple values for startAt()");
/*     */     }
/* 314 */     if (this.params.hasStart()) {
/* 315 */       throw new IllegalArgumentException("Can't call startAt() or equalTo() multiple times");
/*     */     }
/* 317 */     ChildKey childKey = key != null ? ChildKey.fromString(key) : null;
/* 318 */     QueryParams newParams = this.params.startAt(node, childKey);
/* 319 */     validateLimit(newParams);
/* 320 */     validateQueryEndpoints(newParams);
/* 321 */     assert (newParams.isValid());
/* 322 */     return new Query(this.repo, this.path, newParams, this.orderByCalled);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query endAt()
/*     */   {
/* 331 */     return endAt(Node.MAX_NODE, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query endAt(String value)
/*     */   {
/* 341 */     return endAt(value, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query endAt(double value)
/*     */   {
/* 351 */     return endAt(value, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query endAt(boolean value)
/*     */   {
/* 362 */     return endAt(value, null);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query endAt(String value, String key)
/*     */   {
/* 374 */     Node node = value != null ? new StringNode(value, PriorityUtilities.NullPriority()) : EmptyNode.Empty();
/* 375 */     return endAt(node, key);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query endAt(double value, String key)
/*     */   {
/* 387 */     return endAt(new DoubleNode(Double.valueOf(value), PriorityUtilities.NullPriority()), key);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query endAt(boolean value, String key)
/*     */   {
/* 400 */     return endAt(new BooleanNode(Boolean.valueOf(value), PriorityUtilities.NullPriority()), key);
/*     */   }
/*     */   
/*     */   private Query endAt(Node node, String key) {
/* 404 */     Validation.validateNullableKey(key);
/* 405 */     if ((!node.isLeafNode()) && (!node.isEmpty())) {
/* 406 */       throw new IllegalArgumentException("Can only use simple values for endAt()");
/*     */     }
/* 408 */     ChildKey childKey = key != null ? ChildKey.fromString(key) : null;
/* 409 */     if (this.params.hasEnd()) {
/* 410 */       throw new IllegalArgumentException("Can't call endAt() or equalTo() multiple times");
/*     */     }
/* 412 */     QueryParams newParams = this.params.endAt(node, childKey);
/* 413 */     validateLimit(newParams);
/* 414 */     validateQueryEndpoints(newParams);
/* 415 */     assert (newParams.isValid());
/* 416 */     return new Query(this.repo, this.path, newParams, this.orderByCalled);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query equalTo(String value)
/*     */   {
/* 425 */     validateEqualToCall();
/* 426 */     return startAt(value).endAt(value);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query equalTo(double value)
/*     */   {
/* 435 */     validateEqualToCall();
/* 436 */     return startAt(value).endAt(value);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query equalTo(boolean value)
/*     */   {
/* 446 */     validateEqualToCall();
/* 447 */     return startAt(value).endAt(value);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query equalTo(String value, String key)
/*     */   {
/* 458 */     validateEqualToCall();
/* 459 */     return startAt(value, key).endAt(value, key);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query equalTo(double value, String key)
/*     */   {
/* 470 */     validateEqualToCall();
/* 471 */     return startAt(value, key).endAt(value, key);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query equalTo(boolean value, String key)
/*     */   {
/* 482 */     validateEqualToCall();
/* 483 */     return startAt(value, key).endAt(value, key);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   @Deprecated
/*     */   public Query limit(int limit)
/*     */   {
/* 494 */     if (limit <= 0) {
/* 495 */       throw new IllegalArgumentException("Limit must be a positive integer!");
/*     */     }
/* 497 */     if (this.params.hasLimit()) {
/* 498 */       throw new IllegalArgumentException("Can't call limitToLast on query with previously set limit!");
/*     */     }
/* 500 */     QueryParams newParams = this.params.limit(limit);
/* 501 */     validateLimit(newParams);
/* 502 */     return new Query(this.repo, this.path, newParams, this.orderByCalled);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query limitToFirst(int limit)
/*     */   {
/* 512 */     if (limit <= 0) {
/* 513 */       throw new IllegalArgumentException("Limit must be a positive integer!");
/*     */     }
/* 515 */     if (this.params.hasLimit()) {
/* 516 */       throw new IllegalArgumentException("Can't call limitToLast on query with previously set limit!");
/*     */     }
/* 518 */     return new Query(this.repo, this.path, this.params.limitToFirst(limit), this.orderByCalled);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query limitToLast(int limit)
/*     */   {
/* 528 */     if (limit <= 0) {
/* 529 */       throw new IllegalArgumentException("Limit must be a positive integer!");
/*     */     }
/* 531 */     if (this.params.hasLimit()) {
/* 532 */       throw new IllegalArgumentException("Can't call limitToLast on query with previously set limit!");
/*     */     }
/* 534 */     return new Query(this.repo, this.path, this.params.limitToLast(limit), this.orderByCalled);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query orderByChild(String path)
/*     */   {
/* 544 */     if (path == null) {
/* 545 */       throw new NullPointerException("Key can't be null");
/*     */     }
/* 547 */     if ((path.equals("$key")) || (path.equals(".key"))) {
/* 548 */       throw new IllegalArgumentException("Can't use '" + path + "' as path, please use orderByKey() instead!");
/*     */     }
/* 550 */     if ((path.equals("$priority")) || (path.equals(".priority"))) {
/* 551 */       throw new IllegalArgumentException("Can't use '" + path + "' as path, please use orderByPriority() instead!");
/*     */     }
/* 553 */     if ((path.equals("$value")) || (path.equals(".value"))) {
/* 554 */       throw new IllegalArgumentException("Can't use '" + path + "' as path, please use orderByValue() instead!");
/*     */     }
/* 556 */     Validation.validatePathString(path);
/* 557 */     validateNoOrderByCall();
/* 558 */     Path indexPath = new Path(path);
/* 559 */     if (indexPath.size() == 0) {
/* 560 */       throw new IllegalArgumentException("Can't use empty path, use orderByValue() instead!");
/*     */     }
/* 562 */     Index index = new PathIndex(indexPath);
/* 563 */     return new Query(this.repo, this.path, this.params.orderBy(index), true);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query orderByPriority()
/*     */   {
/* 572 */     validateNoOrderByCall();
/* 573 */     QueryParams newParams = this.params.orderBy(PriorityIndex.getInstance());
/* 574 */     validateQueryEndpoints(newParams);
/* 575 */     return new Query(this.repo, this.path, newParams, true);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query orderByKey()
/*     */   {
/* 584 */     validateNoOrderByCall();
/* 585 */     QueryParams newParams = this.params.orderBy(KeyIndex.getInstance());
/* 586 */     validateQueryEndpoints(newParams);
/* 587 */     return new Query(this.repo, this.path, newParams, true);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Query orderByValue()
/*     */   {
/* 596 */     validateNoOrderByCall();
/* 597 */     return new Query(this.repo, this.path, this.params.orderBy(com.firebase.client.snapshot.ValueIndex.getInstance()), true);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public Firebase getRef()
/*     */   {
/* 604 */     return new Firebase(this.repo, getPath());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public Path getPath()
/*     */   {
/* 614 */     return this.path;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public Repo getRepo()
/*     */   {
/* 622 */     return this.repo;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public QuerySpec getSpec()
/*     */   {
/* 630 */     return new QuerySpec(this.path, this.params);
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/Query.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */