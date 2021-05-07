package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.core.refactoring.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.javapattern.JavaPatternReplacement;
import org.alfasoftware.astra.exampleTypes.A;


public class InvocationChangeInheritedFromSuperclassMatcher {

  @JavaPattern
  void matcher(Object a){
    a.notify();
  }

  @JavaPatternReplacement
  void replacement(Object a) {
    a.getClass();
  }
}
