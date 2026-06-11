package org.alfasoftware.astra.core.refactoring.operations.sonar.s1155;

import java.util.ArrayList;
import java.util.List;

public class CollectionIsEmptyNonCollectionExample {

  private final NonCollectionWithSize custom = new NonCollectionWithSize();
  private final List<String> list = new ArrayList<>();

  void nonCollectionShouldNotChange() {
    boolean a = custom.size() == 0;
    boolean b = custom.size() != 0;
    boolean c = custom.size() > 0;
  }

  void alreadyUsingIsEmpty() {
    boolean a = list.isEmpty();
    boolean b = !list.isEmpty();
  }

  void unsupportedLiteralShouldNotChange() {
    boolean a = list.size() == 2;
    boolean b = list.size() > 5;
  }
}
