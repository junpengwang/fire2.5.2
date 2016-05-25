package com.shaded.fasterxml.jackson.core;

import java.io.IOException;

public abstract interface PrettyPrinter
{
  public abstract void writeRootValueSeparator(JsonGenerator paramJsonGenerator)
    throws IOException, JsonGenerationException;
  
  public abstract void writeStartObject(JsonGenerator paramJsonGenerator)
    throws IOException, JsonGenerationException;
  
  public abstract void writeEndObject(JsonGenerator paramJsonGenerator, int paramInt)
    throws IOException, JsonGenerationException;
  
  public abstract void writeObjectEntrySeparator(JsonGenerator paramJsonGenerator)
    throws IOException, JsonGenerationException;
  
  public abstract void writeObjectFieldValueSeparator(JsonGenerator paramJsonGenerator)
    throws IOException, JsonGenerationException;
  
  public abstract void writeStartArray(JsonGenerator paramJsonGenerator)
    throws IOException, JsonGenerationException;
  
  public abstract void writeEndArray(JsonGenerator paramJsonGenerator, int paramInt)
    throws IOException, JsonGenerationException;
  
  public abstract void writeArrayValueSeparator(JsonGenerator paramJsonGenerator)
    throws IOException, JsonGenerationException;
  
  public abstract void beforeArrayValues(JsonGenerator paramJsonGenerator)
    throws IOException, JsonGenerationException;
  
  public abstract void beforeObjectEntries(JsonGenerator paramJsonGenerator)
    throws IOException, JsonGenerationException;
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/shaded/fasterxml/jackson/core/PrettyPrinter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */