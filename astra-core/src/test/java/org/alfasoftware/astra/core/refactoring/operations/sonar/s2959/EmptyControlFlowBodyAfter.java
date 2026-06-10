package org.alfasoftware.astra.core.refactoring.operations.sonar.s2959;

import java.util.List;

public class EmptyControlFlowBodyAfter {

  void emptyWhileBody(List<Object> list) {
    while (!list.isEmpty()) ;
  }

  void emptyForBody() {
    for (int i = 0; i < 10; i++) ;
  }

  void emptyEnhancedForBody(List<Object> list) {
    for (Object o : list) ;
  }

  void emptyIfBody() {
    if (true) ;
  }

  void doSomething() {}
}
