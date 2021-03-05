package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.edge;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTNode;

public class UnknownTypeInLambdaExample {

  public void test1() {
    ASTNode node = null;

    Objects.equals(node, null);
    Optional.of(node).filter(d -> Objects.equals(d, null));
  }
}