package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

public class AnnotationChangeWithPredicateAndTransformExample {

  @AnnotationA
  protected long someField;

  @AnnotationA("Foo")
  protected long someOtherField;
}

