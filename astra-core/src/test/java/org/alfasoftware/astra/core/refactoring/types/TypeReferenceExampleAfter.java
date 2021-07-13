package org.alfasoftware.astra.core.refactoring.types;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.B;
import org.alfasoftware.astra.exampleTypes.C;

/**
 * {@link B}
 * {@link C}
 * {@link org.alfasoftware.astra.exampleTypes.B}
 * {@link org.alfasoftware.astra.exampleTypes.C}
 */
public class TypeReferenceExampleAfter extends B {
  B field = new B();
	B[] fieldArray = new B[0];

  @SuppressWarnings({ "unused", "rawtypes" })
  public <T extends B> T generic(T n, List<? super B> in) {
    org.alfasoftware.astra.exampleTypes.B.staticOne();
		B.staticOne();
		this.<B>generic(null, null);
		Class clazz = B.class;
		return n;
	}

  public B parameter(B param) {
    return param;
  }
}
