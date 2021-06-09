package org.alfasoftware.astra.core.refactoring.imports;

import static org.alfasoftware.astra.core.refactoring.imports.StaticImportSameTypeInnerClassExampleAfter.InnerClass.innerClassStaticMethod;

/**
 * Example class with a static import for a method in the enclosed class
 * The static import is expected to remain when the outer class is refactored
 */
class StaticImportSameTypeInnerClassExampleAfter {
  
  
  void someMethod() {
    innerClassStaticMethod();
  }
  
  
  static class InnerClass {
    static void innerClassStaticMethod() {
    }
  }
}