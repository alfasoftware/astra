package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class TestSubstituteMethodExampleMatching extends AbstractRefactorTest {

  @Test
  public void test() throws IOException {
    assertRefactor(
        SubstituteMethodExample.class,
        Collections.singleton(
            new JavaPatternASTOperation(
                new File(TEST_EXAMPLES + "/" + SubstituteMethodExamplePattern.class.getName().replaceAll("\\.", "/") + ".java")
            )
        )
    );
  }

}
