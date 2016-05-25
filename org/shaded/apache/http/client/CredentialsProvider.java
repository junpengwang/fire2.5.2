package org.shaded.apache.http.client;

import org.shaded.apache.http.auth.AuthScope;
import org.shaded.apache.http.auth.Credentials;

public abstract interface CredentialsProvider
{
  public abstract void setCredentials(AuthScope paramAuthScope, Credentials paramCredentials);
  
  public abstract Credentials getCredentials(AuthScope paramAuthScope);
  
  public abstract void clear();
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/org/shaded/apache/http/client/CredentialsProvider.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       0.7.1
 */