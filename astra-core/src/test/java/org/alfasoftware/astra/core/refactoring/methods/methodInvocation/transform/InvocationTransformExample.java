package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.transform;

public class InvocationTransformExample {

  @SuppressWarnings("unused")
  void doFoo() {
    ReturnsObject instance = new ReturnsObject();
    ReturnsObject sequence = (ReturnsObject) instance.get();
  }
}
