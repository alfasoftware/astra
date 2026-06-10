package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import org.mockito.Mockito;

public class MockitoEqWhenExampleAfter {

  interface FooService {
    String multiArg(String a, String b);
    int singleArg(int x);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testMultipleEqArgs() {
    Mockito.when(foo.multiArg("a", "b")).thenReturn("c");
  }

  void testSingleEqArg() {
    Mockito.when(foo.singleArg(1)).thenReturn(2);
  }
}
