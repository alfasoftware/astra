package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

public class MockitoEqStaticImportExample {

  interface FooService {
    String multiArg(String a, String b);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testStaticWhenWithAllEqArgs() {
    when(foo.multiArg(eq("a"), eq("b"))).thenReturn("c");
  }

  void testStaticGivenWithAllEqArgs() {
    given(foo.multiArg(eq("a"), eq("b"))).willReturn("c");
  }

  void testStaticVerifyWithAllEqArgs() {
    verify(foo).multiArg(eq("a"), eq("b"));
  }
}
