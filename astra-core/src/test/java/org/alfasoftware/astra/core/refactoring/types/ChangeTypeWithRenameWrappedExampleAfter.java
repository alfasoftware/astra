package org.alfasoftware.astra.core.refactoring.types;

import java.util.Optional;

import org.alfasoftware.astra.exampleTypes.B;

public class ChangeTypeWithRenameWrappedExampleAfter {

  Optional<B> nameAfter;

  public ChangeTypeWithRenameWrappedExampleAfter(
    Optional<B> nameAfter
  ) {
    this.nameAfter = nameAfter;
  }

  @SuppressWarnings("unused")
  private void function() {
    this.nameAfter.equals(null);
    nameAfter.equals(null);
  }
}
