package org.alfasoftware.astra.core.refactoring.methods.constructortobuilder;

import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.builder.BuiltType;

public class ConstructorToBuilderInnerClassBuilderExample {

  @SuppressWarnings("unused")
  void foo() {
    BuiltType one = new BuiltType(null, 1L, "");

    BuiltType two = new BuiltType("", Integer.valueOf(1), Integer.valueOf(2));

    BuiltType three = new BuiltType("", "", Integer.valueOf(1), Integer.valueOf(2));
  }
}
