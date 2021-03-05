package org.alfasoftware.astra.core.refactoring.imports;

import static java.util.stream.Collectors.averagingDouble;
import static org.alfasoftware.astra.exampleTypes.A.staticOne;
import static org.alfasoftware.astra.exampleTypes.EnumA.ONE;
import static org.alfasoftware.astra.exampleTypes.a.A.staticTwo;

import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;
import org.alfasoftware.astra.exampleTypes.a.Z;

import com.foo.bar.ExampleForImport;

/*
 * The imports in this file will be in a different order than the default
 * which might be applied by your IDE on saving, to test the refactor's sorting.
 */
@SuppressWarnings("unused")
public class ImportsReorderingExampleAfter {

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

