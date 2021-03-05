package org.alfasoftware.astra.core.refactoring.interfaces.inlining;

import java.util.List;

import org.alfasoftware.astra.exampleTypes.A;

public interface InlineInterfaceExample extends SampleInterface, OtherInterface {

  @Override
  List<A> getGeneric();
}
