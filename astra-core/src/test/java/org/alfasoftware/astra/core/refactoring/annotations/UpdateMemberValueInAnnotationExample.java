package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class UpdateMemberValueInAnnotationExample {

  @AnnotationD("FOO")
  protected long someField;

  @AnnotationD(value="FOO")
  protected String someStringField;
}

