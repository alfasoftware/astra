package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;

public class MockitoEqStaticImportExampleAfter {

  interface FooService {
    String multiArg(String a, String b);
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testStaticWhenWithAllEqArgs() {
    when(foo.multiArg("a", "b")).thenReturn("c");
  }

  void testStaticGivenWithAllEqArgs() {
    given(foo.multiArg("a", "b")).willReturn("c");
  }

  void testStaticVerifyWithAllEqArgs() {
    verify(foo).multiArg("a", "b");
  }
}
