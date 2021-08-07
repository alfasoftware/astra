package org.alfasoftware.astra.core.refactoring.types;

import org.alfasoftware.astra.exampleTypes.EnumB;

class TypeReferenceEnumExampleAfter {

  @SuppressWarnings("unused")
  void doFoo() {
    EnumB fullyQualified = org.alfasoftware.astra.exampleTypes.EnumB.ONE;
    EnumB simpleName = EnumB.ONE;
  }
}

