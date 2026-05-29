package org.alfasoftware.astra.core.refactoring.types;

@SuppressWarnings("unused")
public class TypeReferenceCompactConstructorExampleAfter {

  public record InnerRenamed(int value) {
    public InnerRenamed {
      if (value < 0) {
        throw new IllegalArgumentException("value must not be negative");
      }
    }
  }

  InnerRenamed make() {
    return new InnerRenamed(1);
  }
}
