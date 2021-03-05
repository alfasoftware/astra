package org.alfasoftware.astra.core.refactoring.methods.constructortostaticmethod;

import java.time.LocalDate;
import java.util.Date;

import org.alfasoftware.astra.exampleTypes.A;

@SuppressWarnings({ "unused", "deprecation" })
public class ConstructorToStaticMethodWithArgsExampleAfter {

  private void doConstruct() {
    new Date();
    LocalDate.of(2020, 1, 1);
    A.deprecated();
  }
}

