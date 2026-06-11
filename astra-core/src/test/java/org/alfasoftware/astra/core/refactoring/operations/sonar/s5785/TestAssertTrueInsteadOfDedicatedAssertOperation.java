package org.alfasoftware.astra.core.refactoring.operations.sonar.s5785;

import java.nio.file.Paths;
import java.util.Set;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.junit.Test;

public class TestAssertTrueInsteadOfDedicatedAssertOperation extends AbstractRefactorTest {

  private static final String M2 =
      Paths.get(System.getProperty("user.home"), ".m2", "repository").toString();

  private static final String[] JUNIT4_CLASSPATH = {
      Paths.get(M2, "junit", "junit", "4.13.2", "junit-4.13.2.jar").toString()
  };

  private static final String[] JUNIT5_CLASSPATH = {
      Paths.get(M2, "org", "junit", "jupiter", "junit-jupiter-api", "5.10.2", "junit-jupiter-api-5.10.2.jar").toString(),
      Paths.get(M2, "org", "opentest4j", "opentest4j", "1.3.0", "opentest4j-1.3.0.jar").toString()
  };

  private static final Set<AssertTrueInsteadOfDedicatedAssertOperation> OPERATION =
      Set.of(new AssertTrueInsteadOfDedicatedAssertOperation());

  /**
   * Core rewrites using qualified {@code Assert.*} calls (JUnit 4):
   * null checks, equals method, Objects.equals, primitive comparison,
   * object reference comparison, logical negation, and message argument.
   */
  @Test
  public void testJUnit4QualifiedCalls() {
    assertRefactorWithClassPath(AssertTrueJUnit4Example.class, OPERATION, JUNIT4_CLASSPATH);
  }

  /**
   * Core rewrites using qualified {@code Assertions.*} calls (JUnit 5),
   * including the JUnit-5 message-last convention.
   * Uses a .txt source file because junit-jupiter-api is not a Maven test compile dependency.
   */
  @Test
  public void testJUnit5QualifiedCalls() {
    assertRefactorWithSourcesAndClassPathAndTextFileExamples(
        "org.alfasoftware.astra.core.refactoring.operations.sonar.s5785.AssertTrueJUnit5Example",
        "AssertTrueJUnit5Example",
        OPERATION,
        new String[]{TEST_SOURCE},
        JUNIT5_CLASSPATH);
  }

  /**
   * Statically-imported calls with a wildcard import — method name and args are rewritten,
   * no import changes needed.
   */
  @Test
  public void testStaticWildcardImport() {
    assertRefactorWithClassPath(AssertTrueStaticImportExample.class, OPERATION, JUNIT4_CLASSPATH);
  }

  /**
   * Cases that must NOT be rewritten:
   * plain boolean variables, non-equals method calls, compound boolean expressions.
   */
  @Test
  public void testNoopCases() {
    assertRefactorWithClassPath(AssertTrueNoopExample.class, OPERATION, JUNIT4_CLASSPATH);
  }
}
