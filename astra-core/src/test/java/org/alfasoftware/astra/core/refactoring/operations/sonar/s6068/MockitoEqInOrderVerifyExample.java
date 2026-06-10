package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import static org.mockito.ArgumentMatchers.eq;

import org.mockito.InOrder;
import org.mockito.Mockito;

public class MockitoEqInOrderVerifyExample {

  interface FooService {
    String multiArg(String a, String b);
  }

  private final FooService foo = Mockito.mock(FooService.class);
  private final InOrder inOrder = Mockito.inOrder(foo);

  void testInOrderVerifyWithAllEqArgs() {
    inOrder.verify(foo).multiArg(eq("a"), eq("b"));
  }
}
