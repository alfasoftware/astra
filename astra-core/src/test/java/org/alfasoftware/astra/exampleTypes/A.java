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

	public B first() {
	  return new B();
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
}
