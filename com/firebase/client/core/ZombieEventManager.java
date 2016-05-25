/*     */ package com.firebase.client.core;
/*     */ 
/*     */ import com.firebase.client.annotations.NotNull;
/*     */ import com.firebase.client.core.view.QuerySpec;
/*     */ import java.util.ArrayList;
/*     */ import java.util.HashMap;
/*     */ import java.util.HashSet;
/*     */ import java.util.List;
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
/*     */ public class ZombieEventManager
/*     */   implements EventRegistrationZombieListener
/*     */ {
/*  23 */   final HashMap<EventRegistration, List<EventRegistration>> globalEventRegistrations = new HashMap();
/*     */   
/*     */ 
/*  26 */   private static ZombieEventManager defaultInstance = new ZombieEventManager();
/*     */   
/*     */ 
/*     */ 
/*     */   @NotNull
/*     */   public static ZombieEventManager getInstance()
/*     */   {
/*  33 */     return defaultInstance;
/*     */   }
/*     */   
/*     */   public void recordEventRegistration(EventRegistration registration) {
/*  37 */     synchronized (this.globalEventRegistrations) {
/*  38 */       List<EventRegistration> registrationList = (List)this.globalEventRegistrations.get(registration);
/*  39 */       if (registrationList == null) {
/*  40 */         registrationList = new ArrayList();
/*  41 */         this.globalEventRegistrations.put(registration, registrationList);
/*     */       }
/*  43 */       registrationList.add(registration);
/*     */       
/*     */ 
/*     */ 
/*  47 */       if (!registration.getQuerySpec().isDefault()) {
/*  48 */         EventRegistration defaultRegistration = registration.clone(QuerySpec.defaultQueryAtPath(registration.getQuerySpec().getPath()));
/*     */         
/*  50 */         registrationList = (List)this.globalEventRegistrations.get(defaultRegistration);
/*  51 */         if (registrationList == null) {
/*  52 */           registrationList = new ArrayList();
/*  53 */           this.globalEventRegistrations.put(defaultRegistration, registrationList);
/*     */         }
/*  55 */         registrationList.add(registration);
/*     */       }
/*     */       
/*  58 */       registration.setIsUserInitiated(true);
/*  59 */       registration.setOnZombied(this);
/*     */     }
/*     */   }
/*     */   
/*     */   private void unRecordEventRegistration(EventRegistration zombiedRegistration) {
/*  64 */     synchronized (this.globalEventRegistrations) {
/*  65 */       boolean found = false;
/*     */       
/*  67 */       List<EventRegistration> registrationList = (List)this.globalEventRegistrations.get(zombiedRegistration);
/*  68 */       if (registrationList != null) {
/*  69 */         for (int i = 0; i < registrationList.size(); i++) {
/*  70 */           if (registrationList.get(i) == zombiedRegistration) {
/*  71 */             found = true;
/*  72 */             registrationList.remove(i);
/*  73 */             break;
/*     */           }
/*     */         }
/*  76 */         if (registrationList.isEmpty()) {
/*  77 */           this.globalEventRegistrations.remove(zombiedRegistration);
/*     */         }
/*     */       }
/*  80 */       assert ((found) || (!zombiedRegistration.isUserInitiated()));
/*     */       
/*     */ 
/*     */ 
/*  84 */       if (!zombiedRegistration.getQuerySpec().isDefault()) {
/*  85 */         EventRegistration defaultRegistration = zombiedRegistration.clone(QuerySpec.defaultQueryAtPath(zombiedRegistration.getQuerySpec().getPath()));
/*     */         
/*     */ 
/*  88 */         registrationList = (List)this.globalEventRegistrations.get(defaultRegistration);
/*  89 */         if (registrationList != null) {
/*  90 */           for (int i = 0; i < registrationList.size(); i++) {
/*  91 */             if (registrationList.get(i) == zombiedRegistration) {
/*  92 */               registrationList.remove(i);
/*  93 */               break;
/*     */             }
/*     */           }
/*  96 */           if (registrationList.isEmpty()) {
/*  97 */             this.globalEventRegistrations.remove(defaultRegistration);
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public void zombifyForRemove(EventRegistration registration) {
/* 105 */     synchronized (this.globalEventRegistrations) {
/* 106 */       List<EventRegistration> registrationList = (List)this.globalEventRegistrations.get(registration);
/* 107 */       if ((registrationList != null) && (!registrationList.isEmpty())) {
/* 108 */         if (registration.getQuerySpec().isDefault())
/*     */         {
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
/* 120 */           HashSet<QuerySpec> zombiedQueries = new HashSet();
/*     */           
/* 122 */           for (int i = registrationList.size() - 1; i >= 0; i--) {
/* 123 */             EventRegistration currentRegistration = (EventRegistration)registrationList.get(i);
/* 124 */             if (!zombiedQueries.contains(currentRegistration.getQuerySpec())) {
/* 125 */               zombiedQueries.add(currentRegistration.getQuerySpec());
/* 126 */               currentRegistration.zombify();
/*     */             }
/*     */           }
/*     */         }
/*     */         else
/*     */         {
/* 132 */           ((EventRegistration)registrationList.get(0)).zombify();
/*     */         }
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public void onZombied(EventRegistration zombiedInstance)
/*     */   {
/* 140 */     unRecordEventRegistration(zombiedInstance);
/*     */   }
/*     */ }


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/core/ZombieEventManager.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */