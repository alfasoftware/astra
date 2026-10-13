package org.alfasoftware.astra.core.refactoring.javapattern.prefiltering;

import java.util.Arrays;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

/**
 * Requires "arraycopy" only. The array parameter captures every argument from its position onwards,
 * so the invocations in the later arguments are never compared.
 */
class VarargsPattern {

  @JavaPattern
  void pattern(Object[] values) {
    System.arraycopy(values, 0, values, Integer.valueOf(1).intValue(), Integer.valueOf(2).intValue());
  }

  @JavaPatternReplacement
  void replacement(Object[] values) {
    Arrays.fill(values, null);
  }
}
