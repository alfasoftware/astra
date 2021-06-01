package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod;

class SubstituteMethodExampleAfter {

  void someMethodWithCodeToMatch(){
    String someText = "someText";
    Integer anInteger = 1;
    String arg1 = "arg1";
    String arg2 = "arg2";

    BuilderExample.otherMethod(anInteger, someText).withArguments(arg1, arg2);
    BuilderExample.otherMethod(anInteger, someText).withArguments(arg1);
    aVoidMethod(someText, anInteger, arg1, arg2);
  }

  boolean aBooleanMethod(String string, Integer integer, Object ... objects){
    return true;
  }

  void aVoidMethod(String string, Integer integer, Object ... objects){

  }
}
