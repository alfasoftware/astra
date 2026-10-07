package org.alfasoftware.astra.exampleTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * A cache with a legacy API ({@link #lookup(Object)} and {@link #store(Object, Object)}), which the
 * performance benchmark's java patterns migrate to {@link #get(Object)} and {@link #put(Object, Object)}.
 */
public class LegacyCache<K, V> {

  private final Map<K, V> values = new HashMap<>();

  /**
   * Legacy accessor, replaced by {@link #get(Object)}.
   */
  public V lookup(K key) {
    return values.get(key);
  }

  /**
   * Legacy mutator, replaced by {@link #put(Object, Object)}.
   */
  public void store(K key, V value) {
    values.put(key, value);
  }

  public V get(K key) {
    return values.get(key);
  }

  public void put(K key, V value) {
    values.put(key, value);
  }
}
