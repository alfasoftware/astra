package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link MockitoEqSimplificationRefactor} (java:S6068).
 *
 * <p>Covers all five Mockito entry points, static imports of when/verify/given,
 * parenthesised eq() calls, single vs multiple arguments, and no-op cases where
 * the rule must not fire (mixed matchers, zero-arg methods).
 *
 * <p>Mockito is passed as an explicit JDT classpath entry so that method
 * bindings resolve to their declaring types — required for exact rule matching.
 */
public class TestMockitoEqSimplificationRefactor extends AbstractRefactorTest {

  /**
   * Path to the mockito-core jar on the local Maven classpath.
   * Resolved at runtime from the test classloader so it tracks the pom.xml version.
   */
  private static final String MOCKITO_JAR;
  static {
    try {
      MOCKITO_JAR = Paths.get(
          Mockito.class.getProtectionDomain().getCodeSource().getLocation().toURI()
      ).toString();
    } catch (URISyntaxException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static final String[] MOCKITO_CLASSPATH = new String[]{MOCKITO_JAR};


  /** {@code Mockito.when(foo.method(eq(x)))} — all-eq arguments are unwrapped. */
  @Test
  public void testWhenAllEqArgs_simplified() {
    assertRefactorWithClassPath(MockitoEqWhenExample.class,
        Collections.singleton(new MockitoEqSimplificationRefactor()),
        MOCKITO_CLASSPATH);
  }


  /** {@code BDDMockito.given(foo.method(eq(x)))} — all-eq arguments are unwrapped. */
  @Test
  public void testGivenAllEqArgs_simplified() {
    assertRefactorWithClassPath(MockitoEqGivenExample.class,
        Collections.singleton(new MockitoEqSimplificationRefactor()),
        MOCKITO_CLASSPATH);
  }


  /** {@code Mockito.verify(mock).method(eq(x))} — chained-call pattern, all-eq arguments are unwrapped. */
  @Test
  public void testVerifyAllEqArgs_simplified() {
    assertRefactorWithClassPath(MockitoEqVerifyExample.class,
        Collections.singleton(new MockitoEqSimplificationRefactor()),
        MOCKITO_CLASSPATH);
  }


  /** {@code inOrder.verify(mock).method(eq(x))} — InOrder chained-call pattern, all-eq arguments are unwrapped. */
  @Test
  public void testInOrderVerifyAllEqArgs_simplified() {
    assertRefactorWithClassPath(MockitoEqInOrderVerifyExample.class,
        Collections.singleton(new MockitoEqSimplificationRefactor()),
        MOCKITO_CLASSPATH);
  }


  /**
   * {@code Mockito.doReturn(...).when(mock).method(eq(x))} — Stubber chained-call
   * pattern (doReturn, doThrow), all-eq arguments are unwrapped.
   */
  @Test
  public void testStubberWhenAllEqArgs_simplified() {
    assertRefactorWithClassPath(MockitoEqStubberWhenExample.class,
        Collections.singleton(new MockitoEqSimplificationRefactor()),
        MOCKITO_CLASSPATH);
  }


  /**
   * Static imports of {@code when}, {@code given}, and {@code verify} — the operation
   * resolves their declaring types via bindings and still simplifies correctly.
   * The now-unused static import of {@code eq} is removed.
   */
  @Test
  public void testStaticImports_simplified() {
    assertRefactorWithClassPath(MockitoEqStaticImportExample.class,
        Collections.singleton(new MockitoEqSimplificationRefactor()),
        MOCKITO_CLASSPATH);
  }


  /**
   * {@code (eq(x))} — parenthesised eq() calls are unwrapped after stripping the
   * parentheses, and the whole {@code (eq(x))} expression is replaced with just {@code x}.
   */
  @Test
  public void testParenthesisedEq_simplified() {
    assertRefactorWithClassPath(MockitoEqParenthesisedExample.class,
        Collections.singleton(new MockitoEqSimplificationRefactor()),
        MOCKITO_CLASSPATH);
  }


  /**
   * Mixed matchers — when at least one argument uses a non-eq matcher (e.g. anyString()),
   * the operation must not simplify any argument (removing eq() would break Mockito at runtime).
   */
  @Test
  public void testMixedMatchers_notSimplified() {
    assertRefactorWithClassPath(MockitoEqMixedMatchersExample.class,
        Collections.singleton(new MockitoEqSimplificationRefactor()),
        MOCKITO_CLASSPATH);
  }


  /**
   * Zero-argument method calls — nothing to simplify; the operation is a no-op.
   */
  @Test
  public void testZeroArgs_notSimplified() {
    assertRefactorWithClassPath(MockitoEqZeroArgsExample.class,
        Collections.singleton(new MockitoEqSimplificationRefactor()),
        MOCKITO_CLASSPATH);
  }
}
