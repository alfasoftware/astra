package org.alfasoftware.astra.core.refactoring.types;

import org.alfasoftware.astra.exampleTypes.A;

public class RenameExample {

  A directFieldName;

  public RenameExample(
    A constructorDirectName
  ) {
    this.directFieldName = constructorDirectName;
  }

  @SuppressWarnings("unused")
  private void function() {
    this.directFieldName.equals(null);
    directFieldName.equals(null);
  }
}
