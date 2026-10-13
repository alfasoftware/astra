package org.alfasoftware.astra.exampleTypes;

/**
 * A value type whose legacy constructor the performance benchmark's java patterns replace
 * with the {@link #of(long, String)} factory method.
 */
public class Money {

  private final long amount;
  private final String currency;

  /**
   * Legacy constructor, replaced by {@link #of(long, String)}.
   */
  public Money(long amount, String currency) {
    this.amount = amount;
    this.currency = currency;
  }

  public static Money of(long amount, String currency) {
    return new Money(amount, currency);
  }

  @Override
  public String toString() {
    return amount + " " + currency;
  }
}
