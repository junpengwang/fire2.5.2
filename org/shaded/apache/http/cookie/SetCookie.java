package org.shaded.apache.http.cookie;

import java.util.Date;

public abstract interface SetCookie
  extends Cookie
{
  public abstract void setValue(String paramString);
  
  public abstract void setComment(String paramString);
  
  public abstract void setExpiryDate(Date paramDate);
  
  public abstract void setDomain(String paramString);
  
  public abstract void setPath(String paramString);
  
  public abstract void setSecure(boolean paramBoolean);
  
  public abstract void setVersion(int paramInt);
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/cookie/SetCookie.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       0.7.1
 */