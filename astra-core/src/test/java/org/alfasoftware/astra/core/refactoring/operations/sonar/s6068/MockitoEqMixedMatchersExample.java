package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.Mockito;

public class MockitoEqMixedMatchersExample {

  interface FooService {
    String multiArg(String a, String b);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testMixedMatchersNotSimplified() {
    // Not all args are eq() — must not be simplified (mixing would break Mockito)
    Mockito.verify(foo).multiArg(eq("a"), anyString());
    Mockito.when(foo.multiArg(anyString(), eq("b"))).thenReturn("c");
  }
}
