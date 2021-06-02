package org.alfasoftware.astra.core.refactoring.operations.javapattern;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;
import java.util.Map;

/**
 * Contains information about a matched ASTNode and the pattern captures such as
 * <ul>
 *   <li>The ASTNodes that have been captured to replace simpleName parameters</li>
 *   <li>The ASTNodes captured to replace @Substitute methods</li>
 *   <li>The resolved TypeParameters for parameterized types that have been captured</li>
 * </ul>
 *
 * This information is used by the {@link JavaPatternASTOperation} when rewriting a compilation unit
 */
class ASTNodeMatchInformation {
  private final ASTNode nodeThatWasMatched;
  private final Map<String, ASTNode> substituteMethodToCapturedNode;
  private final Map<String, ASTNode> simpleNameToCapturedNode;
  private final Map<String, ITypeBinding> simpleTypeToCapturedType;
  private final Map<String, List<ASTNode>> varArgsToCapturedNodes;

  public ASTNodeMatchInformation(ASTNode nodeThatWasMatched, Map<String, ASTNode> substituteMethodToCapturedNode, Map<String, ASTNode> simpleNameToCapturedNode, Map<String, ITypeBinding> simpleTypeToCapturedType, Map<String, List<ASTNode>> varArgsToCapturedNodes) {
    this.nodeThatWasMatched = nodeThatWasMatched;
    this.substituteMethodToCapturedNode = substituteMethodToCapturedNode;
    this.simpleNameToCapturedNode = simpleNameToCapturedNode;
    this.simpleTypeToCapturedType = simpleTypeToCapturedType;
    this.varArgsToCapturedNodes = varArgsToCapturedNodes;
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

  public Map<String, ITypeBinding> getSimpleTypeToCapturedType() {
    return simpleTypeToCapturedType;
  }

  public Map<String, List<ASTNode>> getVarArgsToCapturedNodes() {
    return varArgsToCapturedNodes;
  }
}
