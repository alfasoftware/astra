package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import java.util.Optional;

import org.alfasoftware.astra.exampleTypes.C;

@SuppressWarnings("unused")
class InvocationChainedWrappedExampleAfter {

  Optional<C> foo = Optional.of(new C());

  private void a() {
    foo.get().third();
  }
}

