package org.alfasoftware.astra.core.refactoring.types.makefinal;

class HasNonPrivateCtor {
  HasNonPrivateCtor() {}
  private HasNonPrivateCtor(int x) {}
}
