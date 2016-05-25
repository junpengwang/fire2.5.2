package org.shaded.apache.http;

public abstract interface HeaderElement
{
  public abstract String getName();
  
  public abstract String getValue();
  
  public abstract NameValuePair[] getParameters();
  
  public abstract NameValuePair getParameterByName(String paramString);
  
  public abstract int getParameterCount();
  
  public abstract NameValuePair getParameter(int paramInt);
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/HeaderElement.class
 * Java compiler version: 3 (47.0)
 * JD-Core Version:       0.7.1
 */