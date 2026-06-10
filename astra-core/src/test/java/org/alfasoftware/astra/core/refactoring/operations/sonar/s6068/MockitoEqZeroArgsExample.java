package org.alfasoftware.astra.core.refactoring.operations.sonar.s6068;

import org.mockito.Mockito;

public class MockitoEqZeroArgsExample {

  interface FooService {
    void noArgs();
    String noArgsReturnsString();
  }

  private final FooService foo = Mockito.mock(FooService.class);

  void testZeroArgVerifyNotSimplified() {
    // Zero-arg method — nothing to simplify
    Mockito.verify(foo).noArgs();
    Mockito.when(foo.noArgsReturnsString()).thenReturn("result");
  }
}
