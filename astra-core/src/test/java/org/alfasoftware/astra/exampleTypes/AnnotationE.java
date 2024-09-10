package org.alfasoftware.astra.exampleTypes;

public @interface AnnotationE {

  String value() default "";

  Class<?> type() default void.class;

  Class<?> anotherType() default void.class;
}

