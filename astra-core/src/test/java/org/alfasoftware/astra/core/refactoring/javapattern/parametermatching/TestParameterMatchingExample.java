package org.alfasoftware.astra.core.refactoring.javapattern.parametermatching;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class TestParameterMatchingExample extends AbstractRefactorTest {

  /**
   * Tests that the parameters in a JavaPattern are captured
   * and replaced correctly.
   *
   * See {@link ParameterMatchingPattern} for details.
   *
   */
  @Test
  public void test() throws IOException {
    assertRefactor(
        ParameterMatchingExample.class,
        Collections.singleton(
            new JavaPatternASTOperation(
                new File(TEST_EXAMPLES + "/" + ParameterMatchingPattern.class.getName().replaceAll("\\.", "/") + ".java")
            )
        )
    );
  }

}
