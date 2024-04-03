package org.alfasoftware.astra.core.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.analysis.operations.AnalysisOperation;
import org.alfasoftware.astra.core.analysis.operations.AnalysisResult;
import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.utils.AstraCore;
import org.eclipse.jface.text.BadLocationException;

public abstract class AbstractAnalysisTest {

  protected static final String TEST_SOURCE = Paths.get(".").toAbsolutePath().normalize().toString().concat("/src/test/java");
  protected static final String TEST_EXAMPLES = "./src/test/java";

  /**
   * Reads the fileToAnalyse from the examples. Calls the Astra runner on the file example,
   * and checks that the gathered results match the passed in expected values.
   *
   * @param fileToAnalyse The file to perform the analysis on
   * @param analysisOperation Set of analysisOperation to apply
   * @param expectedResults The collection of expected analysis results for the example file
   */
  protected <T extends AnalysisResult> void assertAnalysis(Class<?> fileToAnalyse, AnalysisOperation<T> analysisOperation, Collection<T> expectedResults) {
    // This just gets the java path.
    assertAnalysisWithClassPath(fileToAnalyse, analysisOperation, expectedResults, UseCase.DEFAULT_CLASSPATH_ENTRIES.toArray(new String[0]));
  }

  protected <T extends AnalysisResult> void assertAnalysisWithClassPath(Class<?> fileToAnalyse, AnalysisOperation<T> analysisOperation, Collection<T> expectedResults, String[] classPath) {
    assertAnalysisWithSourcesAndClassPath(fileToAnalyse, analysisOperation, expectedResults, new HashSet<>(Arrays.asList(TEST_SOURCE)).toArray(new String[0]), classPath);
  }

  protected <T extends AnalysisResult> void assertAnalysisWithSourcesAndClassPath(Class<?> fileToAnalyse, AnalysisOperation<T> analysisOperation, Collection<T> expectedResults, String[] sources, String[] classPath) {

    try {
      File file = new File(TEST_EXAMPLES + "/" + fileToAnalyse.getName().replaceAll("\\.", "/") + ".java");
      String fileContent = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
      new AstraCore().applyOperationsToFile(file, fileContent, Collections.singleton(analysisOperation), sources, classPath);
    } catch (IOException | BadLocationException e) {
      fail();
    }

    assertEquals("The number of results should exactly match the number of expected results", expectedResults.size(), analysisOperation.getResults().size());
    final List<T> sortedResult = analysisOperation.getResults().stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList());
    final List<T> sortedExpectation = expectedResults.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList());

    assertTrue("Expected \n" + sortedResult + "\n to contain all of \n " + sortedExpectation , sortedResult.containsAll(sortedExpectation));
  }
}
