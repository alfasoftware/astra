package org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.foobar;

import org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.bar.Bar;

public class CallerFooBarAfter {

  private final Bar fooBar;

  public CallerFooBarAfter(Bar fooBar) {
    this.fooBar = fooBar;
  }

  @SuppressWarnings("unused")
  private void doThing() {
    fooBar.doBar();
  }
}

