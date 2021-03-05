package org.alfasoftware.astra.core.analysis.operations;

/**
 * Encapsulates the results found during an analysis run. This allows us to reason about the results and write tests for them.
 *
 * Forces implementation of the equals method to allow comparison of results.
 */
public abstract class AnalysisResult {

  public abstract boolean equals(Object other);

  public abstract int hashCode();

}
