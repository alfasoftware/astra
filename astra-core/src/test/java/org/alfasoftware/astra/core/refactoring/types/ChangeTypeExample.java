package org.alfasoftware.astra.core.refactoring.types;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.A;

/**
 * {@link A}
 */
public class ChangeTypeExample extends A {
	A[] aArr = new A[0];
	A a = new A();

  @SuppressWarnings({ "unused", "rawtypes" })
  public <T extends A> T generic(T n, List<? super A> in) {
    org.alfasoftware.astra.exampleTypes.A.staticOne();
		A.staticOne();
		this.<A>generic(null, null);
		Class clazz = A.class;
		return n;
	}

  public A parameter(A a) {
    return a;
  }
}
