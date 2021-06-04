package org.alfasoftware.astra.core.matchers;

import static org.alfasoftware.astra.core.matchers.ExampleUsedClass.methodWithClassArg;

class ExampleClassUsingMethodWithClassParameter {

  void foo() {
    methodWithClassArg(Class.class);
  }
}

