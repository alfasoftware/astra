package org.alfasoftware.astra.core.matchers;

class ExampleUsedClassWithAnnotatedMethods {

  @Deprecated
  void deprecatedMethod() {}

  @SuppressWarnings("unused")
  void suppressedMethod() {}

  void normalMethod() {}
}
