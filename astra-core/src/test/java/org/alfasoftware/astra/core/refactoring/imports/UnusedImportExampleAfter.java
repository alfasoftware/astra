package org.alfasoftware.astra.core.refactoring.imports;

import static org.alfasoftware.astra.exampleTypes.A.STRING_VALUE_A;

import java.lang.Thread.State;

import org.alfasoftware.astra.core.refactoring.imports.ExampleTypeSamePackage.OtherInnerClass;
import org.alfasoftware.astra.core.refactoring.imports.UnusedImportExampleAfter.InnerClassExample;
import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.C;
import org.alfasoftware.astra.exampleTypes.D;
import org.junit.experimental.categories.Category;

@SuppressWarnings("unused")
@Category(InnerClassExample.class)
public class UnusedImportExampleAfter {

  A a = new A();
  OtherInnerClass inner = new OtherInnerClass();
  State baseState = State.RUNNABLE;

  /**
   * {@link C}
   * {@link D#one()}
   */
  private String foo() {
    return STRING_VALUE_A;
  }

  static class InnerClassExample {

  }
}

