package org.alfasoftware.astra.core.refactoring.types;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.B;

/**
 * {@link B}
 */
public class ChangeTypeAndRenameExampleAfter extends B {
  B[] aArr = new B[0];
  B b = new B();

  @SuppressWarnings("unused")
  public <T extends B> T generic(T n, List<? super B> in) {
    B.staticOne();
    this.<B>generic(null, null);
    return n;
  }

  public B parameter(B b) {
    return b;
  }
}
