package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

public class UpdateMemberValueInAnnotationExample {

  @AnnotationA("FOO")
  protected long someField;

  @AnnotationA(value="FOO")
  protected String someStringField;
}

