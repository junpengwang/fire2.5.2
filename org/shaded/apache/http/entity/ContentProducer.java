package org.shaded.apache.http.entity;

import java.io.IOException;
import java.io.OutputStream;

public abstract interface ContentProducer
{
  public abstract void writeTo(OutputStream paramOutputStream)
    throws IOException;
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/entity/ContentProducer.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */