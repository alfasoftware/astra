package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import org.mockito.Mockito;

public class MockitoEqParenthesisedExampleAfter {

  interface FooService {
    String multiArg(String a, String b);
    int singleArg(int x);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testParenthesisedEqArgs() {
    Mockito.verify(foo).multiArg("a", "b");
    Mockito.when(foo.singleArg(7)).thenReturn(8);
  }
}
