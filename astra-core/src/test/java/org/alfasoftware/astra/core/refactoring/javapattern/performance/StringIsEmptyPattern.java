package org.alfasoftware.astra.core.refactoring.javapattern.performance;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

/**
 * Benchmark pattern: replaces two idioms for checking for an empty String with {@link String#isEmpty()}.
 * The method names in these patterns are common, so most files contain them.
 */
class StringIsEmptyPattern {

  @JavaPattern
  boolean lengthIsZero(String string) {
    return string.length() == 0;
  }

  @JavaPattern
  boolean equalsEmptyString(String string) {
    return string.equals("");
  }

  @JavaPatternReplacement
  boolean replacement(String string) {
    return string.isEmpty();
  }
}
