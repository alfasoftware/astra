package org.alfasoftware.astra.core.matchers;

class ExampleUsedClass {

  String baseMethod(){
    return "anything";
  }

  String baseMethod(String arg1){
    return "anything" + arg1;
  }

  String methodWithArgs(Long arg1) {
    return String.valueOf(arg1);
  }

  String methodWithArgs(String... arg1) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < arg1.length; i++) {
      result.append(arg1[i]);
    }
    return result.toString();
  }

  static String staticMethod(char arg1, Integer arg2) {
    return String.copyValueOf(String.valueOf(arg1).toCharArray(), 0, arg2);
  }
}

