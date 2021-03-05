package org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.foobar;

public class CallerFooBar {

  private final FooBar fooBar;

  public CallerFooBar(FooBar fooBar) {
    this.fooBar = fooBar;
  }

  @SuppressWarnings("unused")
  private void doThing() {
    fooBar.doBar();
  }
}

