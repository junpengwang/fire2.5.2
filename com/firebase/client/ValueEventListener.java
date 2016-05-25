package com.firebase.client;

public abstract interface ValueEventListener
{
  public abstract void onDataChange(DataSnapshot paramDataSnapshot);
  
  public abstract void onCancelled(FirebaseError paramFirebaseError);
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/client/ValueEventListener.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */