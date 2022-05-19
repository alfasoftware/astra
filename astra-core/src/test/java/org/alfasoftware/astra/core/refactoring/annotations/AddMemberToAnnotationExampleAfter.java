package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class AddMemberToAnnotationExampleAfter {

  @AnnotationD(value = "BAR")
  protected long someField;

  @AnnotationD(othervalue="A string of no importance", value = "BAR")
  protected String someStringField;
}

