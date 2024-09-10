package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationE;
import org.alfasoftware.astra.exampleTypes.WithNestedClass;

public class UpdateMemberTypeInAnnotationExampleAfter {

  @AnnotationE(type = Integer.class, anotherType = WithNestedClass.NestedClass.class)
  protected long someField;

  @AnnotationE(value = "A string of no importance", type = Integer.class, anotherType = WithNestedClass.NestedClass.class)
  protected String anotherField;
}

