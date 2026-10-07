package org.alfasoftware.astra.core.refactoring.javapattern.prefiltering;

import java.util.List;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.Substitute;

/**
 * Requires "add" and "trim", but not "convert", which captures any method with a matching signature.
 */
abstract class SubstituteMethodPattern {

  @Substitute
  abstract String convert(Object value);

  @JavaPattern
  void pattern(List<String> list, Object value) {
    list.add(convert(value).trim());
  }

  @JavaPatternReplacement
  void replacement(List<String> list, Object value) {
    list.add(convert(value).strip());
  }
}
