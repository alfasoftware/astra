package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod;

class BuilderExample {

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
