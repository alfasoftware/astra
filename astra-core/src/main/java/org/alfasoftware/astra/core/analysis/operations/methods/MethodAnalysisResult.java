package org.alfasoftware.astra.core.analysis.operations.methods;

import java.util.List;
import java.util.Map;

import org.alfasoftware.astra.core.analysis.operations.AnalysisResult;
import org.alfasoftware.astra.core.matchers.MethodMatcher;

/**
 * Method based AnalysisResult implementation mapping method matchers to matched methods.
 * This allows the same implementation to cover both method invocations and declarations.
 */
public class MethodAnalysisResult extends AnalysisResult implements Map.Entry<MethodMatcher, List<MatchedMethodResult>> {

  MethodMatcher methodToMatch;
  List<MatchedMethodResult> matches;

  public MethodAnalysisResult(MethodMatcher methodToMatch, List<MatchedMethodResult> matches) {
    super();
    this.methodToMatch = methodToMatch;
    this.matches = matches;
  }

  @Override
  public MethodMatcher getKey() {
    return methodToMatch;
  }

  @Override
  public List<MatchedMethodResult> getValue() {
    return matches;
  }

  @Override
  public List<MatchedMethodResult> setValue(List<MatchedMethodResult> value) {
    List<MatchedMethodResult> oldValue = matches;
    matches = value;
    return oldValue;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (matches == null ? 0 : matches.hashCode());
    result = prime * result + (methodToMatch == null ? 0 : methodToMatch.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MethodAnalysisResult other = (MethodAnalysisResult) obj;
    if (matches == null) {
      if (other.matches != null) return false;
    } else if (!matches.equals(other.matches)) return false;
    if (methodToMatch == null) {
      if (other.methodToMatch != null) return false;
    } else if (!methodToMatch.equals(other.methodToMatch)) return false;
    return true;
  }
}