package org.alfasoftware.astra.core.refactoring.types;

import java.util.Optional;

import org.alfasoftware.astra.exampleTypes.A;

public class ChangeTypeWithRenameWrappedExample {

  Optional<A> wrappedFieldName;

  public ChangeTypeWithRenameWrappedExample(
    Optional<A> constructorWrappedName
  ) {
    this.wrappedFieldName = constructorWrappedName;
  }

  @SuppressWarnings("unused")
  private void function() {
    this.wrappedFieldName.equals(null);
    wrappedFieldName.equals(null);
  }
}
