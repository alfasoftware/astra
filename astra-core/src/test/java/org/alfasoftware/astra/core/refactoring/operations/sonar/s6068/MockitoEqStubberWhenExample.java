package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import static org.mockito.ArgumentMatchers.eq;

import org.mockito.Mockito;

public class MockitoEqStubberWhenExample {

  interface FooService {
    String multiArg(String a, String b);
    int singleArg(int x);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testDoReturnWhenWithAllEqArgs() {
    Mockito.doReturn("result").when(foo).multiArg(eq("a"), eq("b"));
  }

  void testDoThrowWhenWithSingleEqArg() {
    Mockito.doThrow(new RuntimeException()).when(foo).singleArg(eq(99));
  }
}
