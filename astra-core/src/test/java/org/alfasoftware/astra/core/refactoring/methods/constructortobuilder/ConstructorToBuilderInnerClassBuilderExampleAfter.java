package org.alfasoftware.astra.core.refactoring.methods.constructortobuilder;

import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.builder.BuiltType;

public class ConstructorToBuilderInnerClassBuilderExampleAfter {

  @SuppressWarnings("unused")
  void foo() {
    BuiltType one = BuiltType.builderFor(null).withTwoAndThree(1L, "");

    BuiltType two = BuiltType.builderForKey("").withValues(Integer.valueOf(1), Integer.valueOf(2)).build();

    BuiltType three = BuiltType.builderForKey("").withValues("", Integer.valueOf(1), Integer.valueOf(2)).build();
  }
}
