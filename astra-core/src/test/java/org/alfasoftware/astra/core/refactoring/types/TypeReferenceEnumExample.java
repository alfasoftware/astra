package org.alfasoftware.astra.core.refactoring.types;

import org.alfasoftware.astra.exampleTypes.EnumA;

class TypeReferenceEnumExample {

  @SuppressWarnings("unused")
  void doFoo() {
    EnumA fullyQualified = org.alfasoftware.astra.exampleTypes.EnumA.ONE;
    EnumA simpleName = EnumA.ONE;
  }
}

