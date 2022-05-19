package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

@AnnotationD(value = "BAR")
public class AnnotationMemberChangeExampleAfter {

  @AnnotationD("BAR")
  protected long someField;

  @AnnotationD(value="A string of no importance", othervalue = "A new string")
  protected String someStringField;

  @AnnotationD(value="A")
  protected long someOtherField;
}
