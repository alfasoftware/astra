package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;


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
