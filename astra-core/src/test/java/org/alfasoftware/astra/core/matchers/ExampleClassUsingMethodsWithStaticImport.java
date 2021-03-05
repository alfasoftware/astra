package org.alfasoftware.astra.core.matchers;

import static java.util.Currency.getInstance;

class ExampleClassUsingMethodsWithStaticImport {

  void exampleMethod1(String someCurrencyCode) {

    getInstance(someCurrencyCode);
  }

}

