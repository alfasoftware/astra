package org.alfasoftware.astra.core.refactoring.javapattern.performance;

import org.alfasoftware.astra.core.refactoring.javapattern.performance.lib.LegacyCache;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

/**
 * Benchmark pattern: migrates {@link LegacyCache#lookup(Object)} to {@link LegacyCache#get(Object)},
 * capturing the type arguments of the cache.
 */
class LegacyCacheLookupPattern<K, V> {

  @JavaPattern
  V pattern(LegacyCache<K, V> cache, K key) {
    return cache.lookup(key);
  }

  @JavaPatternReplacement
  V replacement(LegacyCache<K, V> cache, K key) {
    return cache.get(key);
  }
}
