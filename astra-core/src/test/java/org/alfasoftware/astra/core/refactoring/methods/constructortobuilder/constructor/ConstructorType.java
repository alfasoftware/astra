package org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.constructor;

import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.builder.BuiltType;

// To be switched to using BuiltType
@SuppressWarnings("unused")
public class ConstructorType extends BuiltType {

  // use builderFor(Object).withTwoAndThree(Long, String) instead
  public ConstructorType(Object one, long two, String three) {
    super(one, two, three);
  }

  // use builderForKey(key).withValues(values).build() instead
  public ConstructorType(String key, Object... values) {
    super(key, values);
  }

  // use builderForKey(key).withValues(values).build() instead
  public ConstructorType(String key, String value0, Object... values) {
    this(key, values);
  }
}

