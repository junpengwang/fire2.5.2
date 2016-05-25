package com.firebase.tubesock;

public abstract interface WebSocketEventHandler
{
  public abstract void onOpen();
  
  public abstract void onMessage(WebSocketMessage paramWebSocketMessage);
  
  public abstract void onClose();
  
  public abstract void onError(WebSocketException paramWebSocketException);
  
  public abstract void onLogMessage(String paramString);
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/firebase/tubesock/WebSocketEventHandler.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */