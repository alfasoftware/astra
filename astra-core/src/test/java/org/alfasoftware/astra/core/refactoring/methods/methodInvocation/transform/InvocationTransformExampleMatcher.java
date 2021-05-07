package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.transform;

import org.alfasoftware.astra.core.refactoring.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.javapattern.JavaPatternReplacement;

public class InvocationTransformExampleMatcher {

  @JavaPattern
  ReturnsObject matcher(ReturnsObject returnsObject){
    return (ReturnsObject) returnsObject.get();
  }

  @JavaPatternReplacement
  ReturnsObject replacement(ReturnsObject returnsObject){
    return returnsObject;
  }
}
