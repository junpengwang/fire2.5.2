package org.shaded.apache.http;

import java.util.Iterator;

public abstract interface TokenIterator
  extends Iterator
{
  public abstract boolean hasNext();
  
  public abstract String nextToken();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/TokenIterator.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */