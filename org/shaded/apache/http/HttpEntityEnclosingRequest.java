package org.shaded.apache.http;

public abstract interface HttpEntityEnclosingRequest
  extends HttpRequest
{
  public abstract boolean expectContinue();
  
  public abstract void setEntity(HttpEntity paramHttpEntity);
  
  public abstract HttpEntity getEntity();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/HttpEntityEnclosingRequest.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */