package org.alfasoftware.astra.core.refactoring.imports;

import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;
import org.alfasoftware.astra.exampleTypes.C;
import org.alfasoftware.astra.exampleTypes.D;

@SuppressWarnings("unused")
public class UnusedImportExample {

  A a = new A();
  /**
   * {@link C}
   * {@link D#one()}
   */
  private void foo() {
  }
}

