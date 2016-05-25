package org.shaded.apache.http;

import java.net.InetAddress;

public abstract interface HttpInetConnection
  extends HttpConnection
{
  public abstract InetAddress getLocalAddress();
  
  public abstract int getLocalPort();
  
  public abstract InetAddress getRemoteAddress();
  
  public abstract int getRemotePort();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/HttpInetConnection.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */