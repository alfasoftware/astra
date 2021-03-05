package org.alfasoftware.astra.core.analysis.operations.assignment;

import java.util.Objects;

import org.alfasoftware.astra.core.analysis.operations.AnalysisResult;

/**
 * Result type for assignment analysis. Captures the variable name assigned and the value assigned to it.
 */
public class AssignmentAnalysisResult extends AnalysisResult {

  private String variableName;
  private String assignedValue;

  public AssignmentAnalysisResult(){
  }

  public AssignmentAnalysisResult(final String variableName, final String assignedValue) {
    this.variableName = variableName;
    this.assignedValue = assignedValue;
  }

  public void setVariableName(final String variableName) {
    this.variableName = variableName;
  }

  public void setAssignedValue(final String assignedValue) {
    this.assignedValue = assignedValue;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final AssignmentAnalysisResult that = (AssignmentAnalysisResult) o;
    return Objects.equals(variableName, that.variableName) &&
        Objects.equals(assignedValue, that.assignedValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(variableName, assignedValue);
  }

  @Override
  public String toString() {
    return "AssignmentAnalysisResult{" +
        "variableName='" + variableName + '\'' +
        ", assignedValue='" + assignedValue + '\'' +
        '}';
  }
}
