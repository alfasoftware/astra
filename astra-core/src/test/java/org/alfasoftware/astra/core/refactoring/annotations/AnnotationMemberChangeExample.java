package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;
import org.alfasoftware.astra.exampleTypes.AnnotationD;

@AnnotationA(value = "FOO")
public class AnnotationMemberChangeExample {

  @AnnotationA("FOO")
  protected long someField;

  @AnnotationA(value="A string of no importance")
  protected String someStringField;

  @AnnotationD(description="A", othervalue = "B")
  protected long someOtherField;
}
