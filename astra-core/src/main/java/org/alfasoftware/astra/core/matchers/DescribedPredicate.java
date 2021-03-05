package org.alfasoftware.astra.core.matchers;

import java.util.function.Predicate;

/**
 * Extension of Predicate which also provides a human-readable description.
 * @param <T> the type of the input to the predicate.
 */
public class DescribedPredicate<T> implements Predicate<T> {

  private final String description;
  private final Predicate<T> predicate;

  private DescribedPredicate(String description, Predicate<T> predicate) {
    super();
    this.description = description;
    this.predicate = predicate;
  }

  public static <T> DescribedPredicate<T> describedPredicate(String description, Predicate<T> predicate) {
    return new DescribedPredicate<>(description, predicate);
  }

  @Override
  public boolean test(T t) {
    return predicate.test(t);
  }

  @Override
  public String toString() {
    return description;
  }
}

