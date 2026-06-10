package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import static org.mockito.ArgumentMatchers.eq;

import org.mockito.Mockito;

public class MockitoEqParenthesisedExample {

  interface FooService {
    String multiArg(String a, String b);
    int singleArg(int x);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testParenthesisedEqArgs() {
    Mockito.verify(foo).multiArg((eq("a")), (eq("b")));
    Mockito.when(foo.singleArg((eq(7)))).thenReturn(8);
  }
}
