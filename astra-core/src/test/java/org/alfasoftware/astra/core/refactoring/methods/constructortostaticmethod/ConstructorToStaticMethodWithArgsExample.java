package org.alfasoftware.astra.core.refactoring.methods.constructortostaticmethod;

import java.util.Date;

import org.alfasoftware.astra.exampleTypes.A;

@SuppressWarnings({ "unused", "deprecation" })
public class ConstructorToStaticMethodWithArgsExample {

  private void doConstruct() {
    new Date();
    new Date(2020, 1, 1);
    A.deprecated();
  }
}

