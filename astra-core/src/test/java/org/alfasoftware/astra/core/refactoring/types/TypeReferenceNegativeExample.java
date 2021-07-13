package org.alfasoftware.astra.core.refactoring.types;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.C;
import org.alfasoftware.astra.exampleTypes.a.A;

/**
 * Nothing should be changed in this class
 * 
 * {@link A}
 * {@link C}
 * {@link org.alfasoftware.astra.exampleTypes.a.A}
 * {@link org.alfasoftware.astra.exampleTypes.C}
 */
public class TypeReferenceNegativeExample extends A {
  A field = new A();
  C field2 = new C();
  A[] fieldArray = new A[0];
  C[] fieldArray2 = new C[0];

  @SuppressWarnings({ "unused", "rawtypes" })
  public <T extends A> T generic(T n, List<? super A> in) {
    org.alfasoftware.astra.exampleTypes.a.A.staticOne();
    org.alfasoftware.astra.exampleTypes.C.staticOne();
    A.staticOne();
    C.staticOne();
    this.<A>generic(null, null);
    Class clazz = A.class;
    Class clazz2 = C.class;
    return n;
  }

  public A parameter(A param) {
    return param;
  }
  
  public C parameter2(C param) {
    return param;
  }
}
