package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import java.util.Optional;

import org.alfasoftware.astra.exampleTypes.A;

@SuppressWarnings("unused")
class InvocationChainedWrappedExample {

  Optional<A> foo = Optional.of(new A());

  private void a() {
    foo.get().first().second();
  }
}

