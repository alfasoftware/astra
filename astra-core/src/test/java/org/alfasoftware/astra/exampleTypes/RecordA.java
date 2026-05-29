package org.alfasoftware.astra.exampleTypes;

public record RecordA(int value, String name) {

  public RecordA {
    if (value < 0) {
      throw new IllegalArgumentException("value must not be negative");
    }
  }

  public String describe() {
    return name + "=" + value;
  }
}
