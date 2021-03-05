package org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.foo;

import org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.foobar.FooBar;

public class CallerFoo {

  private final FooBar fooBar;

  public CallerFoo(FooBar fooBar) {
    this.fooBar = fooBar;
  }

  @SuppressWarnings("unused")
  private void doThing() {
    fooBar.doFoo();
  }
}

