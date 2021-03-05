package org.alfasoftware.astra.core.refactoring.javadoc;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.javadoc.JavadocTagRefactor;
import org.eclipse.jdt.core.dom.TagElement;
import org.junit.Test;

public class TestJavadocTagsRefactor extends AbstractRefactorTest {

  @Test
  public void testJavadocTagRefactor() {
    assertRefactor(JavadocTagExample.class,
      new HashSet<>(Arrays.asList(JavadocTagRefactor.fromTag(TagElement.TAG_LINK).toTag(TagElement.TAG_LINKPLAIN))));
  }

}
