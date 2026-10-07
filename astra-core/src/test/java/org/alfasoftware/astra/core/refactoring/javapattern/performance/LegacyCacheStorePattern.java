package org.alfasoftware.astra.core.refactoring.javapattern.performance;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.Substitute;
import org.alfasoftware.astra.exampleTypes.LegacyCache;

/**
 * Benchmark pattern: migrates {@link LegacyCache#store(Object, Object)} to {@link LegacyCache#put(Object, Object)}
 * where the stored value is produced by a static conversion method, captured with a {@link Substitute} method.
 */
abstract class LegacyCacheStorePattern {

  @Substitute
  abstract String convert(Object value);

  @JavaPattern
  void pattern(LegacyCache<String, String> cache, String key, Object value) {
    cache.store(key, convert(value));
  }

  @JavaPatternReplacement
  void replacement(LegacyCache<String, String> cache, String key, Object value) {
    cache.put(key, convert(value));
  }
}
