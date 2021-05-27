package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod;

public class MethodBuilder {

  static RequiresArguments otherMethod(Integer integer, String string){
    return new RequiresArguments(integer, string);
  }

  static class RequiresArguments {
    public RequiresArguments(Integer integer, String string) {
    }

    void withArguments(Object ... objects){

    }
  }
}
