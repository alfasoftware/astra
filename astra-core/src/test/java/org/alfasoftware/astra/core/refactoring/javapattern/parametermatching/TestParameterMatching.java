package org.alfasoftware.astra.core.refactoring.javapattern.parametermatching;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class TestParameterMatching extends AbstractRefactorTest {

  @Test
  public void test() throws IOException {
    assertRefactor(
        ParameterMatching.class,
        Collections.singleton(
            new JavaPatternASTOperation(
                new File(TEST_EXAMPLES + "/" + ParameterMatchingPattern.class.getName().replaceAll("\\.", "/") + ".java")
            )
        )
    );
  }

}
