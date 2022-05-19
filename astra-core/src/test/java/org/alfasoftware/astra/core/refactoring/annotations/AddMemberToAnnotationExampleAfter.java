package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationD;

public class AddMemberToAnnotationExampleAfter {

  @AnnotationD(othervalue = "BAR")
  protected long someField;

  @AnnotationD(value = "Foo", othervalue = "BAR")
  protected long someOtherField;

  @AnnotationD(value="A string of no importance", othervalue = "BAR")
  protected String someStringField;
}

