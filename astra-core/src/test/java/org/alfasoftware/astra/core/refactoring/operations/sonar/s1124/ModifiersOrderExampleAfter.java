package org.alfasoftware.astra.core.refactoring.operations.sonar.s1124;

public class ModifiersOrderExampleAfter {

  // static before public — wrong order on a method
  public static void staticBeforePublic() {}

  // final before static — wrong order on a field
  static final int FINAL_STATIC = 1;

  // all three wrong: final before public before static
  public static final String ALL_THREE_WRONG = "x";

  // synchronized before static — wrong order on a method
  static synchronized void synchronizedStatic() {}

  // native before static — wrong order (native method has no body)
  static native void nativeStatic();

  // volatile before transient — wrong order on a field
  transient volatile int volatileTransient;

  // nested class: abstract static public — wrong order (public should lead, then abstract, then static)
  public abstract static class WrongNestedClass {

    // synchronized before static inside nested class — wrong order
    static synchronized void nestedSyncStatic() {}
  }
}
