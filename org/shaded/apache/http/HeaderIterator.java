package org.shaded.apache.http;

import java.util.Iterator;

public abstract interface HeaderIterator
  extends Iterator
{
  public abstract boolean hasNext();
  
  public abstract Header nextHeader();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/HeaderIterator.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */