package org.alfasoftware.astra.core.matchers;

class ExampleClassCallingAnnotatedMethod {

  @SuppressWarnings("deprecation")
  void callMethods() {
    ExampleUsedClassWithAnnotatedMethods obj = new ExampleUsedClassWithAnnotatedMethods();
    obj.deprecatedMethod();
    obj.suppressedMethod();
    obj.normalMethod();
  }
}
