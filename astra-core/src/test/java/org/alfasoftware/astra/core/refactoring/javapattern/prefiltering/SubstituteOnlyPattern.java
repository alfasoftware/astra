package org.alfasoftware.astra.core.refactoring.javapattern.prefiltering;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.Substitute;

/**
 * Requires no identifiers at all: the only method invoked is a substitute, and its argument is a parameter.
 */
abstract class SubstituteOnlyPattern {

  @Substitute
  abstract String convert(Object value);

  @JavaPattern
  String pattern(Object value) {
    return convert(value);
  }

  @JavaPatternReplacement
  String replacement(Object value) {
    return String.valueOf(convert(value));
  }
}
