package org.alfasoftware.astra.core.refactoring.javapattern.prefiltering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;

/**
 * Has two patterns. The first requires "StringBuilder" and "reverse", the second only "ArrayList".
 */
class ConstructorsPattern<E> {

  @JavaPattern
  Object reversed(String string) {
    return new StringBuilder(string).reverse();
  }

  @JavaPattern
  Object copied(Collection<E> collection) {
    return new ArrayList<E>(collection);
  }

  @JavaPatternReplacement
  Object replacement(String string, Collection<E> collection) {
    return List.of(string, collection);
  }
}
