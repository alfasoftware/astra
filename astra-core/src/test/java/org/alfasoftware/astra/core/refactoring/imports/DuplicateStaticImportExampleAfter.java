package org.alfasoftware.astra.core.refactoring.imports;

import static org.alfasoftware.astra.exampleTypes.A.staticThree;
import static org.alfasoftware.astra.exampleTypes.B.staticThree;

@SuppressWarnings("unused")
public class DuplicateStaticImportExampleAfter {

  private void foo() {
    staticThree();
    staticThree("");
  }
}
