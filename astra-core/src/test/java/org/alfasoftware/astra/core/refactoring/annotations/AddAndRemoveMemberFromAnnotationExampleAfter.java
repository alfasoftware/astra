package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class AddAndRemoveMemberFromAnnotationExampleAfter {

  @AnnotationD(othervalue = "BAR")
  protected long someField;

  @AnnotationD(othervalue = "BAR")
  protected String someStringField;

  // TODO
  @AnnotationD(othervalue = "Other string of no importance")
  protected String someOtherStringField;
}

