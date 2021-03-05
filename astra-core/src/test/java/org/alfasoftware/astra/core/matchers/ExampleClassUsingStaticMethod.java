package org.alfasoftware.astra.core.matchers;

import static org.alfasoftware.astra.core.matchers.ExampleUsedClass.staticMethod;

class ExampleClassUsingStaticMethod {

  void exampleMethod1() {
    System.out.println(staticMethod('f', 12));
  }

}

