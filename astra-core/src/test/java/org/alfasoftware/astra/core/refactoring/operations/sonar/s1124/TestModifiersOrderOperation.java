package org.alfasoftware.astra.core.refactoring.operations.sonar.s1124;

import java.util.Set;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.junit.Test;

public class TestModifiersOrderOperation extends AbstractRefactorTest {

  private static final Set<ModifiersOrderOperation> OPERATION =
      Set.of(new ModifiersOrderOperation());

  /**
   * All cases where keyword modifiers are out of the JLS canonical order and must be reordered:
   * <ul>
   *   <li>{@code static public}           &rarr; {@code public static} (method)</li>
   *   <li>{@code final static}            &rarr; {@code static final} (field)</li>
   *   <li>{@code final public static}     &rarr; {@code public static final} (field)</li>
   *   <li>{@code synchronized static}     &rarr; {@code static synchronized} (method)</li>
   *   <li>{@code abstract static public}  &rarr; {@code public abstract static} (nested class)</li>
   *   <li>{@code volatile transient}      &rarr; {@code transient volatile} (field)</li>
   *   <li>{@code native static}           &rarr; {@code static native} (method)</li>
   *   <li>{@code protected abstract} + {@code static synchronized} inside nested class</li>
   * </ul>
   */
  @Test
  public void testModifiersReordered() {
    assertRefactor(ModifiersOrderExample.class, OPERATION);
  }

  /**
   * Cases where modifier order is already correct — the operation must make no change:
   * <ul>
   *   <li>{@code public static final} field</li>
   *   <li>{@code public abstract} method</li>
   *   <li>{@code private static final} field</li>
   *   <li>{@code protected static} method</li>
   *   <li>{@code static synchronized} method</li>
   *   <li>{@code transient volatile} field</li>
   *   <li>Single-modifier declarations</li>
   * </ul>
   */
  @Test
  public void testNoopCases() {
    assertRefactor(ModifiersOrderNoopExample.class, OPERATION);
  }
}
