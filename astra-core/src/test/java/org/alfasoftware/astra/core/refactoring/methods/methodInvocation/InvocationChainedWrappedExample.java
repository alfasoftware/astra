package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import java.util.Optional;

import org.alfasoftware.astra.exampleTypes.A;

@SuppressWarnings("unused")
public class InvocationChainedWrappedExample {

  Optional<A> wrappedA = Optional.of(new A());

  private void a() {
    wrappedA.get().first().second();
  }
}

