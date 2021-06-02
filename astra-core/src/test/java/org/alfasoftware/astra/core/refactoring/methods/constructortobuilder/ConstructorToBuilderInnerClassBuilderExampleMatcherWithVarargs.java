package org.alfasoftware.astra.core.refactoring.methods.constructortobuilder;

import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.builder.BuiltType;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

class ConstructorToBuilderInnerClassBuilderExampleMatcherWithVarargs {

  @JavaPattern
  void pattern(String parameter1, Object[] parameterArray){
    new BuiltType(parameter1, parameterArray);
  }

  @JavaPatternReplacement
  void patternReplacement(String parameter1, Object[] parameterArray){
    BuiltType.builderForKey(parameter1).withValues(parameterArray).build();
  }

}
