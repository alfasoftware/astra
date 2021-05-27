package org.alfasoftware.astra.core.refactoring.javapattern.parametermatching;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

import java.util.Map;


public class ParameterMatchingPattern<K,V> {

  @JavaPattern
  void patternWithParameters(String string, Map<K,V> map, K key){
    map.get(key).toString().equals(string);
  }

  @JavaPatternReplacement
  void patternReplacement(String string, Map<K,V> map, K key){
    map.computeIfAbsent(key, (K k) -> { // Test that we have captured "map"
      ((V) string).getClass(); //Test that we have captured V
      string.toString(); // Test that we have captured "string"
      key.toString(); // Test that we have captured "key"
      return (V) k;
    });
  }
}
