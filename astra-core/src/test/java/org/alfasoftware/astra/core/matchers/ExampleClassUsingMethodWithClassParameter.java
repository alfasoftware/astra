package org.alfasoftware.astra.core.matchers;

import static org.alfasoftware.astra.core.matchers.ExampleUsedClass.methodWithClassArg;

public class ExampleClassUsingMethodWithClassParameter {

  void foo() {
    methodWithClassArg(Class.class);
  }
}

