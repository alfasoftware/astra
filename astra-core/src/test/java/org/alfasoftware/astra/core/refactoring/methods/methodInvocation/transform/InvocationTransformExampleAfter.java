package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.transform;

public class InvocationTransformExampleAfter {

  @SuppressWarnings("unused")
  void doFoo() {
    ReturnsObject instance = new ReturnsObject();
    ReturnsObject sequence = instance;
  }
}
