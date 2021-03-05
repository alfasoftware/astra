package org.alfasoftware.astra.core.matchers;

class ExampleClassUsingMultipleMethods {

  void exampleMethod1() {
    ExampleUsedClass usedClass = new ExampleUsedClass();

    usedClass.methodWithArgs(usedClass.baseMethod(), "foo");
  }

}

