package org.alfasoftware.astra.core.refactoring.javapattern.substitutemethod.staticmethodinvocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

class StaticMethodInvocation {

  void someMethodWithCodeToMatch(){
    String key = "key";
    String parameter = "parameter";
    HashMap<String, List<String>> stringListHashMap = new HashMap<>();
    stringListHashMap.put(key, Collections.singletonList(parameter));
  }
}
