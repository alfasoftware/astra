package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class AddMemberToAnnotationExample {

  @AnnotationD
  protected long someField;

  @AnnotationD("Foo")
  protected long someOtherField;

  @AnnotationD(value="A string of no importance")
  protected String someStringField;
}
