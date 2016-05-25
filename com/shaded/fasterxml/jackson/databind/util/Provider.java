package com.shaded.fasterxml.jackson.databind.util;

import java.util.Collection;

@Deprecated
public abstract interface Provider<T>
{
  public abstract Collection<T> provide();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/shaded/fasterxml/jackson/databind/util/Provider.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */