package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.Substitute;


public abstract class SubstituteMethodExamplePattern {

  @Substitute
  abstract boolean someMethod(String string, Integer integer, Object[] objects);

  @JavaPattern
  void patternWithParameters(String string, Integer integer, Object[] objects){
    someMethod(string, integer, objects);
  }

  @JavaPatternReplacement
  void patternReplacement(String string, Integer integer, Object[] objects){
    BuilderExample
        .otherMethod(integer, string) // swap order
        .withArguments(objects); // capture varargs
  }
}
