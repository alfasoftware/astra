package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class AddMemberToAnnotationExampleAfter {

  @AnnotationD(description="BAR")
  protected long someField;

  @AnnotationD(othervalue="A string of no importance", description="BAR")
  protected String someStringField;
}

