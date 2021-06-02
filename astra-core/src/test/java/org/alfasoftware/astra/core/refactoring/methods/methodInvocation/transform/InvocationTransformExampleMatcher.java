package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.transform;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

class InvocationTransformExampleMatcher {

  @JavaPattern
  ReturnsObject matcher(ReturnsObject toReturn){
    return (ReturnsObject) toReturn.get();
  }

  @JavaPatternReplacement
  ReturnsObject replacement(ReturnsObject toReturn){
    return toReturn;
  }
}
