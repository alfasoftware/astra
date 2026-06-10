package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import org.mockito.Mockito;

public class MockitoEqVerifyExampleAfter {

  interface FooService {
    String multiArg(String a, String b);
    int singleArg(int x);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testVerifyMultipleEqArgs() {
    Mockito.verify(foo).multiArg("a", "b");
  }

  void testVerifySingleEqArg() {
    Mockito.verify(foo).singleArg(42);
  }
}
