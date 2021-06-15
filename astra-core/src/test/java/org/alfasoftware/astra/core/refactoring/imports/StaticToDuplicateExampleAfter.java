package org.alfasoftware.astra.core.refactoring.imports;

import static org.alfasoftware.astra.exampleTypes.A.staticFour;
import static org.alfasoftware.astra.exampleTypes.A.staticThree;

class StaticToDuplicateExampleAfter {

  void foo() {
    staticThree();
    staticFour("");
  }
}

