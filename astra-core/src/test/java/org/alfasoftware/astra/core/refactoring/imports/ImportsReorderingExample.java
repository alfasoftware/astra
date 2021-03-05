package org.alfasoftware.astra.core.refactoring.imports;

import com.foo.bar.ExampleForImport;

import org.alfasoftware.astra.exampleTypes.a.Z;

import static org.alfasoftware.astra.exampleTypes.a.A.staticTwo;

import org.alfasoftware.astra.exampleTypes.A;

import static org.alfasoftware.astra.exampleTypes.EnumA.ONE;

import static org.alfasoftware.astra.exampleTypes.A.staticOne;

import static java.util.stream.Collectors.averagingDouble;

import org.alfasoftware.astra.exampleTypes.B;

/*
 * The imports in this file will be in a different order than the default
 * which might be applied by your IDE on saving, to test the refactor's sorting.
 */
@SuppressWarnings("unused")
public class ImportsReorderingExample {

  private void foo() {
    System.out.println(ONE);
    A a = new A();
    B b = new B();
    staticOne();
    Z z = new Z();
    staticTwo();
    averagingDouble(null);
    ExampleForImport adapter = null;
  }
}

