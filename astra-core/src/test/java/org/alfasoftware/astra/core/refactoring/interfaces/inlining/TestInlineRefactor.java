package org.alfasoftware.astra.core.refactoring.interfaces.inlining;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.types.InlineInterfaceRefactor;
import org.junit.Test;

public class TestInlineRefactor extends AbstractRefactorTest {

  @Test
  public void testInlineInterface() {
    assertRefactor(InlineInterfaceExample.class,
        new HashSet<>(Arrays.asList(new InlineInterfaceRefactor(SampleInterface.class.getName(),
          TEST_SOURCE + "/" + SampleInterface.class.getName().replaceAll("\\.", "/") + ".java"))));
  }


  @Test
  public void testInlineInterfaceGenerics() {
    assertRefactor(InlineInterfaceGenericsExample.class,
        new HashSet<>(Arrays.asList(new InlineInterfaceRefactor(SampleInterface.class.getName(),
          TEST_SOURCE + "/" + SampleInterface.class.getName().replaceAll("\\.", "/") + ".java"))));
  }


  @Test
  public void testInlineGenericTypedInterface() {
    assertRefactor(FooAccess.class,
        new HashSet<>(Arrays.asList(new InlineInterfaceRefactor(BaseInterface.class.getName(),
            TEST_SOURCE + "/" + BaseInterface.class.getName().replaceAll("\\.", "/") + ".java"))));
  }

  @Test
  public void testInlineInterfaceEdgeCases() {
    assertRefactor(InlineEdgeCasesExample.class,
        new HashSet<>(Arrays.asList(new InlineInterfaceRefactor(BaseInterface.class.getName(),
            TEST_SOURCE + "/" + BaseInterface.class.getName().replaceAll("\\.", "/") + ".java"))));
  }
}
