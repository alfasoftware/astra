package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

public class UpdateMemberNameInAnnotationExample {

  @AnnotationA
  protected long someField;

  @AnnotationA("A string of no importance")
  protected String someStringField;

  @AnnotationA(value = "A string of no importance")
  protected String someStringField2;
}

