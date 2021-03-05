package org.alfasoftware.astra.core.analysis.operations;

import java.util.Collection;

import org.alfasoftware.astra.core.utils.ASTOperation;

/**
 * An extended operation with structured results.
 *
 * An example use might be to present all method invocations found matching some criteria.
 */
public interface AnalysisOperation<T extends AnalysisResult> extends ASTOperation {

  /**
   * Returns all of the results captured during the analysis run
   * @return a collection of typed results
   */
  Collection<T> getResults();
}

