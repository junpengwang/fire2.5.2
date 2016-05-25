package org.shaded.apache.http.client.methods;

import java.net.URI;
import org.shaded.apache.http.HttpRequest;

public abstract interface HttpUriRequest
  extends HttpRequest
{
  public abstract String getMethod();
  
  public abstract URI getURI();
  
  public abstract void abort()
    throws UnsupportedOperationException;
  
  public abstract boolean isAborted();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/client/methods/HttpUriRequest.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       0.7.1
 */