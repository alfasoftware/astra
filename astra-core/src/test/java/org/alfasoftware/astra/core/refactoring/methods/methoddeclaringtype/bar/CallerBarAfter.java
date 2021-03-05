package org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.bar;

public class CallerBarAfter {

  private final Bar fooBar;

  public CallerBarAfter(Bar fooBar) {
    this.fooBar = fooBar;
  }

  @SuppressWarnings("unused")
  private void doThing() {
    fooBar.doBar();
  }
}

