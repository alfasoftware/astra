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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + foundInFullyQualifiedType.hashCode(); 
    result = prime * result + matchedNode.hashCode();
    result = prime * result + lineNumber;
    return result;
  }

  @Override
  public boolean equals(Object obj){
    if(obj == this){
      return true;  
    }
    boolean isEqual = true;
    if (obj == null || getClass() != obj.getClass()) {
      isEqual = false;
    }
    MatchedMethodResult other = (MatchedMethodResult) obj;
    if(other != null && hashCode() != other.hashCode()){
      isEqual = false;
    }
    return isEqual;
  }

  @Override
  public String toString() {
    return "[" + foundInFullyQualifiedType + "][" + lineNumber + "]: [" + matchedNode + "]";
  }
}
