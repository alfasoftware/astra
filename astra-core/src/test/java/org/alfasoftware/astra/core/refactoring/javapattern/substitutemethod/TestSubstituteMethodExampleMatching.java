package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.junit.Test;

public class TestSubstituteMethodExampleMatching extends AbstractRefactorTest {

  @Test
  public void test() throws IOException {
    assertRefactor(
        SubstituteMethodExample.class,
        Collections.singleton(
            new JavaPatternASTOperation(
                Path.of(TEST_EXAMPLES + "/" + SubstituteMethodExamplePattern.class.getName().replaceAll("\\.", "/") + ".java")
            )
        )
    );
  }

}
