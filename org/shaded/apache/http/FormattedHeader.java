package org.shaded.apache.http;

import org.shaded.apache.http.util.CharArrayBuffer;

public abstract interface FormattedHeader
  extends Header
{
  public abstract CharArrayBuffer getBuffer();
  
  public abstract int getValuePos();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/FormattedHeader.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */