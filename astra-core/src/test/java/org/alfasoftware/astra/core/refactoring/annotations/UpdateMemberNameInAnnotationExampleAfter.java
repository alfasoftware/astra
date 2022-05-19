package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class UpdateMemberNameInAnnotationExampleAfter {

  @AnnotationD
  protected long someField;

  @AnnotationD(othervalue="A string of no importance")
  protected String someStringField;
}

