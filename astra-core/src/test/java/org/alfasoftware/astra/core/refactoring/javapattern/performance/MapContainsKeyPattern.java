package org.alfasoftware.astra.core.refactoring.javapattern.performance;

import java.util.Map;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

/**
 * Benchmark pattern: replaces {@code map.keySet().contains(key)} with {@link Map#containsKey(Object)},
 * capturing the type arguments of the map.
 */
class MapContainsKeyPattern<K, V> {

  @JavaPattern
  boolean pattern(Map<K, V> map, K key) {
    return map.keySet().contains(key);
  }

  @JavaPatternReplacement
  boolean replacement(Map<K, V> map, K key) {
    return map.containsKey(key);
  }
}
