package com.firebase.client;

public abstract interface ChildEventListener
{
  public abstract void onChildAdded(DataSnapshot paramDataSnapshot, String paramString);
  
  public abstract void onChildChanged(DataSnapshot paramDataSnapshot, String paramString);
  
  public abstract void onChildRemoved(DataSnapshot paramDataSnapshot);
  
  public abstract void onChildMoved(DataSnapshot paramDataSnapshot, String paramString);
  
  public abstract void onCancelled(FirebaseError paramFirebaseError);
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/ChildEventListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */