/*     */ package com.firebase.client.core.view;
/*     */ 
/*     */ import com.firebase.client.core.EventRegistration;
/*     */ import com.firebase.client.core.Path;
/*     */ import com.firebase.client.core.operation.Operation;
/*     */ import com.firebase.client.core.view.filter.IndexedFilter;
/*     */ import com.firebase.client.snapshot.IndexedNode;
/*     */ import com.firebase.client.snapshot.NamedNode;
/*     */ import com.firebase.client.snapshot.Node;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ 
/*     */ public class View
/*     */ {
/*     */   private final QuerySpec query;
/*     */   private final ViewProcessor processor;
/*     */   private ViewCache viewCache;
/*     */   private final List<EventRegistration> eventRegistrations;
/*     */   private final EventGenerator eventGenerator;
/*     */   
/*     */   public View(QuerySpec query, ViewCache initialViewCache)
/*     */   {
/*  23 */     this.query = query;
/*  24 */     IndexedFilter indexFilter = new IndexedFilter(query.getIndex());
/*  25 */     com.firebase.client.core.view.filter.NodeFilter filter = query.getParams().getNodeFilter();
/*  26 */     this.processor = new ViewProcessor(filter);
/*  27 */     CacheNode initialServerCache = initialViewCache.getServerCache();
/*  28 */     CacheNode initialEventCache = initialViewCache.getEventCache();
/*     */     
/*     */ 
/*  31 */     IndexedNode emptyIndexedNode = IndexedNode.from(com.firebase.client.snapshot.EmptyNode.Empty(), query.getIndex());
/*  32 */     IndexedNode serverSnap = indexFilter.updateFullNode(emptyIndexedNode, initialServerCache.getIndexedNode(), null);
/*  33 */     IndexedNode eventSnap = filter.updateFullNode(emptyIndexedNode, initialEventCache.getIndexedNode(), null);
/*  34 */     CacheNode newServerCache = new CacheNode(serverSnap, initialServerCache.isFullyInitialized(), indexFilter.filtersNodes());
/*  35 */     CacheNode newEventCache = new CacheNode(eventSnap, initialEventCache.isFullyInitialized(), filter.filtersNodes());
/*     */     
/*  37 */     this.viewCache = new ViewCache(newEventCache, newServerCache);
/*     */     
/*  39 */     this.eventRegistrations = new ArrayList();
/*     */     
/*  41 */     this.eventGenerator = new EventGenerator(query);
/*     */   }
/*     */   
/*     */   public static class OperationResult {
/*     */     public final List<DataEvent> events;
/*     */     public final List<Change> changes;
/*     */     
/*     */     public OperationResult(List<DataEvent> events, List<Change> changes) {
/*  49 */       this.events = events;
/*  50 */       this.changes = changes;
/*     */     }
/*     */   }
/*     */   
/*     */   public QuerySpec getQuery() {
/*  55 */     return this.query;
/*     */   }
/*     */   
/*     */   public Node getCompleteNode() {
/*  59 */     return this.viewCache.getCompleteEventSnap();
/*     */   }
/*     */   
/*     */   public Node getServerCache() {
/*  63 */     return this.viewCache.getServerCache().getNode();
/*     */   }
/*     */   
/*     */   public Node getEventCache() {
/*  67 */     return this.viewCache.getEventCache().getNode();
/*     */   }
/*     */   
/*     */   public Node getCompleteServerCache(Path path) {
/*  71 */     Node cache = this.viewCache.getCompleteServerSnap();
/*  72 */     if (cache != null)
/*     */     {
/*     */ 
/*  75 */       if ((this.query.loadsAllData()) || ((!path.isEmpty()) && (!cache.getImmediateChild(path.getFront()).isEmpty())))
/*     */       {
/*  77 */         return cache.getChild(path);
/*     */       }
/*     */     }
/*  80 */     return null;
/*     */   }
/*     */   
/*     */   public boolean isEmpty() {
/*  84 */     return this.eventRegistrations.isEmpty();
/*     */   }
/*     */   
/*     */ 
/*  88 */   public void addEventRegistration(@com.firebase.client.annotations.NotNull EventRegistration registration) { this.eventRegistrations.add(registration); }
/*     */   
/*     */   public List<Event> removeEventRegistration(@com.firebase.client.annotations.Nullable EventRegistration registration, com.firebase.client.FirebaseError cancelError) { List<Event> cancelEvents;
/*     */     Path path;
/*     */     List<Event> cancelEvents;
/*  93 */     if (cancelError != null) {
/*  94 */       cancelEvents = new ArrayList();
/*  95 */       assert (registration == null) : "A cancel should cancel all event registrations";
/*  96 */       path = this.query.getPath();
/*  97 */       for (EventRegistration eventRegistration : this.eventRegistrations) {
/*  98 */         cancelEvents.add(new CancelEvent(eventRegistration, cancelError, path));
/*     */       }
/*     */     } else {
/* 101 */       cancelEvents = java.util.Collections.emptyList();
/*     */     }
/* 103 */     if (registration != null)
/*     */     {
/*     */ 
/* 106 */       int indexToDelete = -1;
/* 107 */       for (int i = 0; i < this.eventRegistrations.size(); i++) {
/* 108 */         EventRegistration candidate = (EventRegistration)this.eventRegistrations.get(i);
/* 109 */         if (candidate.isSameListener(registration)) {
/* 110 */           indexToDelete = i;
/* 111 */           if (candidate.isZombied()) {
/*     */             break;
/*     */           }
/*     */         }
/*     */       }
/* 116 */       if (indexToDelete != -1) {
/* 117 */         EventRegistration deletedRegistration = (EventRegistration)this.eventRegistrations.get(indexToDelete);
/* 118 */         this.eventRegistrations.remove(indexToDelete);
/* 119 */         deletedRegistration.zombify();
/*     */       }
/*     */     } else {
/* 122 */       for (EventRegistration eventRegistration : this.eventRegistrations) {
/* 123 */         eventRegistration.zombify();
/*     */       }
/* 125 */       this.eventRegistrations.clear();
/*     */     }
/* 127 */     return cancelEvents;
/*     */   }
/*     */   
/*     */   public OperationResult applyOperation(Operation operation, com.firebase.client.core.WriteTreeRef writesCache, Node optCompleteServerCache) {
/* 131 */     if ((operation.getType() == com.firebase.client.core.operation.Operation.OperationType.Merge) && (operation.getSource().getQueryParams() != null)) {
/* 132 */       assert (this.viewCache.getCompleteServerSnap() != null) : "We should always have a full cache before handling merges";
/* 133 */       assert (this.viewCache.getCompleteEventSnap() != null) : "Missing event cache, even though we have a server cache";
/*     */     }
/* 135 */     ViewCache oldViewCache = this.viewCache;
/* 136 */     ViewProcessor.ProcessorResult result = this.processor.applyOperation(oldViewCache, operation, writesCache, optCompleteServerCache);
/*     */     
/*     */ 
/* 139 */     assert ((result.viewCache.getServerCache().isFullyInitialized()) || (!oldViewCache.getServerCache().isFullyInitialized())) : "Once a server snap is complete, it should never go back";
/*     */     
/* 141 */     this.viewCache = result.viewCache;
/* 142 */     List<DataEvent> events = generateEventsForChanges(result.changes, result.viewCache.getEventCache().getIndexedNode(), null);
/*     */     
/* 144 */     return new OperationResult(events, result.changes);
/*     */   }
/*     */   
/*     */   public List<DataEvent> getInitialEvents(EventRegistration registration) {
/* 148 */     CacheNode eventSnap = this.viewCache.getEventCache();
/* 149 */     List<Change> initialChanges = new ArrayList();
/* 150 */     for (NamedNode child : eventSnap.getNode()) {
/* 151 */       initialChanges.add(Change.childAddedChange(child.getName(), child.getNode()));
/*     */     }
/* 153 */     if (eventSnap.isFullyInitialized()) {
/* 154 */       initialChanges.add(Change.valueChange(eventSnap.getIndexedNode()));
/*     */     }
/* 156 */     return generateEventsForChanges(initialChanges, eventSnap.getIndexedNode(), registration);
/*     */   }
/*     */   
/*     */   private List<DataEvent> generateEventsForChanges(List<Change> changes, IndexedNode eventCache, EventRegistration registration) { List<EventRegistration> registrations;
/*     */     List<EventRegistration> registrations;
/* 161 */     if (registration == null) {
/* 162 */       registrations = this.eventRegistrations;
/*     */     } else {
/* 164 */       registrations = java.util.Arrays.asList(new EventRegistration[] { registration });
/*     */     }
/* 166 */     return this.eventGenerator.generateEventsForChanges(changes, eventCache, registrations);
/*     */   }
/*     */   
/*     */   List<EventRegistration> getEventRegistrations()
/*     */   {
/* 171 */     return this.eventRegistrations;
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/view/View.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */