package org.alfasoftware.astra.core.refactoring.types;

import org.alfasoftware.astra.exampleTypes.EnumB;

public class ChangeTypeInEnumExampleAfter {

  @SuppressWarnings("unused")
  void doFoo() {
    EnumB fullyQualified = org.alfasoftware.astra.exampleTypes.EnumB.ONE;
    EnumB simpleName = EnumB.ONE;
  }
}

