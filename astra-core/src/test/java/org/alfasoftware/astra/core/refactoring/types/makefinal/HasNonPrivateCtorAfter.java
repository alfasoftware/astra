package org.alfasoftware.astra.core.refactoring.types.makefinal;

class HasNonPrivateCtorAfter {
  HasNonPrivateCtorAfter() {}
  private HasNonPrivateCtorAfter(int x) {}
}
