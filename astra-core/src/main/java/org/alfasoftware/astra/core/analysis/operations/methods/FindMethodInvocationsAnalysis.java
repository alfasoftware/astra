package org.alfasoftware.astra.core.analysis.operations.methods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;

/**
 * Analysis operation to find method invocations matching set criteria.
 */
public class FindMethodInvocationsAnalysis implements AnalysisOperation<MethodAnalysisResult> {

  private final Set<MethodMatcher> matchers;
  private final Map<MethodMatcher, List<MatchedMethodResult>> matchedNodes = new HashMap<>();

  public FindMethodInvocationsAnalysis(Set<MethodMatcher> matchers) {
    this.matchers = matchers;
  }

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter)
      throws IOException, MalformedTreeException, BadLocationException {
    if (node instanceof MethodInvocation || node instanceof ClassInstanceCreation) {
      matchers.stream()
        .filter(m -> 
             node instanceof MethodInvocation && m.matches((MethodInvocation) node, compilationUnit)
          || node instanceof ClassInstanceCreation && m.matches((ClassInstanceCreation) node)
        )
        .findAny()
        .ifPresent(method -> 
          matchedNodes.computeIfAbsent(method, m -> new ArrayList<>()).add(
            new MatchedMethodResult(node, AstraUtils.getNameForCompilationUnit(compilationUnit),
              compilationUnit.getLineNumber(compilationUnit.getExtendedStartPosition(node)))
          )
        );
    }
  }

  public Collection<String> getPrintableResults() {
    List<String> results = new LinkedList<>();
    for (Entry<MethodMatcher, List<MatchedMethodResult>> method : matchedNodes.entrySet()) {
      StringBuilder sb = new StringBuilder();
      sb.append("\r\n");
      sb.append(method.getKey());
      for (MatchedMethodResult result : method.getValue()) {
        sb.append("\r\n");
        sb.append(result.toString());
      }
      results.add(sb.toString());
    }
    
    results.add("\r\n ============ SUMMARY =========== ");
    matchedNodes.entrySet().stream()
        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue(Comparator.comparingInt(List::size))))
        .forEach(e -> results.add("\r\n Usages: [" + e.getValue().size() + "], Method: [" + e.getKey() + "]"));
    
    return results;
  }

  @Override
  public Collection<MethodAnalysisResult> getResults() {
    return matchedNodes.entrySet().stream().map(m -> new MethodAnalysisResult(m.getKey(), m.getValue())).collect(Collectors.toSet());
  }
}
