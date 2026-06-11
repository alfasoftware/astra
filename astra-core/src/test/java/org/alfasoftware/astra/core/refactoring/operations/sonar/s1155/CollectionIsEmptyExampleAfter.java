package org.alfasoftware.astra.core.refactoring.operations.sonar.s1155;

import java.util.ArrayList;
import java.util.List;

public class CollectionIsEmptyExampleAfter {

  private final List<String> list = new ArrayList<>();

  void sizeOnLeft() {
    boolean a = list.isEmpty();
    boolean b = ! list.isEmpty();
    boolean c = ! list.isEmpty();
    boolean d = ! list.isEmpty();
    boolean e = list.isEmpty();
    boolean f = list.isEmpty();
  }

  void reversedOperands() {
    boolean a = list.isEmpty();
    boolean b = ! list.isEmpty();
    boolean c = ! list.isEmpty();
    boolean d = ! list.isEmpty();
    boolean e = list.isEmpty();
    boolean f = list.isEmpty();
  }
}
