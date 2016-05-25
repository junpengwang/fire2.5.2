package org.shaded.apache.http;

public abstract interface StatusLine
{
  public abstract ProtocolVersion getProtocolVersion();
  
  public abstract int getStatusCode();
  
  public abstract String getReasonPhrase();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/StatusLine.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */