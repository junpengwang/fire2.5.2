package org.shaded.apache.http.conn;

import org.shaded.apache.http.HttpResponse;
import org.shaded.apache.http.protocol.HttpContext;

public abstract interface ConnectionKeepAliveStrategy
{
  public abstract long getKeepAliveDuration(HttpResponse paramHttpResponse, HttpContext paramHttpContext);
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/conn/ConnectionKeepAliveStrategy.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       0.7.1
 */