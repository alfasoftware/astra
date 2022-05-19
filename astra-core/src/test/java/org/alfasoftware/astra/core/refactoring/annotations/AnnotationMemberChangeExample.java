package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

@AnnotationA(value = "FOO")
public class AnnotationMemberChangeExample {

  @AnnotationA("FOO")
  protected long someField;
}
