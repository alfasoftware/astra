package org.alfasoftware.astra.core.matchers;

class ExampleClassUsingInvocationWithReturnTypeString {

  void exampleMethod() {
    ExampleUsedClass usedClass = new ExampleUsedClass();
    String result = usedClass.baseMethod();
    System.out.println(result);
  }
}
