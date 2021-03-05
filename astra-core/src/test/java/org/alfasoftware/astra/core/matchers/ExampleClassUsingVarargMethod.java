package org.alfasoftware.astra.core.matchers;

class ExampleClassUsingVarargMethod {

  void exampleMethod1(long someValue) {
    ExampleUsedClass usedClass = new ExampleUsedClass();

    @SuppressWarnings("unused")
    String methodResult = usedClass.methodWithArgs("123", String.valueOf(someValue), "456");
  }

}

