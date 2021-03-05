package org.alfasoftware.astra.core.refactoring.interfaces.inlining;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.A;

public interface InlineInterfaceExampleAfter extends OtherInterface {

  List<A> getGeneric();

  /**
   * Some Javadoc
   */
  void somethingWithJavaDoc();

  List<A[]> getGenericArray();

  void varargs(A... variableName);
}
