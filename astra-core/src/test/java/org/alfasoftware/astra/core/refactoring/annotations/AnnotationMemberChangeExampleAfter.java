package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

@AnnotationD(description = "BAR")
public class AnnotationMemberChangeExampleAfter {

  @AnnotationD(description = "BAR")
  protected long someField;
}
