package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.edge;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTNode;

public class UnknownTypeInLambdaExampleAfter {

  public void test1() {
    ASTNode node = null;

    Objects.deepEquals(node, null);
    Optional.of(node).filter(d -> Objects.deepEquals(d, null));
  }
}