package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class AnnotationChangeWithPredicateAndTransformExample {

  @AnnotationD
  protected long someField;

  @AnnotationD("Foo")
  protected long someOtherField;
}

