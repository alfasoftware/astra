package org.alfasoftware.astra.core.refactoring.types.makefinal;

import java.util.Collections;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.types.MakeFinalClassOperation;
import org.alfasoftware.astra.core.utils.ASTOperation;
import org.junit.Test;

public class TestMakeFinalClassOperation extends AbstractRefactorTest {

  private static final java.util.Set<ASTOperation> OPERATION =
      Collections.singleton(new MakeFinalClassOperation());

  @Test
  public void testSinglePrivateConstructorGetsFinal() {
    assertRefactor(SimpleFinalCandidate.class, OPERATION);
  }

  @Test
  public void testMultiplePrivateConstructorsGetFinal() {
    assertRefactor(MultiPrivateCtor.class, OPERATION);
  }

  @Test
  public void testAlreadyFinalIsNoOp() {
    assertRefactor(AlreadyFinalClass.class, OPERATION);
  }

  @Test
  public void testAbstractClassIsNoOp() {
    assertRefactor(AbstractClassCandidate.class, OPERATION);
  }

  @Test
  public void testNonPrivateConstructorIsNoOp() {
    assertRefactor(HasNonPrivateCtor.class, OPERATION);
  }

  @Test
  public void testNoExplicitConstructorIsNoOp() {
    assertRefactor(NoExplicitCtor.class, OPERATION);
  }

  @Test
  public void testStaticNestedClassGetsFinal() {
    assertRefactor(NestedStaticClass.class, OPERATION);
  }
}
