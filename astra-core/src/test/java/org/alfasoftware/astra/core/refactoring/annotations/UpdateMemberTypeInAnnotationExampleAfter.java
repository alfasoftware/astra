package org.alfasoftware.astra.core.refactoring.annotations;

import org.alfasoftware.astra.exampleTypes.AnnotationE;
import org.alfasoftware.astra.exampleTypes.WithNestedClass;

public class UpdateMemberTypeInAnnotationExampleAfter {

  @AnnotationE(anotherType = WithNestedClass.NestedClass.class, type = Integer.class)
  protected long someField;

  @AnnotationE(value = "A string of no importance", anotherType = WithNestedClass.NestedClass.class, type = Integer.class)
  protected String anotherField;
}

