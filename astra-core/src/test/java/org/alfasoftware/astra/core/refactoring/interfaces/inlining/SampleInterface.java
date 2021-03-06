package org.alfasoftware.astra.core.refactoring.interfaces.inlining;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.A;


public interface SampleInterface {

  /**
   * Some Javadoc
   */
  void somethingWithJavaDoc();

  List<A> getGeneric();

  List<A[]> getGenericArray();

  void varargs(A... variableName);
}
