package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationE;

public class UpdateMemberTypeInAnnotationExample {

  @AnnotationE
  protected long someField;

  @AnnotationE(value = "A string of no importance")
  protected String anotherField;
}

