package org.alfasoftware.astra.core.refactoring.operations.sonar.s5361;

import java.util.Set;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;

import org.junit.Test;

public class TestReplaceAllLiteralOperation extends AbstractRefactorTest {

  private static final Set<ReplaceAllLiteralOperation> OPERATION =
      Set.of(new ReplaceAllLiteralOperation());

  /**
   * Plain string literals and escaped metacharacters that should be rewritten to
   * {@code String.replace()}:
   * <ul>
   *   <li>Simple strings with no metacharacters</li>
   *   <li>Empty string</li>
   *   <li>Strings with spaces</li>
   *   <li>Escaped dot: {@code "\\."} &rarr; {@code "."}</li>
   *   <li>Escaped asterisk: {@code "\\*"} &rarr; {@code "*"}</li>
   *   <li>Escaped parenthesis: {@code "\\("} &rarr; {@code "("}</li>
   *   <li>Escaped backslash: {@code "\\\\"} &rarr; {@code "\\"}</li>
   * </ul>
   */
  @Test
  public void testReplaceAllRewrittenToReplace() {
    assertRefactor(ReplaceAllLiteralExample.class, OPERATION);
  }

  /**
   * Cases that must NOT be rewritten:
   * <ul>
   *   <li>Unescaped metacharacters: {@code .}, {@code [}, {@code *}, {@code +}, {@code ?},
   *       {@code ^}, {@code $}, {@code |}</li>
   *   <li>Regex shorthands: {@code \d}, {@code \w}, {@code \s}</li>
   *   <li>Non-literal first argument (variable)</li>
   *   <li>Non-{@code java.lang.String} receiver</li>
   * </ul>
   */
  @Test
  public void testNoopCases() {
    assertRefactor(ReplaceAllLiteralNoopExample.class, OPERATION);
  }
}
