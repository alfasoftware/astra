package org.alfasoftware.astra.core.refactoring.javapattern.parametermatching;

import java.util.HashMap;

public class ParameterMatchingAfter {

  void someMethodWithCodeToMatch(){
    HashMap<Integer, Object> map = new HashMap<>();
    Integer anIntegerKey = 1;
    String someText = "SomeText";

    map.computeIfAbsent(anIntegerKey, (Integer k) -> {
      ((Object) someText).getClass();
      someText.toString();
      anIntegerKey.toString();
      return (Object) k;
    });
  }
}
