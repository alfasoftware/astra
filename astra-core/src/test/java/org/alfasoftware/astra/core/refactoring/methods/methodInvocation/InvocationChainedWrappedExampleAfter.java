package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import java.util.Optional;

import org.alfasoftware.astra.exampleTypes.C;

@SuppressWarnings("unused")
public class InvocationChainedWrappedExampleAfter {

  Optional<C> wrappedC = Optional.of(new C());

  private void a() {
    wrappedC.get().third();
  }
}

