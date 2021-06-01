package org.alfasoftware.astra.core.refactoring.methods.constructortobuilder;

import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.builder.BuiltType;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

class ConstructorToBuilderInnerClassBuilderExampleMatcherWithThreeParameters {

  @JavaPattern
  void pattern(Object parameter1, long parameter2, String parameter3){
    new BuiltType(parameter1, parameter2, parameter3);
  }

  @JavaPatternReplacement
  void patternReplacement(Object parameter1, long parameter2, String parameter3){
    BuiltType.builderFor(parameter1).withTwoAndThree(parameter2, parameter3);
  }

}
