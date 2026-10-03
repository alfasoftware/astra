package org.alfasoftware.astra.core.refactoring.javapattern.prefiltering;

import java.util.Map;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

/**
 * Requires "get", "toString" and "equals", but not the names of its parameters.
 */
class MethodNamesPattern<K, V> {

  @JavaPattern
  boolean pattern(Map<K, V> map, K key, String string) {
    return map.get(key).toString().equals(string);
  }

  @JavaPatternReplacement
  boolean replacement(Map<K, V> map, K key, String string) {
    return string.equals(String.valueOf(map.get(key)));
  }
}
