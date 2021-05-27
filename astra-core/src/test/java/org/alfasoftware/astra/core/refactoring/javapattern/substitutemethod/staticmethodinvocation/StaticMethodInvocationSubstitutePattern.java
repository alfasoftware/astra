package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod.staticmethodinvocation;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.Substitute;

import java.util.List;
import java.util.Map;


public abstract class StaticMethodInvocationSubstitutePattern {

  @Substitute
  abstract List<String> someMethod(String string);

  @JavaPattern
  void patternWithParameters(String key, Map<String, List<String>> map, String parameter){
    map.put(key, someMethod(parameter));
  }

  @JavaPatternReplacement
  void patternReplacement(String key, Map<String, List<String>> map, String parameter){
    map.put(parameter, someMethod(key));
  }
}
