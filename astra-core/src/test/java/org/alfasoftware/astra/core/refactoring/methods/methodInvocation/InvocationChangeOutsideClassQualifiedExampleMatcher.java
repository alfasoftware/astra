package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;
import org.alfasoftware.astra.exampleTypes.A;


public class InvocationChangeOutsideClassQualifiedExampleMatcher {

  @JavaPattern
  void matcher(A a){
    a.notify();
  }

  @JavaPatternReplacement
  void replacement(A a) {
    a.getClass();
  }
}
