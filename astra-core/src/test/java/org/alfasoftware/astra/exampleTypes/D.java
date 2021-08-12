package org.alfasoftware.astra.exampleTypes;

@SuppressWarnings("unused")
public class D {

  public D(){}

  public D(B b){}

  public void one() {

  }

  public void two() {

  }

  public static void staticOne() {

  }

  public static void staticTwo() {

  }

  public D third() {
    return new D();
  }
  
  public C getC() {
  	return new C();
  }
}
