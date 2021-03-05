package org.alfasoftware.astra.core.analysis;

import java.util.Set;

import org.alfasoftware.astra.core.analysis.operations.AnalysisOperation;
import org.alfasoftware.astra.core.analysis.operations.AnalysisResult;
import org.alfasoftware.astra.core.refactoring.UseCase;

/**
 * A use case consisting solely of analysis operations.
 */
public interface AnalysisUseCase extends UseCase {

  @Override
  Set<? extends AnalysisOperation<? extends AnalysisResult>> getOperations();
}

