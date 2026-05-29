package org.alfasoftware.astra.core.refactoring.types;

import org.alfasoftware.astra.exampleTypes.RecordB;

/**
 * {@link RecordB}
 * {@link org.alfasoftware.astra.exampleTypes.RecordB}
 */
public class TypeReferenceRecordExampleAfter {

  RecordB field = new RecordB(1, "a");

  @SuppressWarnings("unused")
  public RecordB doFoo(RecordB param) {
    Object o = param;
    if (o instanceof RecordB r) {
      return r;
    }
    return new RecordB(2, "b");
  }
}
