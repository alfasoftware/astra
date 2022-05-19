package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class AddAndRemoveMemberFromAnnotationExample {

  @AnnotationD
  protected long someField;

  @AnnotationD(value="A string of no importance")
  protected String someStringField;

  // TODO
  @AnnotationD(value="A string of no importance", othervalue = "Other string of no importance")
  protected String someOtherStringField;
}
