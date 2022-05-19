package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class UpdateMemberValueInAnnotationExampleAfter {

  @AnnotationD("BAR")
  protected long someField;

  @AnnotationD(value="BAR")
  protected String someStringField;
}

