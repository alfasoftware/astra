package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import org.mockito.Mockito;

public class MockitoEqStubberWhenExampleAfter {

  interface FooService {
    String multiArg(String a, String b);
    int singleArg(int x);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testDoReturnWhenWithAllEqArgs() {
    Mockito.doReturn("result").when(foo).multiArg("a", "b");
  }

  void testDoThrowWhenWithSingleEqArg() {
    Mockito.doThrow(new RuntimeException()).when(foo).singleArg(99);
  }
}
