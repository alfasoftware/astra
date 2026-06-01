package org.alfasoftware.astra.core.refactoring.operations.sonar.s5361;

/** Dummy class with a replaceAll method that is NOT java.lang.String, used in no-op tests. */
class ReplaceAllProvider {
  String replaceAll(String pattern, String replacement) {
    return pattern + replacement;
  }
}
