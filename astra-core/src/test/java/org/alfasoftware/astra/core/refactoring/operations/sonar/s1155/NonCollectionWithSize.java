package org.alfasoftware.astra.core.refactoring.operations.sonar.s1155;

/** Helper type used by tests — has {@code size()} but does NOT implement {@code java.util.Collection}. */
class NonCollectionWithSize {
  public int size() { return 0; }
}
