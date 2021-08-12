package org.alfasoftware.astra.exampleTypes;

public class C {

  public static final String STRING_VALUE_C = "static string: C";

	public void one() {

	}

	public void two() {

	}

	public static void staticOne() {

	}

	public static void staticTwo() {

	}

  public C third() {
    return new C();
  }
  
  public B getB() {
    return new B();
  }
  
  public D getD() {
    return new D();
  }
}
