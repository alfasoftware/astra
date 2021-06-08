package org.alfasoftware.astra.core.refactoring.imports;

import static org.alfasoftware.astra.exampleTypes.A.staticFour;
import static org.alfasoftware.astra.exampleTypes.A.staticThree;

@SuppressWarnings("unused")
public class DuplicateStaticImportExample {

  private void foo() {
    staticThree();
    staticFour("");
  }
}
