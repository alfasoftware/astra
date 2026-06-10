package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import static org.mockito.ArgumentMatchers.eq;

import org.mockito.Mockito;

public class MockitoEqVerifyExample {

  interface FooService {
    String multiArg(String a, String b);
    int singleArg(int x);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testVerifyMultipleEqArgs() {
    Mockito.verify(foo).multiArg(eq("a"), eq("b"));
  }

  void testVerifySingleEqArg() {
    Mockito.verify(foo).singleArg(eq(42));
  }
}
