package org.alfasoftware.astra.core.refactoring.types;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.B;

/**
 * {@link B}
 */
public class ChangeTypeExampleAfter extends B {
	B[] aArr = new B[0];
	B a = new B();

  @SuppressWarnings({ "unused", "rawtypes" })
  public <T extends B> T generic(T n, List<? super B> in) {
    org.alfasoftware.astra.exampleTypes.B.staticOne();
		B.staticOne();
		this.<B>generic(null, null);
		Class clazz = B.class;
		return n;
	}

  public B parameter(B a) {
    return a;
  }
}
