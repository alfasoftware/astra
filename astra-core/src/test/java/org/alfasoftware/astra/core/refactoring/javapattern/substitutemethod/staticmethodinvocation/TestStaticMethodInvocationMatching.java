package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod.staticmethodinvocation;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class TestStaticMethodInvocationMatching extends AbstractRefactorTest {

  /**
   * Tests that captured static method invocations are
   * qualified similarly after replacement.
   *
   * E.g. when changing the parameter to Collections.singletonList()
   * ensure that the result is also Collections.singletoneList()
   * and not just singletonList()
   *
   * @throws IOException
   */
  @Test
  public void test() throws IOException {
    assertRefactor(
        StaticMethodInvocation.class,
        Collections.singleton(
            new JavaPatternASTOperation(
                new File(TEST_EXAMPLES + "/" + StaticMethodInvocationSubstitutePattern.class.getName().replaceAll("\\.", "/") + ".java")
            )
        )
    );
  }

}
