package org.alfasoftware.astra.exampleTypes;

public @interface AnnotationD {

  String description();

  String othervalue() default "";
}
