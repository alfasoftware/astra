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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + description.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj){
    if(obj == this){
      return true;  
    }
    boolean isEqual = true;
    if (obj == null || getClass() != obj.getClass()) {
      isEqual = false;
    }
    DescribedPredicate<?> other = (DescribedPredicate<?>) obj;
    if(other != null && !description.equals(other.description)){
      isEqual = false;
    }
    return isEqual;
  }
}

