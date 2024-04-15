package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod.staticmethodinvocation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.junit.Test;

public class TestStaticMethodInvocationMatching extends AbstractRefactorTest {

  /**
   * Tests that captured static method invocations are
   * qualified similarly after replacement.
   *
   * E.g. when changing the parameter to Collections.singletonList()
   * ensure that the result is also Collections.singletonList()
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
              Path.of(TEST_EXAMPLES + "/" + StaticMethodInvocationSubstitutePattern.class.getName().replaceAll("\\.", "/") + ".java")
            )
        )
    );
  }

}
