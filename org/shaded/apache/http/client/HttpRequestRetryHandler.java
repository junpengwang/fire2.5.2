package org.shaded.apache.http.client;

import java.io.IOException;
import org.shaded.apache.http.protocol.HttpContext;

public abstract interface HttpRequestRetryHandler
{
  public abstract boolean retryRequest(IOException paramIOException, int paramInt, HttpContext paramHttpContext);
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/client/HttpRequestRetryHandler.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       0.7.1
 */