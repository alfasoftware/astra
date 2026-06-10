package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import static org.mockito.ArgumentMatchers.eq;

import org.mockito.BDDMockito;
import org.mockito.Mockito;

public class MockitoEqGivenExample {

  interface FooService {
    String multiArg(String a, String b);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testGivenWithAllEqArgs() {
    BDDMockito.given(foo.multiArg(eq("a"), eq("b"))).willReturn("c");
  }
}
