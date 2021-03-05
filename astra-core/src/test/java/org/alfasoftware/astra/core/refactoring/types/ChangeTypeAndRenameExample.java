package org.alfasoftware.astra.core.refactoring.types;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.A;

/**
 * {@link A}
 */
public class ChangeTypeAndRenameExample extends A {
  A[] aArr = new A[0];
  A a = new A();

  @SuppressWarnings("unused")
  public <T extends A> T generic(T n, List<? super A> in) {
    A.staticOne();
    this.<A>generic(null, null);
    return n;
  }

  public A parameter(A a) {
    return a;
  }
}
