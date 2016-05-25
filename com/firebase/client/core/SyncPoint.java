/*     */ package com.firebase.client.core;
/*     */ 
/*     */ import com.firebase.client.FirebaseError;
/*     */ import com.firebase.client.annotations.NotNull;
/*     */ import com.firebase.client.annotations.Nullable;
/*     */ import com.firebase.client.core.operation.Operation;
/*     */ import com.firebase.client.core.operation.OperationSource;
/*     */ import com.firebase.client.core.persistence.PersistenceManager;
/*     */ import com.firebase.client.core.view.CacheNode;
/*     */ import com.firebase.client.core.view.Change;
/*     */ import com.firebase.client.core.view.DataEvent;
/*     */ import com.firebase.client.core.view.Event;
/*     */ import com.firebase.client.core.view.Event.EventType;
/*     */ import com.firebase.client.core.view.QueryParams;
/*     */ import com.firebase.client.core.view.QuerySpec;
/*     */ import com.firebase.client.core.view.View;
/*     */ import com.firebase.client.core.view.View.OperationResult;
/*     */ import com.firebase.client.core.view.ViewCache;
/*     */ import com.firebase.client.snapshot.ChildKey;
/*     */ import com.firebase.client.snapshot.IndexedNode;
/*     */ import com.firebase.client.snapshot.NamedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import com.firebase.client.utilities.Pair;
/*     */ import java.util.ArrayList;
/*     */ import java.util.HashMap;
/*     */ import java.util.HashSet;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Map.Entry;
/*     */ import java.util.Set;
/*     */ 
/*     */ 
/*     */ public class SyncPoint
/*     */ {
/*     */   private final Map<QueryParams, View> views;
/*     */   private final PersistenceManager persistenceManager;
/*     */   
/*     */   public SyncPoint(PersistenceManager persistenceManager)
/*     */   {
/*  41 */     this.views = new HashMap();
/*  42 */     this.persistenceManager = persistenceManager;
/*     */   }
/*     */   
/*     */   public boolean isEmpty() {
/*  46 */     return this.views.isEmpty();
/*     */   }
/*     */   
/*     */   private List<DataEvent> applyOperationToView(View view, Operation operation, WriteTreeRef writes, Node optCompleteServerCache) {
/*  50 */     View.OperationResult result = view.applyOperation(operation, writes, optCompleteServerCache);
/*     */     
/*  52 */     if (!view.getQuery().loadsAllData()) {
/*  53 */       Set<ChildKey> removed = new HashSet();
/*  54 */       Set<ChildKey> added = new HashSet();
/*  55 */       for (Change change : result.changes) {
/*  56 */         Event.EventType type = change.getEventType();
/*  57 */         if (type == Event.EventType.CHILD_ADDED) {
/*  58 */           added.add(change.getChildKey());
/*  59 */         } else if (type == Event.EventType.CHILD_REMOVED) {
/*  60 */           removed.add(change.getChildKey());
/*     */         }
/*     */       }
/*  63 */       if ((!added.isEmpty()) || (!removed.isEmpty())) {
/*  64 */         this.persistenceManager.updateTrackedQueryKeys(view.getQuery(), added, removed);
/*     */       }
/*     */     }
/*  67 */     return result.events;
/*     */   }
/*     */   
/*     */   public List<DataEvent> applyOperation(Operation operation, WriteTreeRef writesCache, Node optCompleteServerCache) {
/*  71 */     QueryParams queryParams = operation.getSource().getQueryParams();
/*  72 */     if (queryParams != null) {
/*  73 */       View view = (View)this.views.get(queryParams);
/*  74 */       assert (view != null);
/*  75 */       return applyOperationToView(view, operation, writesCache, optCompleteServerCache);
/*     */     }
/*  77 */     List<DataEvent> events = new ArrayList();
/*  78 */     for (Map.Entry<QueryParams, View> entry : this.views.entrySet()) {
/*  79 */       View view = (View)entry.getValue();
/*  80 */       events.addAll(applyOperationToView(view, operation, writesCache, optCompleteServerCache));
/*     */     }
/*  82 */     return events;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public List<DataEvent> addEventRegistration(@NotNull EventRegistration eventRegistration, WriteTreeRef writesCache, CacheNode serverCache)
/*     */   {
/*  91 */     QuerySpec query = eventRegistration.getQuerySpec();
/*  92 */     View view = (View)this.views.get(query.getParams());
/*  93 */     if (view == null)
/*     */     {
/*  95 */       Node eventCache = writesCache.calcCompleteEventCache(serverCache.isFullyInitialized() ? serverCache.getNode() : null);
/*     */       boolean eventCacheComplete;
/*  97 */       boolean eventCacheComplete; if (eventCache != null) {
/*  98 */         eventCacheComplete = true;
/*     */       } else {
/* 100 */         eventCache = writesCache.calcCompleteEventChildren(serverCache.getNode());
/* 101 */         eventCacheComplete = false;
/*     */       }
/* 103 */       IndexedNode indexed = IndexedNode.from(eventCache, query.getIndex());
/* 104 */       ViewCache viewCache = new ViewCache(new CacheNode(indexed, eventCacheComplete, false), serverCache);
/* 105 */       view = new View(query, viewCache);
/*     */       
/* 107 */       if (!query.loadsAllData()) {
/* 108 */         Set<ChildKey> allChildren = new HashSet();
/* 109 */         for (NamedNode node : view.getEventCache()) {
/* 110 */           allChildren.add(node.getName());
/*     */         }
/* 112 */         this.persistenceManager.setTrackedQueryKeys(query, allChildren);
/*     */       }
/* 114 */       this.views.put(query.getParams(), view);
/*     */     }
/*     */     
/*     */ 
/* 118 */     view.addEventRegistration(eventRegistration);
/* 119 */     return view.getInitialEvents(eventRegistration);
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
/*     */   public Pair<List<QuerySpec>, List<Event>> removeEventRegistration(@NotNull QuerySpec query, @Nullable EventRegistration eventRegistration, @Nullable FirebaseError cancelError)
/*     */   {
/* 136 */     List<QuerySpec> removed = new ArrayList();
/* 137 */     List<Event> cancelEvents = new ArrayList();
/* 138 */     boolean hadCompleteView = hasCompleteView();
/* 139 */     if (query.isDefault())
/*     */     {
/* 141 */       Iterator<Map.Entry<QueryParams, View>> iterator = this.views.entrySet().iterator();
/* 142 */       while (iterator.hasNext()) {
/* 143 */         Map.Entry<QueryParams, View> entry = (Map.Entry)iterator.next();
/* 144 */         View view = (View)entry.getValue();
/* 145 */         cancelEvents.addAll(view.removeEventRegistration(eventRegistration, cancelError));
/* 146 */         if (view.isEmpty()) {
/* 147 */           iterator.remove();
/*     */           
/*     */ 
/* 150 */           if (!view.getQuery().loadsAllData()) {
/* 151 */             removed.add(view.getQuery());
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */     else {
/* 157 */       View view = (View)this.views.get(query.getParams());
/* 158 */       if (view != null) {
/* 159 */         cancelEvents.addAll(view.removeEventRegistration(eventRegistration, cancelError));
/* 160 */         if (view.isEmpty()) {
/* 161 */           this.views.remove(query.getParams());
/*     */           
/*     */ 
/* 164 */           if (!view.getQuery().loadsAllData()) {
/* 165 */             removed.add(view.getQuery());
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */     
/* 171 */     if ((hadCompleteView) && (!hasCompleteView()))
/*     */     {
/* 173 */       removed.add(QuerySpec.defaultQueryAtPath(query.getPath()));
/*     */     }
/* 175 */     return new Pair(removed, cancelEvents);
/*     */   }
/*     */   
/*     */   public List<View> getQueryViews() {
/* 179 */     List<View> views = new ArrayList();
/* 180 */     for (Map.Entry<QueryParams, View> entry : this.views.entrySet()) {
/* 181 */       View view = (View)entry.getValue();
/* 182 */       if (!view.getQuery().loadsAllData()) {
/* 183 */         views.add(view);
/*     */       }
/*     */     }
/* 186 */     return views;
/*     */   }
/*     */   
/*     */   public Node getCompleteServerCache(Path path) {
/* 190 */     for (View view : this.views.values()) {
/* 191 */       if (view.getCompleteServerCache(path) != null) {
/* 192 */         return view.getCompleteServerCache(path);
/*     */       }
/*     */     }
/* 195 */     return null;
/*     */   }
/*     */   
/*     */   public View viewForQuery(QuerySpec query)
/*     */   {
/* 200 */     if (query.loadsAllData()) {
/* 201 */       return getCompleteView();
/*     */     }
/* 203 */     return (View)this.views.get(query.getParams());
/*     */   }
/*     */   
/*     */   public boolean viewExistsForQuery(QuerySpec query)
/*     */   {
/* 208 */     return viewForQuery(query) != null;
/*     */   }
/*     */   
/*     */   public boolean hasCompleteView() {
/* 212 */     return getCompleteView() != null;
/*     */   }
/*     */   
/*     */   public View getCompleteView() {
/* 216 */     for (Map.Entry<QueryParams, View> entry : this.views.entrySet()) {
/* 217 */       View view = (View)entry.getValue();
/* 218 */       if (view.getQuery().loadsAllData()) {
/* 219 */         return view;
/*     */       }
/*     */     }
/* 222 */     return null;
/*     */   }
/*     */   
/*     */   Map<QueryParams, View> getViews()
/*     */   {
/* 227 */     return this.views;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/SyncPoint.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */