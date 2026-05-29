package org.alfasoftware.astra.exampleTypes;

public record RecordB(int value, String name) {

  public RecordB {
    if (value < 0) {
      throw new IllegalArgumentException("value must not be negative");
    }
  }

  public String describe() {
    return name + "=" + value;
  }
}
