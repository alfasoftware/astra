package org.alfasoftware.astra.core.matchers;

class ExampleClassUsingOneArgMethod {

  void exampleMethod1(long someValue) {
    ExampleUsedClass usedClass = new ExampleUsedClass();

    String methodResult = usedClass.methodWithArgs(someValue);

    // as a possible false match
    methodResult.notify();
  }

  @SuppressWarnings("unused")
  void getInstance(String value) {
    //no body - used to check for false positive matches
  }

}

