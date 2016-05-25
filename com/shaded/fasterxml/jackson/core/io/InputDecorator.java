package com.shaded.fasterxml.jackson.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;

public abstract class InputDecorator
  implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  public abstract InputStream decorate(IOContext paramIOContext, InputStream paramInputStream)
    throws IOException;
  
  public abstract InputStream decorate(IOContext paramIOContext, byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException;
  
  public abstract Reader decorate(IOContext paramIOContext, Reader paramReader)
    throws IOException;
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/shaded/fasterxml/jackson/core/io/InputDecorator.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */