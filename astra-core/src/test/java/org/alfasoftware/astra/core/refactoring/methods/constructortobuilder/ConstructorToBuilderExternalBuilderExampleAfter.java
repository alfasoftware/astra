package org.alfasoftware.astra.core.refactoring.methods.constructortobuilder;

import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.builder.BuiltType;
import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.constructor.ConstructorType;

public class ConstructorToBuilderExternalBuilderExampleAfter {

  @SuppressWarnings("unused")
  void foo() {
    ConstructorType one = BuiltType.builderFor(null).withTwoAndThree(1L, "");

    ConstructorType two = BuiltType.builderForKey("").withValues(Integer.valueOf(1), Integer.valueOf(2)).build();

    ConstructorType three = BuiltType.builderForKey("").withValues("", Integer.valueOf(1), Integer.valueOf(2)).build();
  }
}
