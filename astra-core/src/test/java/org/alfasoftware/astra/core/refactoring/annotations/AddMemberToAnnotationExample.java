package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

public class AddMemberToAnnotationExample {

  @AnnotationA
  protected long someField;

  @AnnotationA("Foo")
  protected long someOtherField;

  @AnnotationA(value="A string of no importance")
  protected String someStringField;
}

