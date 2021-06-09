package org.alfasoftware.astra.core.refactoring.imports;

import static org.alfasoftware.astra.core.refactoring.imports.ExampleTypeSamePackage.OtherInnerClass.otherInnerClassStaticMethod;
import static org.alfasoftware.astra.core.refactoring.imports.ExampleTypeSamePackage.staticMethodOtherType;


/**
 * Example class using a static method from a class in the same package
 * Both imports are expected to remain and be unchanged
 */
class StaticImportSamePackageExampleAfter {
  
  
  void someMethod() {
    staticMethodOtherType();
    otherInnerClassStaticMethod();
  }
}