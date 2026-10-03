package org.alfasoftware.astra.core.refactoring.javapattern.performance.lib;

/**
 * Declares a {@link #lookup(String)} method with the same name as {@link LegacyCache#lookup(Object)},
 * so that the performance benchmark contains invocations which share a method name with a java pattern,
 * but must not be matched by it.
 */
public class ServiceRegistry {

  public Object lookup(String name) {
    return name;
  }
}
