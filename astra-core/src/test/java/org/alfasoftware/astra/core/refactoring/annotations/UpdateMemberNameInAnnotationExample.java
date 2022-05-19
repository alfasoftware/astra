package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class UpdateMemberNameInAnnotationExample {

  @AnnotationD
  protected long someField;

  @AnnotationD(value="A string of no importance")
  protected String someStringField;
}

