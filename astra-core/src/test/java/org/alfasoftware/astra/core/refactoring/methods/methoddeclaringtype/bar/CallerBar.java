package org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.bar;

import org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.foobar.FooBar;

public class CallerBar {

  private final FooBar fooBar;

  public CallerBar(FooBar fooBar) {
    this.fooBar = fooBar;
  }

  @SuppressWarnings("unused")
  private void doThing() {
    fooBar.doBar();
  }
}

