package org.alfasoftware.astra.core.utils;

import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Constant class for Astra specific {@link CompilationUnit} properties.
 */
public final class CompilationUnitProperty {
  private CompilationUnitProperty() {
    // Constant class, do not instantiate.
  }

  /**
   * The absolute path of the source file used to generate the respective {@link CompilationUnit}.
   * Return Type: {@link String}.
   */
  public static final String ABSOLUTE_PATH = "absolute_path";
}
