package org.alfasoftware.astra.exampleTypes;

public class B {

  public void one() {

  }

  public void two() {

  }

  public static void staticOne() {

  }

  public static void staticTwo() {

  }

  public static void staticThree(@SuppressWarnings("unused") String arg) {

  }

  public C second() {
    return new C();
  }

  public A getA() {
  	return new A();
  }

  public C getC() {
  	return new C();
  }

  public static @interface InnerAnnotationB {

  }
}
