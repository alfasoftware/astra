package org.alfasoftware.astra.core.analysis.operations.methods;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Model for method AnalysisResults showing the source of the match,
 * the fully qualified type of the match, and the line number at which the match starts in the source file.
 */
public class MatchedMethodResult {

  private final String matchedNode;
  private final String foundInFullyQualifiedType;
  private final int lineNumber;

  public MatchedMethodResult(ASTNode matchedNode, String foundInFullyQualifiedType, int lineNumber) {
    super();
    this.matchedNode = matchedNode.toString();
    this.foundInFullyQualifiedType = foundInFullyQualifiedType;
    this.lineNumber = lineNumber;
  }

  public MatchedMethodResult(String matchedNode, String foundInFullyQualifiedType, int lineNumber) {
    super();
    this.matchedNode = matchedNode;
    this.foundInFullyQualifiedType = foundInFullyQualifiedType;
    this.lineNumber = lineNumber;
  }

  @Override
  public String toString() {
    return "[" + foundInFullyQualifiedType + "][" + lineNumber + "]: [" + matchedNode + "]";
  }
}