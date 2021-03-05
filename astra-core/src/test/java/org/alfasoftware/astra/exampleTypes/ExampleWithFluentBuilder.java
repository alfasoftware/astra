package org.alfasoftware.astra.exampleTypes;

import java.util.Set;

@SuppressWarnings("unused")
public class ExampleWithFluentBuilder {

  public static Builder withCode(String c) {
    return new Builder(c);
  }


  public static class Builder {

    public Builder(String c) {
    }

    public Builder withA() {
      return this;
    }

    public Builder withB(String s) {
      return this;
    }
    public Builder withBs(String... s) {
      return this;
    }
    public Builder withBs(Set<String> s) {
      return this;
    }

    public Builder withC() {
      return this;
    }

    public ExampleWithFluentBuilder build() {
      return new ExampleWithFluentBuilder();
    }
  }


  public static class AcceptorOfClass {
    public AcceptorOfClass(ExampleWithFluentBuilder b) {
    }
  }
}