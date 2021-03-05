package org.alfasoftware.astra.core.refactoring.methods.constructortobuilder;

import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.constructor.ConstructorType;

public class ConstructorToBuilderExternalBuilderExample {

  @SuppressWarnings("unused")
  void foo() {
    ConstructorType one = new ConstructorType(null, 1L, "");

    ConstructorType two = new ConstructorType("", Integer.valueOf(1), Integer.valueOf(2));

    ConstructorType three = new ConstructorType("", "", Integer.valueOf(1), Integer.valueOf(2));
  }
}
