package org.alfasoftware.astra.core.refactoring.javapattern;

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.Map;

/**
 * Contains information about a matched ASTNode and the pattern captures such as
 * - The ASTNodes that have been captured to replace simpleName parameters
 * - The ASTNodes captured to replace @Substitute methods
 * - The resolved TypeParameters for parameterized types that have been captured
 *
 * This information is used by the {@link JavaPatternASTOperation} when rewriting a compilation unit
 */
public class ASTNodeMatchInformation {
  private final ASTNode nodeThatWasMatched;
  private final Map<String, ASTNode> substituteMethodToCapturedNode;
  private final Map<String, ASTNode> simpleNameToCapturedNode;
  private final Map<String, String> simpleTypeToCapturedType;

  public ASTNodeMatchInformation(ASTNode nodeThatWasMatched, Map<String, ASTNode> substituteMethodToCapturedNode, Map<String, ASTNode> simpleNameToCapturedNode, Map<String, String> simpleTypeToCapturedType) {
    this.nodeThatWasMatched = nodeThatWasMatched;
    this.substituteMethodToCapturedNode = substituteMethodToCapturedNode;
    this.simpleNameToCapturedNode = simpleNameToCapturedNode;
    this.simpleTypeToCapturedType = simpleTypeToCapturedType;
  }

  public ASTNode getNodeThatWasMatched() {
    return nodeThatWasMatched;
  }

  public Map<String, ASTNode> getSubstituteMethodToCapturedNode() {
    return substituteMethodToCapturedNode;
  }

  public Map<String, ASTNode> getSimpleNameToCapturedNode() {
    return simpleNameToCapturedNode;
  }

  public final Map<String, String> getSimpleTypeToCapturedType() {
    return simpleTypeToCapturedType;
  }
}
