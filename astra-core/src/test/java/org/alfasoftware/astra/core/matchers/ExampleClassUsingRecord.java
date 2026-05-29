package org.alfasoftware.astra.core.matchers;

import org.alfasoftware.astra.exampleTypes.RecordA;

public class ExampleClassUsingRecord {

  @SuppressWarnings("unused")
  public void use() {
    RecordA r = new RecordA(1, "a");
    int v = r.value();
    String n = r.name();
    String d = r.describe();
  }
}
