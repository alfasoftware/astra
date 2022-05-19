package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationA;

public class RemoveMemberFromAnnotationExample {

  @AnnotationA(value="BAR")
  protected long someField;

  @AnnotationA(othervalue="A string of no importance", value="BAR")
  protected String someStringField;
}

