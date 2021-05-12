package org.alfasoftware.astra.core.analysis.operations.methods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfasoftware.astra.core.analysis.operations.AnalysisOperation;
import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Analysis operation to find method declarations matching set criteria.
 */
public class FindMethodDeclarationsAnalysis implements AnalysisOperation<MethodAnalysisResult> {

  private final Set<MethodMatcher> matchers;
  private final Map<MethodMatcher, List<MatchedMethodResult>> matchedNodes = new HashMap<>();

  public FindMethodDeclarationsAnalysis(Set<MethodMatcher> matchers) {
    this.matchers = matchers;
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof MethodDeclaration) {
      matchers.stream()
        .filter(m -> m.matches((MethodDeclaration) node))
        .findAny()
        .ifPresent(method -> {
          matchedNodes.computeIfAbsent(method, m -> new ArrayList<>()).add(
            new MatchedMethodResult(node, AstraUtils.getNameForCompilationUnit(compilationUnit),
              compilationUnit.getLineNumber(compilationUnit.getExtendedStartPosition(node)))
          );
        });
    }
  }


  public Collection<String> getPrintableResults() {
    List<String> results = new LinkedList<>();
    for (Map.Entry<MethodMatcher, List<MatchedMethodResult>> methodEntry : matchedNodes.entrySet()) {
      StringBuilder sb = new StringBuilder();
      sb.append("\r\n");
      sb.append(methodEntry.getKey());
      for (MatchedMethodResult result : methodEntry.getValue()) {
        sb.append("\r\n");
        sb.append(result.toString());
      }
      results.add(sb.toString());
    }
    return results;
  }


  @Override
  public Collection<MethodAnalysisResult> getResults() {
    return matchedNodes.entrySet().stream().map(m -> new MethodAnalysisResult(m.getKey(), m.getValue())).collect(Collectors.toSet());
  }
}
