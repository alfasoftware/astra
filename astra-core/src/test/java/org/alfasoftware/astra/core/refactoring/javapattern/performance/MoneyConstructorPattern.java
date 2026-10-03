package org.alfasoftware.astra.core.refactoring.javapattern.performance;

import org.alfasoftware.astra.core.refactoring.javapattern.performance.lib.Money;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

/**
 * Benchmark pattern: replaces the legacy {@link Money} constructor with the {@link Money#of(long, String)} factory.
 */
class MoneyConstructorPattern {

  @JavaPattern
  Money pattern(long amount, String currency) {
    return new Money(amount, currency);
  }

  @JavaPatternReplacement
  Money replacement(long amount, String currency) {
    return Money.of(amount, currency);
  }
}
