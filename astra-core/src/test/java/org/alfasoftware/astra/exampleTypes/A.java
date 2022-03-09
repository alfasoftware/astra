package org.alfasoftware.astra.exampleTypes;

@SuppressWarnings("unused")
public class A implements Fooable {

  public static final String STRING_VALUE_A = "static string: A";

  public void one() {

  }

  public void two() {

  }

  public static void staticOne() {

  }

  public static void staticTwo() {

  }

  public static void staticThree() {

  }


  public static void staticFour(String arg) {

  }

  public B first() {
    return new B();
  }

  public A getA() {
    return new A();
  }

  public B getB() {
    return new B();
  }

  public C getC() {
    return new C();
  }

  public D getD() {
    return new D();
  }

  public void overloaded(Object one) {

  }

  public void overloaded(Object one, String two) {

  }

  @Deprecated
  public static void deprecated() {

  }

  @Override
  public void doFoo() {

  }

  public static Object[] $(Object... objects) {
    return objects;
  }


  public static @interface InnerAnnotationA {

  }
}
