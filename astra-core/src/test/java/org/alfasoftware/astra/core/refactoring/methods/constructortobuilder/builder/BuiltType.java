package org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.builder;

import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.constructor.ConstructorType;

@SuppressWarnings("unused")
public class BuiltType {

  // use builderFor(Object).withTwoAndThree(Long, String) instead
  public BuiltType(Object one, long two, String three) {
  }

  // use builderForKey(key).withValues(values).build() instead
  public BuiltType(String key, Object... values) {
  }

  // use builderForKey(key).withValues(values).build() instead
  public BuiltType(String key, String value0, Object... values) {
    this(key, values);
  }

  public static BuiltType.Builder builderFor(Object one) {
    return new BuiltType.Builder()
      .withOne(one);
  }

  public static BuiltType.Builder builderForKey(String key) {
    return new BuiltType.Builder()
      .withKey(key);
  }

  public static final class Builder {
    private String key;
    private Object one;
    private Object[] values;

    public Builder withOne(Object one) {
      this.one = one;
      return this;
    }
    public Builder withKey(String key) {
      this.key = key;
      return this;
    }
    public Builder withValues(Object... values) {
      this.values = values;
      return this;
    }
    public ConstructorType withTwoAndThree(Long two, String three) {
      return new ConstructorType(one, 1L, null);
    }
    public ConstructorType build() {
      return new ConstructorType(one, 1L, null);
    }
  }
}
