package org.alfasoftware.astra.core.refactoring.types;

@SuppressWarnings("unused")
public class TypeReferenceCompactConstructorExample {

  public record Inner(int value) {
    public Inner {
      if (value < 0) {
        throw new IllegalArgumentException("value must not be negative");
      }
    }
  }

  Inner make() {
    return new Inner(1);
  }
}
