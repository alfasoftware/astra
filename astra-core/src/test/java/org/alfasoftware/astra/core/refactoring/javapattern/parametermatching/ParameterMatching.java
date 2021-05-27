package org.alfasoftware.astra.core.refactoring.javapattern.parametermatching;

import java.util.HashMap;

public class ParameterMatching {

  void someMethodWithCodeToMatch(){
    HashMap<Integer, Object> map = new HashMap<>();
    Integer anIntegerKey = 1;
    String someText = "SomeText";

    map.get(anIntegerKey).toString().equals(someText);
  }
}
