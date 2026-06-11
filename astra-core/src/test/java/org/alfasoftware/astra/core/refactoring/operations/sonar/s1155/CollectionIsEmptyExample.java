package org.alfasoftware.astra.core.refactoring.operations.sonar.s1155;

import java.util.ArrayList;
import java.util.List;

public class CollectionIsEmptyExample {

  private final List<String> list = new ArrayList<>();

  void sizeOnLeft() {
    boolean a = list.size() == 0;
    boolean b = list.size() != 0;
    boolean c = list.size() > 0;
    boolean d = list.size() >= 1;
    boolean e = list.size() < 1;
    boolean f = list.size() <= 0;
  }

  void reversedOperands() {
    boolean a = 0 == list.size();
    boolean b = 0 != list.size();
    boolean c = 0 < list.size();
    boolean d = 1 <= list.size();
    boolean e = 1 > list.size();
    boolean f = 0 >= list.size();
  }
}
