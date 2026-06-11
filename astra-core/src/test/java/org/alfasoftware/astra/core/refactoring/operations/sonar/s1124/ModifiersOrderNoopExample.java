package org.alfasoftware.astra.core.refactoring.operations.sonar.s1124;

public class ModifiersOrderNoopExample {

  // public static — already correct
  public static void publicStatic() {}

  // public static final — already correct
  public static final int PUBLIC_STATIC_FINAL = 1;

  // private static final — already correct
  private static final String PRIVATE_STATIC_FINAL = "x";

  // static synchronized — already correct
  static synchronized void staticSynchronized() {}

  // transient volatile — already correct (transient < volatile in JLS order)
  transient volatile int transientVolatile;

  // public final — already correct
  public final void publicFinal() {}

  // protected static — already correct
  protected static void protectedStatic() {}

  // single modifier: cannot be out of order
  public void singlePublic() {}

  // single modifier
  static void singleStatic() {}

  // single modifier
  final int singleFinal = 0;

  // no modifier: nothing to reorder
  int noModifier;

  // nested class: public abstract static — already correct
  public abstract static class CorrectNestedClass {

    // static synchronized inside nested class — already correct
    static synchronized void nestedStaticSync() {}

    // public static final — already correct
    public static final String NESTED_CONST = "y";
  }
}
