package org.alfasoftware.astra.core.refactoring.types;

import org.alfasoftware.astra.exampleTypes.B;

public class RenameExampleAfter {

  B nameAfter;

  public RenameExampleAfter(
    B nameAfter
  ) {
    this.nameAfter = nameAfter;
  }

  @SuppressWarnings("unused")
  private void function() {
    this.nameAfter.equals(null);
    nameAfter.equals(null);
  }
}
