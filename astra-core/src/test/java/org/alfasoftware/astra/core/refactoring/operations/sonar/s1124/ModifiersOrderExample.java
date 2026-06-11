package org.alfasoftware.astra.core.refactoring.operations.sonar.s1124;

public class ModifiersOrderExample {

  // static before public — wrong order on a method
  static public void staticBeforePublic() {}

  // final before static — wrong order on a field
  final static int FINAL_STATIC = 1;

  // all three wrong: final before public before static
  final public static String ALL_THREE_WRONG = "x";

  // synchronized before static — wrong order on a method
  synchronized static void synchronizedStatic() {}

  // native before static — wrong order (native method has no body)
  native static void nativeStatic();

  // volatile before transient — wrong order on a field
  volatile transient int volatileTransient;

  // nested class: abstract static public — wrong order (public should lead, then abstract, then static)
  abstract static public class WrongNestedClass {

    // synchronized before static inside nested class — wrong order
    synchronized static void nestedSyncStatic() {}
  }
}
