package org.alfasoftware.astra.core.matchers;

import java.util.Date;

class ExampleClassUsingMethodsWithFullyQualifiedName {

  void exampleMethod1(long someValue) {

    Date date = new Date();
    date.setTime(someValue);
    date.notify();

  }

}

