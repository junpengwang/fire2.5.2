package org.shaded.apache.http.io;

import java.io.IOException;
import org.shaded.apache.http.util.CharArrayBuffer;

public abstract interface SessionInputBuffer
{
  public abstract int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException;
  
  public abstract int read(byte[] paramArrayOfByte)
    throws IOException;
  
  public abstract int read()
    throws IOException;
  
  public abstract int readLine(CharArrayBuffer paramCharArrayBuffer)
    throws IOException;
  
  public abstract String readLine()
    throws IOException;
  
  public abstract boolean isDataAvailable(int paramInt)
    throws IOException;
  
  public abstract HttpTransportMetrics getMetrics();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/io/SessionInputBuffer.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */