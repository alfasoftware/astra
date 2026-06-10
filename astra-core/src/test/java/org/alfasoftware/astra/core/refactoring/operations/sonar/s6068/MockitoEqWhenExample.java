package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import static org.mockito.ArgumentMatchers.eq;

import org.mockito.Mockito;

public class MockitoEqWhenExample {

  interface FooService {
    String multiArg(String a, String b);
    int singleArg(int x);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testMultipleEqArgs() {
    Mockito.when(foo.multiArg(eq("a"), eq("b"))).thenReturn("c");
  }

  void testSingleEqArg() {
    Mockito.when(foo.singleArg(eq(1))).thenReturn(2);
  }
}
