package org.alfasoftware.astra.core.refactoring.interfaces.inlining;

public interface FooAccess<T> extends BaseInterface<String> {

  void fooMethod(T instance);
}

