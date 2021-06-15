package org.alfasoftware.astra.core.refactoring.imports;

import static org.alfasoftware.astra.exampleTypes.A.staticThree;
import static org.alfasoftware.astra.exampleTypes.B.staticThree;

class DuplicateStaticImportExampleAfter {

  void foo() {
    staticThree();
    staticThree("");
  }
}
