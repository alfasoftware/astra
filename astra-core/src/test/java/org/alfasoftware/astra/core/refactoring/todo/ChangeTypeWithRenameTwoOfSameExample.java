package org.alfasoftware.astra.core.refactoring.todo;

import java.util.Optional;

import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;

/**
 * Testing where duplicates would be created
 */
public class ChangeTypeWithRenameTwoOfSameExample {

  B directFieldNameOne;
  A directFieldNameTwo;
  Optional<B> wrappedFieldNameOne;
  Optional<A> wrappedFieldNameTwo;

  public ChangeTypeWithRenameTwoOfSameExample(
    B constructorDirectNameOne,
    A constructorDirectNameTwo,
    Optional<B> constructorWrappedNameOne,
    Optional<A> constructorWrappedNameTwo
  ) {
    this.directFieldNameOne = constructorDirectNameOne;
    this.directFieldNameTwo = constructorDirectNameTwo;
    this.wrappedFieldNameOne = constructorWrappedNameOne;
    this.wrappedFieldNameTwo = constructorWrappedNameTwo;
  }

  @SuppressWarnings("unused")
  private void function() {
    this.directFieldNameOne.equals(null);
    directFieldNameOne.equals(null);
    this.directFieldNameTwo.equals(null);
    directFieldNameTwo.equals(null);
    this.wrappedFieldNameOne.equals(null);
    wrappedFieldNameOne.equals(null);
    this.wrappedFieldNameTwo.equals(null);
    wrappedFieldNameTwo.equals(null);
  }
}


