package com.shaded.fasterxml.jackson.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({java.lang.annotation.ElementType.ANNOTATION_TYPE, java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.CONSTRUCTOR, java.lang.annotation.ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonIgnore
{
  boolean value() default true;
}


/* Location:              /Users/junpengwang/Downloads/Download/firebase-client-android-2.5.2.jar!/com/shaded/fasterxml/jackson/annotation/JsonIgnore.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */