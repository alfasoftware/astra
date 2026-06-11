package org.alfasoftware.astra.core.refactoring.operations.sonar.s2959;

import java.util.Collections;
import java.util.Set;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.junit.Test;

public class TestUnnecessarySemicolonOperation extends AbstractRefactorTest {

  private static final Set<ASTOperation> OPERATION =
      Collections.singleton(new UnnecessarySemicolonOperation());

  /**
   * SonarJava's core S2959 detection: trailing semicolons after the last resource in
   * try-with-resources statements must be removed.
   */
  @Test
  public void testTryWithResourcesTrailingSemicolonIsRemoved() {
    assertRefactor(TryWithResourcesTrailingSemicolon.class, OPERATION);
  }

  /**
   * Standalone empty statements (lone {@code ;}) inside braced blocks are removed.
   * Includes static initialisers, instance initialisers, method bodies, and nested blocks.
   */
  @Test
  public void testBlockLevelEmptyStatementsAreRemoved() {
    assertRefactor(BlockLevelEmptyStatement.class, OPERATION);
  }

  /**
   * Semicolons that appear directly in a class body after method or constructor closing
   * braces (invisible to the JDT AST) are found via source scanning and removed.
   */
  @Test
  public void testClassLevelSemicolonsAreRemoved() {
    assertRefactor(ClassLevelSemicolon.class, OPERATION);
  }

  /**
   * Empty statements used as the body of control-flow constructs ({@code while}, {@code for},
   * enhanced {@code for}, {@code if}) are intentional and must NOT be removed.
   */
  @Test
  public void testEmptyControlFlowBodiesArePreserved() {
    assertRefactor(EmptyControlFlowBody.class, OPERATION);
  }

  /**
   * Empty statements inside {@code switch} case lists are removed.
   */
  @Test
  public void testSwitchCaseSemicolonsAreRemoved() {
    assertRefactor(SwitchCaseSemicolon.class, OPERATION);
  }

  /**
   * Semicolons in nested type bodies (static nested class, inner class, nested interface)
   * are removed, including inline double-semicolons on field declarations.
   */
  @Test
  public void testNestedTypeSemicolonsAreRemoved() {
    assertRefactor(NestedTypeSemicolon.class, OPERATION);
  }

  /**
   * Semicolons inside anonymous class bodies are removed. The field declaration's own
   * terminating {@code ;} must be preserved.
   */
  @Test
  public void testAnonymousClassSemicolonsAreRemoved() {
    assertRefactor(AnonymousClassSemicolon.class, OPERATION);
  }

  /**
   * A file containing a mix of class-level and block-level unnecessary semicolons is
   * handled correctly in a single pass.
   */
  @Test
  public void testMixedBlockAndClassLevelSemicolonsAreRemoved() {
    assertRefactor(MixedSemicolons.class, OPERATION);
  }

  /**
   * Enum body declarations section: stray semicolons between and after methods are removed,
   * while the required separator between enum constants and body declarations is preserved.
   */
  @Test
  public void testEnumBodySemicolonsAreHandledCorrectly() {
    assertRefactor(EnumBodySemicolon.class, OPERATION);
  }

  /**
   * Top-level enum with annotated constants: the required {@code ;} terminating the enum
   * constant list must NOT be removed, even though it sits in the gap between the opening
   * {@code \{} and the first body declaration. A class-level stray {@code ;} after a method
   * body inside the same enum must still be removed.
   */
  @Test
  public void testTopLevelEnumAnnotatedConstantsSemicolonPreserved() {
    assertRefactor(TopLevelEnumAnnotatedConstants.class, OPERATION);
  }

  /**
   * A {@code ;} immediately after the closing {@code \}} of a top-level class declaration
   * is removed.
   */
  @Test
  public void testPostTypeBraceSemicolonIsRemoved() {
    assertRefactor(ClassBraceSemicolon.class, OPERATION);
  }

  /**
   * Semicolons that appear only inside comments (line or block comments) must NOT be
   * touched, even when they fall in positions that the source-text scanner would otherwise
   * flag (e.g. class-body gaps between body declarations).
   */
  @Test
  public void testSemicolonsInsideCommentsArePreserved() {
    assertRefactor(CommentedSemicolon.class, OPERATION);
  }
}
