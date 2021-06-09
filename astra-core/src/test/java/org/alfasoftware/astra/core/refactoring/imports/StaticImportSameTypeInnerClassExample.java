package org.alfasoftware.astra.core.refactoring.imports;

import static org.alfasoftware.astra.core.refactoring.imports.StaticImportSameTypeInnerClassExample.InnerClass.innerClassStaticMethod;

/**
 * Example class with a static import for a method in the enclosed class
 * The static import is expected to remain when the outer class is refactored
 */
class StaticImportSameTypeInnerClassExample {
  
  
  void someMethod() {
    innerClassStaticMethod();
  }
  
  
  static class InnerClass {
    static void innerClassStaticMethod() {
    }
  }
}