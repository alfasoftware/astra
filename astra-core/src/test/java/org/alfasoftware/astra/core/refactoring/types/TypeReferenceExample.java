package org.alfasoftware.astra.core.refactoring.types;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.A;

/**
 * {@link A}
 * {@link org.alfasoftware.astra.exampleTypes.A}
 */
public class TypeReferenceExample extends A {
  A field = new A();
	A[] fieldArray = new A[0];

  @SuppressWarnings({ "unused", "rawtypes" })
  public <T extends A> T generic(T n, List<? super A> in) {
    org.alfasoftware.astra.exampleTypes.A.staticOne();
		A.staticOne();
		this.<A>generic(null, null);
		Class clazz = A.class;
		return n;
	}

  public A parameter(A param) {
    return param;
  }
}
