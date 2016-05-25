package org.shaded.apache.http.conn.params;

import org.shaded.apache.http.conn.routing.HttpRoute;

public abstract interface ConnPerRoute
{
  public abstract int getMaxForRoute(HttpRoute paramHttpRoute);
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/conn/params/ConnPerRoute.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       0.7.1
 */