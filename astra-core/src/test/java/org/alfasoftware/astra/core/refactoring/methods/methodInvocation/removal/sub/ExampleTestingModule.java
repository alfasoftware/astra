package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal.sub;

public class ExampleTestingModule {

  public static Object module(@SuppressWarnings("unused") Object test) {
    return new Object() {
      @Override
      protected Object clone() throws CloneNotSupportedException {
        return super.clone();
      }
    };
  }
}

