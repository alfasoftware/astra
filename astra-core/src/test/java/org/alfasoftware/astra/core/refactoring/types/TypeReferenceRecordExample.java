package org.alfasoftware.astra.core.refactoring.types;

import org.alfasoftware.astra.exampleTypes.RecordA;

/**
 * {@link RecordA}
 * {@link org.alfasoftware.astra.exampleTypes.RecordA}
 */
public class TypeReferenceRecordExample {

  RecordA field = new RecordA(1, "a");

  @SuppressWarnings("unused")
  public RecordA doFoo(RecordA param) {
    Object o = param;
    if (o instanceof RecordA r) {
      return r;
    }
    return new RecordA(2, "b");
  }
}
